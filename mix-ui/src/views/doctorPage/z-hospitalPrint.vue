<template>
  <div class="hp-page">
    <div class="no-print hp-toolbar">
      <el-button @click="closeWin">返回</el-button>
      <el-button :disabled="!h" type="primary" @click="doPrint">打印住院记录单</el-button>
      <span class="hp-hint">版式移植自老系统 ReportInhosLog（A4 竖版）</span>
    </div>

    <div v-if="h" class="hp-sheet">
      <div class="hp-head">
        <div class="hp-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
        <div class="hp-title">住 院 记 录 单</div>
        <div class="hp-meta">
          <span>住院单号：H{{ String(h.hospitalId).padStart(6, '0') }}</span>
          <span>病例号：No.{{ h.recordId }}</span>
          <span>状态：{{ h.status ? '已出院' : '住院中' }}</span>
        </div>
      </div>

      <table class="hp-info">
        <tr>
          <td><b>主人名称：</b>{{ h.clientName || '—' }}</td>
          <td><b>动物名称：</b>{{ h.petName || '—' }}</td>
          <td><b>病房：</b>{{ h.roomNo || '—' }}</td>
          <td><b>住院部医师：</b>{{ h.doctorName || '—' }}</td>
        </tr>
        <tr>
          <td><b>入院时间：</b>{{ fmt(h.inTime) }}</td>
          <td><b>出院时间：</b>{{ h.outTime ? fmt(h.outTime) : '—' }}</td>
          <td><b>住院天数：</b>{{ h.days ?? '—' }}</td>
          <td><b>日均费用：</b>{{ h.dailyFee != null ? '￥' + Number(h.dailyFee).toFixed(2) : '—' }}</td>
        </tr>
      </table>

      <div class="hp-sec">住院原因 / 诊断</div>
      <div class="hp-text">{{ h.diagnosis || '—' }}</div>

      <div class="hp-sec">住院说明</div>
      <div class="hp-text">{{ h.note || '—' }}</div>

      <div class="hp-sec">每日监护记录（共 {{ monitors.length }} 条）</div>
      <table class="hp-mon">
        <thead>
        <tr>
          <th style="width:11%">时间</th>
          <th style="width:7%">体温</th>
          <th style="width:7%">心率</th>
          <th style="width:8%">呼吸</th>
          <th style="width:7%">体重</th>
          <th style="width:9%">精神</th>
          <th style="width:9%">食欲</th>
          <th style="width:9%">排泄</th>
          <th style="width:14%">用药/治疗</th>
          <th style="width:11%">备注</th>
          <th style="width:8%">记录人</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(m, i) in monitors" :key="i">
          <td>{{ fmt(m.monitorTime) }}</td>
          <td>{{ m.temperature ?? '—' }}</td>
          <td>{{ m.heartRate ?? '—' }}</td>
          <td>{{ m.respiratoryRate ?? '—' }}</td>
          <td>{{ m.weight ?? '—' }}</td>
          <td>{{ m.mentalState || '—' }}</td>
          <td>{{ m.appetite || '—' }}</td>
          <td>{{ m.excretion || '—' }}</td>
          <td>{{ [m.medication, m.treatment].filter(Boolean).join(' / ') || '—' }}</td>
          <td>{{ m.note || '—' }}</td>
          <td>{{ m.recorder || '—' }}</td>
        </tr>
        <tr v-if="!monitors.length">
          <td class="hp-empty" colspan="11">暂无监护记录</td>
        </tr>
        </tbody>
      </table>

      <div class="hp-sign">
        <span>住院部医师签名：＿＿＿＿＿＿＿</span>
        <span>打印时间：{{ nowText() }}</span>
      </div>
      <div class="hp-foot">注：本记录单由本院诊疗系统生成，仅对所记录的在院观察情况负责。</div>
    </div>

    <div v-else-if="loaded" class="no-print hp-empty-page">未找到该病历的住院记录</div>
  </div>
</template>

