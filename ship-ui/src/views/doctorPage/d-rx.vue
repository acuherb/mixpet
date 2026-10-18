<template>
  <div class="rx-page">
    <!-- 工具栏（不打印） -->
    <div class="no-print rx-toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-button :disabled="!rx" @click="exportPdf">导出 PDF</el-button>
      <el-button type="primary" :disabled="!rx" @click="doPrint">打印处方笺</el-button>
      <span v-if="rx?.supplementStatus === 'VOID'" style="color:#f56c6c;margin-left:10px">该处方笺编号已作废</span>
      <span v-else-if="!rx?.printNo" style="color:#909399;margin-left:10px">未生成编号，请从接诊/病历页「打印处方笺」进入</span>
    </div>

    <div v-if="rx">
      <!-- 按联次渲染纸张：copies=1 仅第三联(畜主留存)；copies=3 三联全打 -->
      <div v-for="pg in pages" :key="pg.no" class="rx-sheet">
        <!-- 联次标识（每页顶部居中，文字标注不用颜色区分） -->
        <div class="rx-copy">{{ pg.label }}</div>

        <!-- 机构名 + 标题 + 编号 -->
        <div class="rx-org">{{ hosp.name || '动物诊疗机构' }}</div>
        <div class="rx-title">兽用处方笺</div>
        <div class="rx-no">编号：{{ rx.printNo || rx.rxNo || '打印时自动生成' }}</div>

        <!-- 前记 -->
        <table class="rx-table rx-front">
          <tbody>
            <tr>
              <td class="k">畜主姓名</td><td class="v">{{ rx.clientName }}</td>
              <td class="k">病历号</td><td class="v">No.{{ rx.recordId }}</td>
            </tr>
            <tr>
              <td class="k">动物种类</td><td class="v">{{ rx.species || '________' }}</td>
              <td class="k">毛色</td><td class="v">{{ rx.hairColor || '________' }}</td>
            </tr>
            <tr>
              <td class="k">性别</td><td class="v">{{ rx.petSex == null ? '____' : (rx.petSex ? '公' : '母') }}</td>
              <td class="k">体重</td><td class="v">{{ rx.petWeight != null ? rx.petWeight + 'kg' : '____' }}</td>
            </tr>
            <tr>
              <td class="k">年（日）龄</td><td class="v">{{ rx.petBirthday ? getAge(rx.petBirthday) : '____' }}</td>
              <td class="k">开具日期</td><td class="v">{{ fmt(rx.prescriptionTime || rx.createTime) }}</td>
            </tr>
          </tbody>
        </table>

        <!-- 正文：初步诊断 -->
        <div class="rx-sec-tit">初步诊断</div>
        <div class="rx-diag">{{ rx.diagnosis || '____' }}</div>

        <!-- 正文：Rp 处方（兽药名称 | 规格 | 数量 | 用法用量 | 休药期） -->
        <div class="rx-rp">Rp<span class="rx-rp-tip">（请取）</span></div>
        <table class="rx-table rx-items">
          <thead>
            <tr>
              <th style="width: 30%">兽药名称</th>
              <th style="width: 15%">规格</th>
              <th style="width: 10%">数量</th>
              <th style="width: 28%">用法用量</th>
              <th style="width: 17%">休药期</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(it, i) in rx.items" :key="i">
              <td>{{ it.drugName }}{{ it.drugUnit ? '（' + it.drugUnit + '）' : '' }}</td>
              <td>{{ it.drugSpec || '—' }}</td>
              <td>{{ it.quantity }}</td>
              <td>{{ usageText(it) }}</td>
              <td>{{ rx.withdrawalPeriod || '—' }}</td>
            </tr>
            <!-- Rp 后空白处打印前自动划斜线（禁止假数据填充） -->
            <tr class="rx-blank"><td colspan="5"/></tr>
            <tr class="rx-blank"><td colspan="5"/></tr>
            <tr class="rx-blank-end"><td colspan="5">以下为空白</td></tr>
          </tbody>
        </table>

        <div class="rx-amount">
          金额（小写）：￥{{ (rx.totalAmount ?? 0).toFixed(2) }}
          <span style="margin-left: 14px">（大写）：{{ amountToCn(rx.totalAmount ?? 0) }}</span>
        </div>

        <!-- 后记 -->
        <table class="rx-table rx-sign">
          <tbody>
            <tr>
              <td class="k">执业兽医师</td><td class="v sign">（签名/盖章）</td>
              <td class="k">注册号</td><td class="v">{{ rx.doctorCert || '____________' }}</td>
            </tr>
            <tr>
              <td class="k">发药人</td><td class="v sign">（签名/盖章）</td>
              <td class="k">发药日期</td><td class="v">____年__月__日</td>
            </tr>
          </tbody>
        </table>

        <div class="rx-foot">备注：兽用处方药须凭执业兽医处方调配；本处方仅限本院药房调配。不同兽药应当另起行开具；兽用麻醉药品、精神药品、毒性药品及中药自拟方应当单独开具；剂量与数量使用法定计量单位。依据：农业农村部公告第734号《兽医处方格式及应用规范》。</div>
        <!-- 打印时间仅作留痕小字，不进入前记/正文/后记 -->
        <div class="rx-print-time">打印时间：{{ nowText() }}</div>
      </div>
    </div>

    <div v-else-if="!loading" class="no-print rx-empty">未找到该病历的处方笺</div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {reqRxSheet} from "@/request/RecordApi";
