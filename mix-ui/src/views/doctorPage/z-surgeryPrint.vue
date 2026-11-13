<template>
  <div class="sp-page">
    <div class="no-print sp-toolbar">
      <el-button @click="closeWin">返回</el-button>
      <el-button :disabled="!s" type="primary" @click="doPrint">打印手术记录单</el-button>
      <span class="sp-hint">版式移植自老系统 ReportOper（A5 横向）</span>
    </div>

    <div v-if="s" class="sp-sheet">
      <div class="sp-head">
        <div class="sp-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
        <div class="sp-title">手 术 记 录 单</div>
        <div class="sp-meta">
          <span>手术单号：S{{ String(s.surgeryId).padStart(6, '0') }}</span>
          <span>病例号：No.{{ s.recordId }}</span>
          <span>状态：{{ statusText(s.status) }}</span>
        </div>
      </div>

      <table class="sp-info">
        <tr>
          <td><b>主人名称：</b>{{ s.clientName || '—' }}</td>
          <td><b>动物名称：</b>{{ s.petName || '—' }}</td>
          <td><b>手术医师：</b>{{ s.surgeon || s.doctorName || '—' }}</td>
          <td><b>助手：</b>{{ s.assistant || '—' }}</td>
        </tr>
        <tr>
          <td><b>手术名称：</b>{{ s.surgeryName || '—' }}</td>
          <td><b>手术类型：</b>{{ s.surgeryType || '—' }}</td>
          <td><b>麻醉方式：</b>{{ s.anesthesiaType || '—' }}</td>
          <td><b>时长：</b>预计 {{ scheduledMinutes != null ? scheduledMinutes + ' 分钟' : '—' }} / 实际 {{ actualMinutes != null ? actualMinutes + ' 分钟' : '—' }}</td>
        </tr>
      </table>

      <div class="sp-grid">
        <div class="sp-block">
          <div class="sp-sec">术前准备</div>
          <div class="sp-text">{{ s.preopNote || '—' }}</div>
        </div>
        <div class="sp-block">
          <div class="sp-sec">过程记录</div>
          <div class="sp-text">{{ s.intraopNote || '—' }}</div>
        </div>
        <div class="sp-block">
          <div class="sp-sec">术后医嘱</div>
          <div class="sp-text">{{ s.postopNote || '—' }}</div>
        </div>
      </div>

      <div class="sp-sign">
        <span>手术医师签名：＿＿＿＿＿＿＿</span>
        <span>手术费用：{{ s.fee != null ? '￥' + Number(s.fee).toFixed(2) : '—' }}</span>
        <span>打印时间：{{ nowText() }}</span>
      </div>
      <div class="sp-foot">（此方三日内有效）　　本记录单记录本次手术过程，术后观察与并发症风险已另行告知。</div>
    </div>

    <div v-else-if="loaded" class="no-print sp-empty-page">未找到该手术记录</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import {Surgery} from '@/model/entiy/Surgery'
import {reqSurgeryOne} from '@/request/SurgeryApi'
import {useBrand} from '@/utils/UseBrand'

const route = useRoute()
const {hosp, load: loadBrand} = useBrand()
const s = ref<Surgery | null>(null)
const loaded = ref(false)

let pageStyle: HTMLStyleElement | null = null
const mountPageStyle = (): void => {
  pageStyle = document.createElement('style')
  pageStyle.textContent = '@page { size: A5 landscape; margin: 6mm; }'
  document.head.appendChild(pageStyle)
}

const minutes = (a?: string | null, b?: string | null): number | null => {
  if (!a || !b) return null
  const t1 = new Date(a.replace(' ', 'T')).getTime()
  const t2 = new Date(b.replace(' ', 'T')).getTime()
  if (Number.isNaN(t1) || Number.isNaN(t2) || t2 < t1) return null
  return Math.round((t2 - t1) / 60000)
}
const scheduledMinutes = computed<number | null>(() => {
  const v = s.value as any
  return minutes(v?.scheduledTime, v?.startTime)
})
const actualMinutes = computed<number | null>(() => minutes(s.value?.startTime, s.value?.endTime))

onMounted(() => {
  loadBrand()
  mountPageStyle()
  const surgeryId = Number(route.query.surgeryId)
  if (!surgeryId) { loaded.value = true; return }
  reqSurgeryOne(surgeryId).then(res => {
    s.value = res
    loaded.value = true
    if (route.query.auto === '1') setTimeout(() => window.print(), 600)
  }).catch(() => { loaded.value = true })
})
onBeforeUnmount(() => pageStyle?.remove())

const statusText = (st: number): string =>
  ({0: '待手术', 1: '手术中', 2: '已完成', 3: '已取消'} as Record<number, string>)[st] ?? '—'
const nowText = (): string => new Date().toLocaleString('zh-CN')
const doPrint = (): void => window.print()
const closeWin = (): void => window.close()
</script>

<style lang="scss" scoped>
.sp-page { padding: 14px; background: #f3f6fb; min-height: 100vh; }
.sp-toolbar { max-width: 210mm; margin: 0 auto 12px; display: flex; align-items: center; gap: 10px; }
.sp-hint { color: #8a93a6; font-size: 12px; }
.sp-sheet {
  width: 210mm; min-height: 140mm; margin: 0 auto; background: #fff; padding: 8mm 10mm;
  box-shadow: 0 2px 16px rgba(30, 111, 168, .10); border-radius: 4px;
  color: #1f2733; font-size: 12px; box-sizing: border-box;
}
.sp-head { text-align: center; border-bottom: 2px solid #1E6FA8; padding-bottom: 5px; margin-bottom: 7px; }
.sp-hosp { font-size: 14px; font-weight: 700; letter-spacing: 1px; }
.sp-title { font-size: 19px; font-weight: 800; letter-spacing: 8px; margin-top: 2px; color: #1E6FA8; }
.sp-meta { display: flex; justify-content: space-between; margin-top: 5px; font-size: 11px; color: #444; }
.sp-info { width: 100%; border-collapse: collapse; margin-bottom: 6px; td { padding: 3px 2px; font-size: 11.5px; } }
.sp-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; }
.sp-sec { font-weight: 700; margin-bottom: 3px; font-size: 12px; }
.sp-text {
  border: 1px dashed #c7d2e0; border-radius: 3px; padding: 5px;
  min-height: 26mm; white-space: pre-wrap; line-height: 1.55; font-size: 11.5px;
}
.sp-sign { display: flex; justify-content: space-between; margin-top: 10px; font-size: 11.5px; }
.sp-foot { margin-top: 5px; font-size: 10.5px; color: #6b7686; text-align: center; }
.sp-empty-page { text-align: center; color: #8a93a6; margin: 40px auto; }
@media print {
  .sp-page { padding: 0; background: #fff; }
  .sp-sheet { box-shadow: none; border-radius: 0; width: auto; min-height: auto; padding: 0; }
}
</style>
