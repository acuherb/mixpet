<template>
  <div class="rp-page" :style="{'--brand': brand}">
    <div class="no-print rp-toolbar">
      <el-button @click="goBack">{{ isClient ? '关闭' : '返回' }}</el-button>
      <span class="rp-badge">寄养结算单</span>
      <el-button :disabled="!row" type="primary" @click="doPrint">打印 / 另存为 PDF</el-button>
      <el-button :disabled="!row" :loading="pdfing" @click="exportPdf">下载 PDF 文件</el-button>
      <span class="rp-hint">医院名称取自「医院资料」；金额由服务端结算，押金冲抵后净额计入账单。</span>
      <span v-if="error" class="rp-err">{{ error }}</span>
    </div>

    <div v-if="row" class="rp-sheet rp-color" :style="{'--brand': brand}">
      <div class="rc-head">
        <div class="rc-head-l">
          <div class="rc-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
          <div class="rc-sub">宠物寄养结算单</div>
        </div>
        <div class="rc-head-r">
          <div class="rc-no">No.{{ row.fosterCode }}</div>
          <div class="rc-date">{{ row.backDate || row.fosterTerm }}</div>
        </div>
      </div>

      <div class="rc-meta-grid">
        <div><span>宠物</span><b>{{ row.petName }}</b></div>
        <div><span>寄养人</span><b>{{ row.clientName }}</b></div>
        <div><span>状态</span><b>{{ fosterStatusText(row.status) }}</b></div>
        <div><span>入托日期</span><b>{{ row.inDate || '—' }}</b></div>
        <div><span>预计接回</span><b>{{ row.fosterTerm || '—' }}</b></div>
        <div><span>实际接回</span><b>{{ row.backDate || '—' }}</b></div>
        <div><span>实际天数</span><b>{{ row.dayCount != null ? row.dayCount + ' 天' : '—' }}</b></div>
        <div><span>结算账单号</span><b>{{ row.billId ? '#' + row.billId : '—' }}</b></div>
      </div>

      <div class="rc-card">
        <div class="rc-card-tit">计费明细（天数含首尾）</div>
        <table class="rc-table">
          <thead><tr><th>项目</th><th>单价/天</th><th>天数</th><th>小计</th></tr></thead>
          <tbody>
            <tr><td>笼位费</td><td>￥{{ n2(row.dailyFee) }}</td><td>{{ row.dayCount ?? '—' }}</td><td>￥{{ (n(row.dailyFee) * (row.dayCount || 0)).toFixed(2) }}</td></tr>
            <tr><td>清洁费</td><td>￥{{ n2(row.cleanFee) }}</td><td>{{ row.dayCount ?? '—' }}</td><td>￥{{ (n(row.cleanFee) * (row.dayCount || 0)).toFixed(2) }}</td></tr>
            <tr><td>餐费</td><td>￥{{ n2(row.mealFee) }}</td><td>{{ row.dayCount ?? '—' }}</td><td>￥{{ (n(row.mealFee) * (row.dayCount || 0)).toFixed(2) }}</td></tr>
            <tr class="sum"><td>结算金额</td><td>—</td><td>—</td><td>￥{{ n2(row.settleMoney) }}</td></tr>
          </tbody>
        </table>
      </div>

      <div class="rc-card">
        <div class="rc-card-tit">押金与多退少补</div>
        <div class="rc-fees">
          <div class="rc-fee"><span>登记押金</span><b>￥{{ n2(row.fosterMoney) }}</b></div>
          <div class="rc-fee"><span>押金冲抵</span><b>￥{{ n2(row.depositUsed) }}</b></div>
          <div class="rc-fee"><span>应退主人</span><b>￥{{ n2(row.refundMoney) }}</b></div>
          <div class="rc-fee rc-fee-total"><span>计入账单补收</span><b>￥{{ n2(row.chargeMoney) }}</b></div>
        </div>
        <div v-if="row.adjustNote" class="rc-adjust">改价留痕：{{ row.adjustNote }}</div>
      </div>

      <div class="rc-advice">
        <div class="rc-advice-tit">订单备注</div>
        <div class="rc-advice-body">{{ row.fosterInfo || '—' }}</div>
      </div>
      <div class="rc-signbox">
        <span>接待 / 结算：{{ row.clientName || '—' }}</span>
        <span>打印时间：{{ nowText() }}</span>
      </div>
      <div class="rc-foot">{{ hosp.name || '动物诊疗机构' }} · 感谢您的信任，祝爱宠健康</div>
    </div>

    <div v-else-if="loaded" class="no-print rp-empty">{{ error || '未找到该寄养单，或无权查看' }}</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {useBrand} from '@/utils/UseBrand'
import {fosterStatusText} from '@/model/entiy/Foster'
import {FosterDto} from '@/model/DO/FosterDto'
import {reqFosterOne, reqFosterSelfOne} from '@/request/FosterApi'

const route = useRoute()
const router = useRouter()
const {hosp, brand, load: loadBrand} = useBrand()
const isClient = computed(() => route.path.startsWith('/reception'))
const row = ref<FosterDto | null>(null)
const loaded = ref(false)
const error = ref('')
const pdfing = ref(false)

