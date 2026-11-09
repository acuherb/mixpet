<template>
  <ReportSheet v-if="report"
               :title="report.type === 'image' ? '影像检查报告' : '检验报告'"
               :doc-no="report.orderNo"
               :hosp-name="hosp.name"
               :brand="brand"
               :fields="patientFields"
               :signers="signerFields"
               print-time>
    <template #toolbar>
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" @click="doPrint">打印报告单</el-button>
      <span v-if="!hasResult" class="rm-muted">尚未出结果，打印仅为空白样张</span>
    </template>

    <section class="rm-sec">
      <div class="rm-sec-title">临床诊断</div>
      <div class="rm-text">{{ report.diagnosis || '—' }}</div>
    </section>

    <template v-if="report.type !== 'image'">
      <section class="rm-sec">
        <div class="rm-sec-title">检验结果</div>
        <table class="rm-table">
          <thead>
          <tr><th>检验项目</th><th>结果</th><th>参考范围</th><th>单位</th><th>提示</th></tr>
          </thead>
          <tbody>
          <tr v-for="(it, i) in report.items" :key="i">
            <td>{{ it.name }}</td>
            <td :class="{'rm-abn': it.abnormal}">{{ it.resultValue || '—' }}</td>
            <td class="rm-muted">{{ it.refRange || '—' }}</td>
            <td class="rm-muted">{{ it.unit || '—' }}</td>
            <td><span v-if="it.abnormal" class="rm-chip">异常</span><span v-else class="rm-ok">正常</span></td>
          </tr>
          <tr v-if="!report.items.length"><td colspan="5" class="rm-muted">无检验项目</td></tr>
          </tbody>
        </table>
      </section>

      <section v-if="report.resultNote" class="rm-sec">
        <div class="rm-sec-title">结果说明</div>
        <div class="rm-text">{{ report.resultNote }}</div>
      </section>

      <div v-if="report.criticalFlag" class="rm-alert">
        危急值提示：本单存在危急值，请立即联系接诊医师并留存处置记录。
      </div>

      <section class="rm-sec">
        <div class="rm-sec-title">标本与时间</div>
        <div class="rm-kv"><span class="rm-k">标本类型</span><span>{{ report.specimenType || '—' }}</span></div>
        <div class="rm-kv"><span class="rm-k">采样 / 接收</span><span>{{ fmt(report.collectTime) || '—' }} / {{ fmt(report.receiveTime) || '—' }}</span></div>
      </section>
    </template>

    <template v-else>
      <section class="rm-sec">
        <div class="rm-sec-title">检查项目</div>
        <div class="rm-kv"><span class="rm-k">项目</span><span>{{ report.examPart ? report.itemName + '（' + report.examPart + '）' : report.itemName }}</span></div>
        <div class="rm-kv"><span class="rm-k">费用</span><span>￥{{ (report.fee ?? 0).toFixed(2) }}</span></div>
      </section>

      <section class="rm-sec">
        <div class="rm-sec-title">影像报告</div>
        <div v-if="report.findings" class="rm-kv"><span class="rm-k">影像所见</span><span class="rm-text">{{ report.findings }}</span></div>
        <div v-if="report.impression" class="rm-kv"><span class="rm-k">影像诊断</span><span class="rm-text">{{ report.impression }}</span></div>
        <div v-if="report.resultText" class="rm-text">{{ report.resultText }}</div>
        <div v-if="!report.findings && !report.impression && !report.resultText" class="rm-muted">暂无报告内容</div>
      </section>

      <section v-if="report.dicomUid || report.dicomSeries || imageFileList.length" class="rm-sec">
        <div class="rm-sec-title">影像资料</div>
        <div v-if="report.dicomUid || report.dicomSeries" class="rm-kv"><span class="rm-k">DICOM</span><span>{{ report.dicomUid || '—' }} / {{ report.dicomSeries || '—' }}</span></div>
        <div v-if="imageFileList.length" class="rm-kv"><span class="rm-k">原图附件</span><span>{{ imageFileList.join('、') }}</span></div>
      </section>
    </template>

    <div class="rm-note">异常结果请结合临床综合判断；本报告仅供本院诊疗参考。</div>
  </ReportSheet>

  <div v-else class="rm-page">
    <div style="text-align:center;color:#8a93a6;padding:80px 0">{{ loading ? '正在加载报告数据…' : '未找到该报告单，请返回检查单号' }}</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {reqImageRequisition} from "@/request/ImageApi";
import {reqLabRequisition, reqLabByRecord} from "@/request/LabApi";
import {reqImageByRecord} from "@/request/ImageApi";
import {Requisition} from "@/model/entiy/Requisition";
import {getAge} from "@/utils/TimeUtil";
import {useBrand} from "@/utils/UseBrand";
import ReportSheet from "@/components/sheet/ReportSheet.vue";

const route = useRoute()
const router = useRouter()
const loading = ref(true)
// 医院名与品牌色：所有报告共用同一套品牌，后台改配置即时生效
const {hosp, brand, load: loadBrand} = useBrand()

