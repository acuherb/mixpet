<template>
  <!-- 数据失效兜底：直接打开链接或重复刷新时明确提示，不显示空白单据 -->
  <div v-if="!payload" class="rm-page">
    <div class="no-print rm-toolbar" style="justify-content:center">
      <el-button @click="closeWin">关闭窗口</el-button>
    </div>
    <div class="rm-sheet" style="padding:60px 28px;text-align:center;color:#8a93a6">
      凭证数据已失效或链接已过期，请回到收银台重新打印。
    </div>
  </div>

  <ReportSheet v-else
               :title="isTicket ? '收费小票' : '收费凭证'"
               :doc-no="'No.' + payload.row.billId"
               :hosp-name="payload.hosp?.name || undefined"
               :brand="payload.hosp?.brandColor || undefined"
               :fields="patientFields"
               :signers="signerFields"
               :ticket="isTicket"
               :footer="footerText"
               print-time>
    <template #toolbar>
      <el-button @click="closeWin">关闭</el-button>
      <el-button type="primary" @click="doPrint">打印凭证</el-button>
      <span class="rm-muted">打印目标选“另存为 PDF”可得到文字可选的矢量文件</span>
    </template>

    <section class="rm-sec">
      <div class="rm-sec-title">费用明细</div>
      <table class="rm-table">
        <thead>
        <tr>
          <th>收费项目</th>
          <th v-if="!isTicket">数量</th>
          <th class="num">金额（元）</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(it, i) in feeRows" :key="i">
          <td>{{ it.name }}</td>
          <td v-if="!isTicket" class="rm-muted">{{ it.qty }}</td>
          <td class="num">{{ fmtMoney(it.amount) }}</td>
        </tr>
        </tbody>
      </table>
      <div class="rm-total"><span>实付合计</span><b>￥{{ fmtMoney(payAmount) }}</b></div>
      <div v-if="!isTicket" class="rm-note">
        合计（大写）：{{ moneyCN(payAmount) }}　（小写）：￥{{ fmtMoney(payAmount) }}
      </div>
    </section>

    <section v-if="!isTicket && payload.prescriptions.length" class="rm-sec">
      <div class="rm-sec-title">处方药品</div>
      <table class="rm-table">
        <thead><tr><th>药品</th><th>规格</th><th class="num">数量</th><th class="num">单价（元）</th></tr></thead>
        <tbody>
        <tr v-for="(p, i) in payload.prescriptions" :key="i">
          <td>{{ p.drugName || '—' }}</td>
          <td class="rm-muted">{{ p.drugSpec || '—' }}</td>
          <td class="num">{{ p.quantity }}{{ p.drugUnit || '' }}</td>
          <td class="num">{{ fmtMoney(p.drugPrice) }}</td>
        </tr>
        </tbody>
      </table>
    </section>

    <section class="rm-sec">
      <div class="rm-sec-title">结算信息</div>
      <div class="rm-kv"><span class="rm-k">支付方式</span><span>{{ payload.row.payType ? '现金' : '余额' }}</span></div>
      <div class="rm-kv"><span class="rm-k">支付时间</span><span>{{ payTime }}</span></div>
      <div class="rm-kv"><span class="rm-k">收银员</span><span>{{ payload.row.payBy || '—' }}</span></div>
      <div v-if="!isTicket" class="rm-kv"><span class="rm-k">备注</span><span>宠物诊疗服务费；宠物 {{ payload.row.petName || '—' }}，医生 {{ payload.row.doctorName || '—' }}</span></div>
    </section>
  </ReportSheet>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, watch, ref} from 'vue'
import {useRoute} from 'vue-router'
import ReportSheet from '@/components/sheet/ReportSheet.vue'
import {BillDto} from '@/model/DO/BillDto'
import {Prescription} from "@/model/entiy/MedicalRecord"
import {HospitalSetting} from '@/model/entiy/HospitalSetting'

type Payload = {row: BillDto; prescriptions: Prescription[]; hosp: HospitalSetting | null}

const route = useRoute()
const isTicket = computed(() => route.query.type === 'ticket')
const payload = ref<Payload | null>(null)
let pageStyle: HTMLStyleElement | null = null

// 同一路由只换 query 时 Vue 会复用组件，onMounted 不会再跑：
// 用 immediate watch 保证首次进入与后续换单号都能重新读取凭证数据。
const loadPayload = (): void => {
  const key = String(route.query.key || '')
  const storageKey = 'mixpet:print:' + key
  try {
    const s = key ? localStorage.getItem(storageKey) : null
    payload.value = s ? JSON.parse(s) as Payload : null
    // 一次性数据，读完即删（含客户电话，不在本地长期留存）
    if (key) localStorage.removeItem(storageKey)
  } catch {
    payload.value = null
  }
}
watch(() => route.query.key, loadPayload, {immediate: true})

