<template>
  <div class="cr-page">
    <div class="no-print cr-bar">
      <el-button @click="goBack">返回</el-button>
      <el-button :disabled="!full" type="primary" @click="doPrint">打印 / 另存为 PDF</el-button>
      <span class="cr-hint">就诊资料（病历 + 处方）· 仅供宠物主人留存，不含院内备注</span>
      <span v-if="error" class="cr-err">{{ error }}</span>
    </div>

    <div v-if="full" class="cr-sheet">
      <div class="cr-head">
        <div class="cr-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
        <div class="cr-title">就 诊 资 料</div>
        <div class="cr-meta">
          <span>就诊号：No.{{ full.record.recordId }}</span>
          <span>就诊日期：{{ fmt(full.record.createTime) }}</span>
          <span>主诊医师：{{ full.record.doctorName || '—' }}</span>
        </div>
      </div>

      <table class="cr-info">
        <tr>
          <td><b>宠物：</b>{{ full.record.petName || '—' }}</td>
          <td><b>主人：</b>{{ full.record.clientName || '—' }}</td>
          <td><b>科室：</b>{{ full.record.departmentName || '—' }}</td>
        </tr>
        <tr>
          <td><b>体重：</b>{{ full.record.weight != null ? full.record.weight + ' kg' : '—' }}</td>
          <td><b>体温：</b>{{ full.record.temperature != null ? full.record.temperature + ' ℃' : '—' }}</td>
          <td><b>复诊日期：</b>{{ fmt(full.record.revisitDate) }}</td>
        </tr>
      </table>

      <div class="cr-sec">主诉与病史</div>
      <div class="cr-kv"><span>主诉</span><p>{{ full.record.chiefComplaint || '—' }}</p></div>
      <div class="cr-kv"><span>现病史</span><p>{{ full.record.presentIllness || '—' }}</p></div>
      <div class="cr-kv"><span>既往史</span><p>{{ full.record.pastHistory || '—' }}</p></div>
      <div class="cr-kv"><span>检查</span><p>{{ full.record.physicalExam || '—' }}</p></div>

      <div class="cr-sec">诊断与处置</div>
      <div class="cr-kv"><span>诊断</span><p>{{ full.record.diagnosis || '—' }}</p></div>
      <div class="cr-kv"><span>处置</span><p>{{ full.record.treatment || '—' }}</p></div>
      <div class="cr-kv"><span>医嘱</span><p>{{ full.record.advice || '—' }}</p></div>

      <div class="cr-sec" :class="{'cr-focus': focusRx}">处方（共 {{ full.prescriptions?.length || 0 }} 项）</div>
      <table class="cr-table">
        <thead>
        <tr><th>药品名称</th><th>规格</th><th>数量</th><th>用法</th></tr>
        </thead>
        <tbody>
        <tr v-for="(p, i) in full.prescriptions || []" :key="i">
          <td>{{ p.drugName || '—' }}</td>
          <td>{{ p.drugSpec || '—' }}</td>
          <td>{{ p.quantity }}{{ p.drugUnit || '' }}</td>
          <td>{{ p.usageInfo || '—' }}</td>
        </tr>
        <tr v-if="!full.prescriptions?.length"><td colspan="4" class="cr-empty">本次就诊无处方</td></tr>
        </tbody>
      </table>

      <div class="cr-sec">检查与检验</div>
      <div v-if="labLines.length || imageLines.length" class="cr-lines">
        <div v-for="(t, i) in [...labLines, ...imageLines]" :key="i">{{ t }}</div>
      </div>
      <div v-else class="cr-empty">本次就诊无化验/影像记录</div>

      <div class="cr-sign">
        <span>医师签名：＿＿＿＿＿＿＿</span>
        <span>打印时间：{{ nowText() }}</span>
      </div>
      <div class="cr-foot">本资料为本院诊疗记录摘要，仅供宠物主人留存与复诊参考；如有疑问请联系主诊医师。</div>
    </div>

    <div v-else-if="loaded" class="no-print cr-none">{{ error || '未找到该就诊资料，或无权查看' }}</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {reqRecordFull} from '@/request/RecordApi'
import {RecordFull} from '@/model/VO/RecordFull'
import {useBrand} from '@/utils/UseBrand'

const route = useRoute()
const router = useRouter()
const {hosp, load: loadBrand} = useBrand()
const full = ref<RecordFull | null>(null)
const loaded = ref(false)
const error = ref('')
const focusRx = computed(() => route.query.focus === 'rx')