import {RxSheet} from "@/model/entiy/RxSheet";
import {reqHospitalSetting} from "@/request/SettingApi";
import {HospitalSetting, emptyHospital} from "@/model/entiy/HospitalSetting";
import {getAge} from "@/utils/TimeUtil";
import {ElMessage} from "element-plus";
import html2pdf from 'html2pdf.js';

const route = useRoute()
const router = useRouter()
const rx = ref<RxSheet | null>(null)
const loading = ref(true)
// 打印页独立渲染：自行拉医院信息（机构名抬头），不用全局缓存
const hosp = ref<HospitalSetting>(emptyHospital())

// 联次定义：文字标注，不用颜色区分（734号：一式三联，机构/药房/动物主人）
const allCopies = [
  {no: 1, label: '第一联 动物诊疗机构留存'},
  {no: 2, label: '第二联 兽药房留存'},
  {no: 3, label: '第三联 动物主人留存'}
]
const pages = ref(allCopies.filter(c => c.no === 3))

onMounted(() => {
  const recordId = Number(route.query.recordId)
  reqHospitalSetting().then(h => { if (h) hosp.value = h }).catch(() => {})
  if (!recordId) { loading.value = false; return }
  reqRxSheet(recordId).then(res => {
    rx.value = res
    loading.value = false
    if (res && res.copies === 3) pages.value = allCopies
  })
  // auto=1：从打印预检弹窗进入，自动触发打印（window.open 同链路，一般不被拦截）
  if (route.query.auto === '1') setTimeout(() => window.print(), 500)
  // pdf=1：档案页批量导出，自动下载 PDF
  if (route.query.pdf === '1') setTimeout(() => exportPdf(), 500)
})

// 导出真实 PDF（小规格横版 210×148mm）
const pdfing = ref(false)
const exportPdf = (): void => {
  const el = document.querySelector('.rx-sheet') as HTMLElement | null
  if (!el || !rx.value) return
  pdfing.value = true
  html2pdf().set({
    margin: 2,
    filename: `处方笺-${rx.value.printNo || rx.value.rxNo || rx.value.recordId}.pdf`,
    image: {type: 'jpeg', quality: 0.95},
    html2canvas: {scale: 2},
    jsPDF: {unit: 'mm', format: [210, 148], orientation: 'landscape'}
  }).from(el).save().catch(() => ElMessage.error('PDF 导出失败')).finally(() => { pdfing.value = false })
}

const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)
const nowText = (): string => new Date().toLocaleString('zh-CN')
const doPrint = (): void => window.print()

/** 用法展示：有结构化单次剂量时展示「单次X×每日Y次」，否则回退自由文本 */
const usageText = (it: any): string => {
  if (it.singleDose != null) {
    const s = `单次${it.singleDose}${it.singleDoseUnit || ''}×每日${it.dailyTimes || 1}次`
    return it.usageInfo ? `${s}（${it.usageInfo}）` : s
  }
  return it.usageInfo || '—'
}