const n = (v: number | null | undefined): number => v == null ? 0 : Number(v)
const n2 = (v: number | null | undefined): string => n(v).toFixed(2)
const nowText = (): string => new Date().toLocaleString('zh-CN')
const doPrint = (): void => window.print()
const goBack = (): void => {
  if (isClient.value || window.history.length <= 1) window.close()
  else router.back()
}
const exportPdf = (): void => {
  const el = document.querySelector('.rp-sheet') as HTMLElement | null
  if (!el || !row.value) return
  pdfing.value = true
  const code = String(row.value.fosterCode || row.value.fosterId)
  import('html2pdf.js').then(({default: html2pdf}) => html2pdf().set({
    margin: 8,
    filename: `寄养结算单-No.${code}.pdf`,
    image: {type: 'jpeg', quality: 0.95},
    html2canvas: {scale: 2},
    jsPDF: {unit: 'mm', format: 'a4', orientation: 'portrait'}
  }).from(el).save())
      .catch(() => ElMessage.error('PDF 导出失败，请改用「打印 / 另存为 PDF」'))
      .finally(() => { pdfing.value = false })
}

onMounted(() => {
  loadBrand()
  const id = Number(route.query.id)
  if (!id) { loaded.value = true; error.value = '缺少寄养单号'; return }
  // 顾客走带 client_id 归属校验的接口，员工走后台详情；后端越权返回 null
  const req = isClient.value ? reqFosterSelfOne(id) : reqFosterOne(id)
  req.then(res => {
    if (!res) { error.value = '未找到该寄养单，或无权查看'; return }
    row.value = res
    if (route.query.autopdf === '1') setTimeout(() => exportPdf(), 600)
  }).catch(() => { error.value = '寄养单加载失败' }).finally(() => { loaded.value = true })
})
</script>

<style lang="scss" scoped>
.rp-page { padding: 20px; background: #f5f6fa; min-height: 100vh; }
.rp-toolbar {
  max-width: 800px; margin: 0 auto 16px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  .rp-hint { font-size: 12px; color: #909399; flex-basis: 100%; }
  .rp-err { font-size: 12px; color: #d03050; font-weight: 600; }
  .rp-badge { font-size: 12px; color: var(--brand); background: #eef6fc; border: 1px solid #cfe4f5; border-radius: 14px; padding: 4px 12px; }
}
.rp-sheet { max-width: 800px; margin: 0 auto 24px; background: #fff; border-radius: 12px; box-shadow: 0 8px 28px rgba(31,45,61,.1); }
.rp-empty { text-align: center; color: #999; margin-top: 60px; }
.rp-color { border: none; border-radius: 12px; padding: 0; overflow: hidden; }
.rc-head {
  display: flex; justify-content: space-between; align-items: center; padding: 18px 24px;
  background: linear-gradient(120deg, var(--brand), #2BB6A8); color: #fff;
  .rc-hosp { font-size: 19px; font-weight: 800; letter-spacing: 2px; }
  .rc-sub { font-size: 13px; opacity: .9; margin-top: 2px; }
  .rc-no { font-size: 14px; font-weight: 700; text-align: right; }
  .rc-date { font-size: 12px; opacity: .85; text-align: right; margin-top: 2px; }
}
.rc-meta-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 4px 12px;
  padding: 14px 24px; background: #f0f7fc; border-bottom: 1px solid #e4e7ed;
  div { display: flex; gap: 6px; font-size: 12px; line-height: 1.8; }
  span { color: #7d8899; flex: none; }
  b { color: #1F2D3D; font-weight: 600; }
}
.rc-card { margin: 14px 20px; border: 1px solid #ebeef5; border-radius: 10px; overflow: hidden; }
.rc-card-tit { padding: 8px 14px; background: #f5f7fa; font-size: 13px; font-weight: 700; color: #1F2D3D; border-left: 3px solid var(--brand); }
.rc-table { width: 100%; border-collapse: collapse; }
.rc-table th, .rc-table td { border-bottom: 1px solid #ebeef5; padding: 7px 12px; font-size: 12.5px; text-align: left; }
.rc-table th { background: #fafbfc; color: #606266; }
.rc-table tr.sum td { font-weight: 700; color: #1F2D3D; background: #f5f7fa; }
.rc-fees { display: flex; flex-wrap: wrap; gap: 8px; padding: 12px 14px; }
.rc-fee { flex: 1; min-width: 120px; background: #f8f9fb; border-radius: 8px; padding: 8px 12px; }
.rc-fee span { display: block; font-size: 11px; color: #909399; }
.rc-fee b { font-size: 15px; color: #1F2D3D; }
.rc-fee-total { background: #fdf2f2; }
.rc-fee-total b { color: #F56C6C; }
.rc-adjust { padding: 6px 14px 12px; font-size: 12px; color: #b88230; }
.rc-advice { margin: 14px 20px; padding: 12px 16px; background: #fdf6ec; border: 1px dashed #e6a23c; border-radius: 10px; }
.rc-advice-tit { font-size: 13px; font-weight: 700; color: #b88230; margin-bottom: 4px; }
.rc-advice-body { font-size: 13px; color: #606266; line-height: 1.7; white-space: pre-wrap; }
.rc-signbox { display: flex; flex-wrap: wrap; gap: 8px 24px; margin: 14px 20px 6px; padding: 10px 12px; border-radius: 8px; background: #f4f8fb; font-size: 12px; color: #42536b; font-weight: 600; }
.rc-foot { padding: 12px 24px 20px; text-align: center; font-size: 11px; color: #a8abb2; }
@page { size: A4 portrait; margin: 10mm; }
@media print {
  .rp-page { padding: 0; background: #fff; }
  .no-print { display: none !important; }
  .rp-sheet { box-shadow: none; margin: 0; max-width: 100%; }
  .rp-color { border-radius: 0; }
  .rc-head, .rc-meta-grid, .rc-card-tit, .rc-advice, .rc-signbox, .rc-fee-total {
    -webkit-print-color-adjust: exact; print-color-adjust: exact;
  }
  .rc-card { break-inside: avoid; }
}
</style>