const fmt = (t?: string | null): string => (t || '').replace('T', ' ').slice(0, 16) || '—'
const nowText = (): string => new Date().toLocaleString('zh-CN')

const labLines = computed<string[]>(() =>
  (full.value?.labOrders || []).map(o => {
    const items = (o.items || []).filter(i => i.resultValue)
      .map(i => `${i.itemName} ${i.resultValue}${i.itemUnit || ''}${i.isAbnormal ? '（异常）' : ''}`)
    return `化验单 #${o.order.orderId}：${items.length ? items.join('；') : '待出结果'}`
  }))
const imageLines = computed<string[]>(() =>
  (full.value?.imageOrders || []).map(o =>
    `影像检查 #${o.orderId}：${o.itemName || '—'}${o.examPart ? '（' + o.examPart + '）' : ''} — ${o.reportText || (o.status ? '已出报告' : '待出报告')}`))

onMounted(() => {
  loadBrand()
  const recordId = Number(route.query.recordId)
  if (!recordId) { loaded.value = true; error.value = '缺少就诊号'; return }
  // 后端 /record/detail 已做「顾客只能看自己宠物」的归属校验，越权返回 null
  reqRecordFull(recordId).then(res => {
    if (!res || !res.record) { error.value = '未找到该就诊资料，或无权查看'; return }
    full.value = res
    if (focusRx.value) {
      // 「查看处方笺」入口：滚动到处方区，便于直接打印
      setTimeout(() => document.querySelector('.cr-focus')?.scrollIntoView({block: 'center'}), 100)
    }
    if (route.query.auto === '1') setTimeout(() => window.print(), 600)
  }).catch(() => { error.value = '资料加载失败，请稍后重试' }).finally(() => { loaded.value = true })
})

const doPrint = (): void => window.print()
const goBack = (): void => {
  if (window.history.length > 1) router.back()
  else window.close()
}
</script>

<style lang="scss" scoped>
.cr-page { padding: 16px; background: #f3f6fb; min-height: 100vh; }
.cr-bar { max-width: 210mm; margin: 0 auto 12px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.cr-hint { color: #8a93a6; font-size: 12px; }
.cr-err { color: #d03050; font-size: 12px; font-weight: 600; }
.cr-sheet {
  width: 210mm; min-height: 280mm; margin: 0 auto; background: #fff; padding: 12mm 14mm;
  box-shadow: 0 2px 16px rgba(30, 111, 168, .10); box-sizing: border-box;
  color: #1f2733; font-size: 12px;
}
.cr-head { text-align: center; border-bottom: 2px solid #1E6FA8; padding-bottom: 8px; }
.cr-hosp { font-size: 15px; font-weight: 700; }
.cr-title { font-size: 19px; font-weight: 800; letter-spacing: 8px; margin: 4px 0; color: #1E6FA8; }
.cr-meta { display: flex; justify-content: space-between; font-size: 11.5px; color: #444; }
.cr-info { width: 100%; border-collapse: collapse; margin: 8px 0; td { padding: 4px 2px; } }
.cr-sec { font-weight: 700; margin: 10px 0 5px; padding-left: 6px; border-left: 3px solid #1E6FA8; }
.cr-focus { background: #fff7e6; }
.cr-kv { display: flex; gap: 8px; border-bottom: 1px dashed #dbe3ee; padding: 3px 0; }
.cr-kv span { width: 62px; flex: none; color: #5a687e; }
.cr-kv p { margin: 0; flex: 1; white-space: pre-wrap; line-height: 1.6; }
.cr-table { width: 100%; border-collapse: collapse; th, td { border: 1px solid #b9c4d2; padding: 4px 6px; text-align: left; font-size: 11.5px; } th { background: #eef4fa; } }
.cr-lines { line-height: 1.8; }
.cr-empty { text-align: center; color: #8a93a6; padding: 6px; }
.cr-sign { display: flex; justify-content: space-between; margin-top: 16px; }
.cr-foot { margin-top: 8px; font-size: 10.5px; color: #6b7686; text-align: center; }
.cr-none { text-align: center; color: #8a93a6; margin: 60px auto; }
@page { size: A4 portrait; margin: 12mm; }
@media print {
  .cr-page { padding: 0; background: #fff; }
  .cr-sheet { box-shadow: none; width: auto; min-height: auto; padding: 0; }
}
</style>