/** 报告单数据（申请单基础信息 + 结果明细合并） */
interface ReportData {
  type: string
  orderNo: string
  createTime: string
  resultTime: string | null
  clientName: string
  petName: string
  petVariety: string | null
  petSex: boolean | null
  petBirthday: string | null
  petWeight: number | null
  petChip: string | null
  diagnosis: string
  remark: string | null
  fee: number
  // 检验
  items: { name: string; refRange: string | null; unit: string | null; resultValue: string | null; abnormal: boolean }[]
  resultNote: string | null
  // 临床时间轴与双签（检验报告）
  specimenType: string | null
  collectTime: string | null
  receiveTime: string | null
  resultBy: string | null
  reviewBy: string | null
  criticalFlag: boolean
  // 影像
  itemName: string
  examPart: string | null
  resultText: string | null
  findings: string | null
  impression: string | null
  dicomUid: string | null
  dicomSeries: string | null
  imageFiles: string | null
}
const report = ref<ReportData | null>(null)
/** 是否已出结果（无结果的报告单打印时提示） */
const hasResult = ref(false)

const loadData = (): void => {
  report.value = null
  hasResult.value = false
  loading.value = true
  const orderId = Number(route.query.orderId)
  const recordId = Number(route.query.recordId)
  const type = route.query.type === 'image' ? 'image' : 'lab'
  if (!orderId) { loading.value = false; return }
  const baseApi = type === 'image' ? reqImageRequisition : reqLabRequisition
  const detailApi = type === 'image' ? reqImageByRecord : reqLabByRecord
  baseApi(orderId).then((base: Requisition) => {
    const data: ReportData = {
      type,
      orderNo: base.orderNo,
      createTime: base.createTime,
      resultTime: null,
      clientName: base.clientName,
      petName: base.petName,
      petVariety: base.petVariety,
      petSex: base.petSex,
      petBirthday: base.petBirthday,
      petWeight: base.petWeight,
      petChip: base.petChip,
      diagnosis: base.diagnosis,
      remark: base.remark,
      fee: base.fee ?? 0,
      items: [],
      resultNote: null,
      specimenType: null,
      collectTime: null,
      receiveTime: null,
      resultBy: null,
      reviewBy: null,
      criticalFlag: false,
      itemName: '',
      examPart: base.examPart ?? null,
      resultText: null,
      findings: null,
      impression: null,
      dicomUid: null,
      dicomSeries: null,
      imageFiles: null
    }
    if (!recordId) { report.value = data; loading.value = false; return }
    detailApi(recordId).then(list => {
      const hit: any = (list || []).find((o: any) => o.orderId === orderId)
      if (hit) {
        if (type === 'image') {
          data.resultTime = hit.reportTime ?? null
          data.itemName = hit.itemName || ''
          data.resultText = hit.result || null
          data.findings = hit.findings ?? null
          data.impression = hit.impression ?? null
          data.dicomUid = hit.dicomUid ?? null
          data.dicomSeries = hit.dicomSeries ?? null
          data.imageFiles = hit.imageFiles ?? null
          data.fee = hit.fee ?? data.fee
          hasResult.value = !!hit.status
        } else {
          data.resultTime = hit.resultTime ?? null
          data.resultNote = hit.resultNote ?? null
          data.specimenType = hit.specimenType ?? null
          data.collectTime = hit.collectTime ?? null
          data.receiveTime = hit.receiveTime ?? null
          data.resultBy = hit.resultBy ?? null
          data.reviewBy = hit.reviewBy ?? null
          data.criticalFlag = !!hit.criticalFlag
          data.items = (hit.items || []).map((i: any) => ({
            name: i.itemName,
            refRange: i.refRange,
            unit: i.itemUnit,
            resultValue: i.resultValue,
            abnormal: !!i.isAbnormal
          }))
          hasResult.value = !!hit.status && data.items.some(i => i.resultValue)
        }
      }
      report.value = data
      loading.value = false
    }).catch(() => { report.value = data; loading.value = false })
  }).catch(() => { loading.value = false })
}
onMounted(loadBrand)
// 同一路由只换 orderId/type 时 Vue 复用组件，onMounted 不会再跑，必须显式重取
watch(() => [route.query.orderId, route.query.recordId, route.query.type], loadData, {immediate: true})

const imageFileList = computed<string[]>(() => {
  if (!report.value?.imageFiles) return []
  try { const a = JSON.parse(report.value.imageFiles); return Array.isArray(a) ? a : [] } catch { return [] }
})
const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)

// 统一外壳的患者信息条与签名区：字段不减，只是不再用黑白公文表格堆
const patientFields = computed(() => {
  const r = report.value
  if (!r) return []
  return [
    {label: '动物主人', value: r.clientName},
    {label: '宠物名', value: r.petName},
    {label: '品种', value: r.petVariety || '—'},
    {label: '性别', value: r.petSex == null ? '—' : (r.petSex ? '公' : '母')},
    {label: '年龄', value: r.petBirthday ? getAge(r.petBirthday) : '—'},
    {label: '体重', value: r.petWeight != null ? r.petWeight + ' kg' : '—'},
    {label: '芯片号', value: r.petChip || '—'},
    {label: '报告日期', value: fmt(r.resultTime || r.createTime) || '—'}
  ]
})
const signerFields = computed(() => {
  const r = report.value
  if (!r) return []
  return [
    {label: '报告医师', value: r.resultBy || '待录入'},
    {label: '审核医师', value: r.reviewBy || '未复核'},
    {label: '报告日期', value: fmt(r.resultTime || r.createTime) || '—'},
    {label: '备注', value: r.remark || '—'}
  ]
})
const doPrint = (): void => window.print()
</script>