// 热敏小票单独声明纸张；A4 凭证用全局 report-modern 的 @page
watch(isTicket, (v) => {
  pageStyle?.remove()
  pageStyle = null
  if (v) {
    pageStyle = document.createElement('style')
    pageStyle.textContent = '@page { size: 80mm auto; margin: 0; }'
    document.head.appendChild(pageStyle)
  }
}, {immediate: true})
onBeforeUnmount(() => pageStyle?.remove())

const fmtMoney = (v: number | null | undefined): string => (Number(v) || 0).toFixed(2)
const payTime = computed(() => payload.value?.row?.payTime || payload.value?.row?.createTime || '—')
const payAmount = computed(() => {
  const r = payload.value?.row
  if (!r) return 0
  return Number(r.realMoney ?? r.totalMoney ?? 0)
})

const feeRows = computed(() => {
  const r = payload.value?.row
  if (!r) return []
  const list: {name: string; qty: string; amount: number}[] = [
    {name: '门诊挂号服务', qty: '1 次', amount: r.feeRegister || 0},
    {name: '诊疗处置服务', qty: '1 次', amount: r.feeTreat || 0}
  ]
  const add = (name: string, v: number | null | undefined, qty = '1 项'): void => {
    if ((v || 0) > 0) list.push({name, qty, amount: Number(v)})
  }
  add('药品费', r.feeDrug)
  add('化验检查费', r.feeLab)
  add('影像检查费', r.feeImage)
  add('住院费', r.feeHospital, '1 天')
  add('套餐费', r.feePackage)
  add('寄养费', r.feeFoster)
  add('手术费', r.feeSurgery)
  add('疫苗费', r.feeImmune)
  return list
})

const patientFields = computed(() => {
  const r = payload.value?.row
  if (!r) return []
  return [
    {label: '客户', value: r.clientName},
    {label: '联系电话', value: r.clientTel || '—'},
    {label: '宠物', value: r.petName},
    {label: '接诊医生', value: r.doctorName || '—'},
    {label: '账单号', value: 'No.' + r.billId},
    {label: '开具日期', value: payTime.value},
    {label: '支付方式', value: r.payType ? '现金' : '余额'},
    {label: '收银员', value: r.payBy || '—'}
  ]
})
const signerFields = computed(() => {
  const h = payload.value?.hosp
  return [
    {label: '收款人', value: payload.value?.row?.payBy || '—'},
    {label: '发药核对', value: payload.value?.row?.dispenseBy || '未核对'},
    {label: '收款单位', value: h?.name || '—'},
    {label: '联系电话', value: h?.phone || '—'}
  ]
})
const footerText = computed(() => isTicket
    ? '谢谢惠顾，祝宠物早日康复！本小票仅作收据凭证。'
    : '本单据为院内收费凭证，非税务发票，不作报销发票使用；如需发票请按当地税务规定另行开具。')

/** 数字金额转中文大写（元角分） */
const moneyCN = (n: number | null | undefined): string => {
  let num = Math.round((Number(n) || 0) * 100)
  if (num === 0) return '零元整'
  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const unit = ['', '拾', '佰', '仟']
  const bigUnit = ['', '万', '亿', '兆']
  const yi = Math.floor(num / 100)
  const fen = num % 100
  let str = ''
  if (yi > 0) {
    let yiStr = ''
    let lastZero = false
    const yiStrRaw = String(yi)
    for (let i = 0; i < yiStrRaw.length; i++) {
      const d = Number(yiStrRaw[i])
      const pos = yiStrRaw.length - 1 - i
      if (d === 0) {
        if (!lastZero && yiStr) yiStr += '零'
        lastZero = true
      } else {
        yiStr += digits[d] + unit[pos % 4] + (pos >= 4 ? bigUnit[Math.floor(pos / 4)] : '')
        lastZero = false
      }
    }
    str += yiStr.replace(/零+$/, '') + '元'
  }
  const jiao = Math.floor(fen / 10) % 10
  const f = fen % 10
  if (jiao > 0) str += digits[jiao] + '角'
  else if (yi > 0 && f > 0) str += '零'
  if (f > 0) str += digits[f] + '分'
  if (jiao === 0 && f === 0) str += '整'
  return str
}

const doPrint = (): void => window.print()
const closeWin = (): void => window.close()
</script>