<script lang="ts" setup>
import {onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import {HospitalDto} from '@/model/DO/HospitalDto'
import {HospitalMonitor} from '@/model/entiy/HospitalMonitor'
import {reqHospitalByRecord, reqHospitalMonitorList} from '@/request/HospitalApi'
import {useBrand} from '@/utils/UseBrand'

const route = useRoute()
const {hosp, load: loadBrand} = useBrand()
const h = ref<HospitalDto | null>(null)
const monitors = ref<HospitalMonitor[]>([])
const loaded = ref(false)

let pageStyle: HTMLStyleElement | null = null
const mountPageStyle = (): void => {
  pageStyle = document.createElement('style')
  pageStyle.textContent = '@page { size: A4 portrait; margin: 12mm; }'
  document.head.appendChild(pageStyle)
}

onMounted(() => {
  loadBrand()
  mountPageStyle()
  const recordId = Number(route.query.recordId)
  const hospitalId = Number(route.query.hospitalId)
  if (!recordId && !hospitalId) { loaded.value = true; return }
  const pick = (list: HospitalDto[]): HospitalDto | null => {
    if (!list?.length) return null
    if (hospitalId) return list.find(x => x.hospitalId === hospitalId) || list[0]
    return list.slice().sort((a, b) => (b.hospitalId - a.hospitalId))[0]
  }
  reqHospitalByRecord(recordId).then(res => {
    h.value = pick(res || [])
    loaded.value = true
    if (h.value) {
      reqHospitalMonitorList(h.value.hospitalId).then(m => { monitors.value = m || [] }).catch(() => undefined)
    }
    if (route.query.auto === '1') setTimeout(() => window.print(), 700)
  }).catch(() => { loaded.value = true })
})
onBeforeUnmount(() => pageStyle?.remove())

const fmt = (t: string | null | undefined): string => (t || '').replace('T', ' ').slice(0, 16) || '—'
const nowText = (): string => new Date().toLocaleString('zh-CN')
const doPrint = (): void => window.print()
const closeWin = (): void => window.close()
</script>

<style lang="scss" scoped>
.hp-page { padding: 16px; background: #f3f6fb; min-height: 100vh; }
.hp-toolbar { max-width: 210mm; margin: 0 auto 12px; display: flex; align-items: center; gap: 10px; }
.hp-hint { color: #8a93a6; font-size: 12px; }
.hp-sheet {
  width: 210mm; min-height: 280mm; margin: 0 auto; background: #fff; padding: 10mm 12mm;
  box-shadow: 0 2px 16px rgba(30, 111, 168, .10); border-radius: 4px;
  color: #1f2733; font-size: 12px; box-sizing: border-box;
}
.hp-head { text-align: center; border-bottom: 2px solid #1E6FA8; padding-bottom: 6px; margin-bottom: 8px; }
.hp-hosp { font-size: 15px; font-weight: 700; letter-spacing: 1px; }
.hp-title { font-size: 20px; font-weight: 800; letter-spacing: 8px; margin-top: 2px; color: #1E6FA8; }
.hp-meta { display: flex; justify-content: space-between; margin-top: 6px; font-size: 11.5px; color: #444; }
.hp-info { width: 100%; border-collapse: collapse; margin-bottom: 4px; td { padding: 3px 2px; } }
.hp-sec { font-weight: 700; margin: 8px 0 4px; }
.hp-text {
  border: 1px dashed #c7d2e0; border-radius: 3px; padding: 5px 6px; min-height: 10mm;
  white-space: pre-wrap; line-height: 1.6;
}
.hp-mon {
  width: 100%; border-collapse: collapse;
  th, td { border: 1px solid #b9c4d2; padding: 3px 4px; font-size: 10.5px; text-align: left; }
  th { background: #eef4fa; font-weight: 600; }
  .hp-empty { text-align: center; color: #8a93a6; padding: 8px; }
}
.hp-sign { display: flex; justify-content: space-between; margin-top: 10px; }
.hp-foot { margin-top: 6px; font-size: 10.5px; color: #6b7686; text-align: center; }
.hp-empty-page { text-align: center; color: #8a93a6; margin: 40px auto; }
@media print {
  .hp-page { padding: 0; background: #fff; }
  .hp-sheet { box-shadow: none; border-radius: 0; width: auto; min-height: auto; padding: 0; }
}
</style>