/** 金额转人民币大写 */
const amountToCn = (num: number): string => {
  if (isNaN(num)) return ''
  const cn = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const unit = ['', '拾', '佰', '仟']
  const group = ['', '万', '亿', '兆']
  let integer = Math.floor(Math.abs(num))
  const decimal = Math.round((Math.abs(num) - integer) * 100)
  let s = ''
  if (integer === 0) {
    s = '零'
  } else {
    let gi = 0
    while (integer > 0) {
      const g = integer % 10000
      if (g !== 0) {
        let gs = ''
        let u = 0
        let gg = g
        let zero = false
        while (gg > 0) {
          const d = gg % 10
          if (d === 0) {
            if (!zero && gs !== '') zero = true
          } else {
            if (zero && gs !== '') gs = '零' + gs
            gs = cn[d] + unit[u] + gs
            zero = false
          }
          gg = Math.floor(gg / 10)
          u++
        }
        s = gs + group[gi] + s
      } else if (s !== '' && !s.startsWith('零')) {
        s = '零' + s
      }
      integer = Math.floor(integer / 10000)
      gi++
    }
  }
  s += '元'
  const jiao = Math.floor(decimal / 10)
  const fen = decimal % 10
  if (jiao === 0 && fen === 0) {
    s += '整'
  } else {
    if (jiao > 0) s += cn[jiao] + '角'
    else if (fen > 0) s += '零'
    if (fen > 0) s += cn[fen] + '分'
  }
  return (num < 0 ? '负' : '') + s
}
</script>

<style scoped>
/* 小规格横版 210mm × 148mm（S1：先只做小规格） */
@page {
  size: 210mm 148mm;
  margin: 0;
}
.rx-page {
  padding: 16px;
  background: #f5f6fa;
  min-height: 100vh;
}
.rx-toolbar {
  max-width: 210mm;
  margin: 0 auto 12px;
  text-align: right;
}
.rx-sheet {
  width: 210mm;
  min-height: 148mm;
  margin: 0 auto 16px;
  background: #fff;
  border: 2px solid #333;
  padding: 18px 22px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
  position: relative;
}
.rx-copy {
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 2px;
  border-bottom: 1px solid #333;
  padding-bottom: 4px;
  margin-bottom: 6px;
}
.rx-org {
  text-align: center;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 3px;
}
.rx-title {
  text-align: center;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 8px;
  margin: 2px 0 2px;
}
.rx-no {
  text-align: right;
  font-size: 12px;
  color: #444;
  margin-bottom: 6px;
}
.rx-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 6px;
}
.rx-table td, .rx-table th {
  border: 1px solid #333;
  padding: 4px 6px;
  font-size: 12px;
}
.rx-table .k {
  background: #f0f2f5;
  width: 76px;
  text-align: center;
  white-space: nowrap;
}
.rx-table .v { min-width: 50px; }
.rx-table .sign { color: #666; letter-spacing: 1px; }
.rx-items th { background: #f0f2f5; text-align: center; }
.rx-sec-tit {
  font-size: 13px;
  font-weight: 700;
  margin: 4px 0 2px;
}
.rx-diag {
  border: 1px solid #333;
  border-radius: 0;
  padding: 5px 8px;
  font-size: 12px;
  min-height: 20px;
  margin-bottom: 6px;
}
.rx-rp {
  font-weight: 700;
  font-size: 13px;
  margin: 2px 0 2px;
}
.rx-rp-tip { font-weight: 400; font-size: 11px; color: #666; }
/* Rp 后空白处：打印前自动划斜线（斜线条纹，无假数据行） */
.rx-blank td {
  height: 22px;
  background: repeating-linear-gradient(45deg, transparent 0 10px, #ddd 10px 11px);
}
.rx-blank-end td {
  text-align: center;
  font-size: 11px;
  color: #666;
  height: 18px;
}
.rx-amount {
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  margin: 4px 0 6px;
}
.rx-sign td { height: 30px; }
.rx-foot {
  font-size: 10px;
  color: #888;
  margin-top: 6px;
}
.rx-print-time {
  position: absolute;
  right: 22px;
  bottom: 6px;
  font-size: 9px;
  color: #aaa;
}
.rx-empty {
  text-align: center;
  color: #999;
  margin-top: 60px;
}

@media print {
  .rx-page { padding: 0; background: #fff; }
  .no-print { display: none !important; }
  .rx-sheet {
    box-shadow: none;
    border: 2px solid #000;
    margin: 0;
    page-break-after: always;
  }
  .rx-sheet:last-child { page-break-after: auto; }
}
</style>
