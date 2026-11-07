<template>
  <div class="rx-page">
    <!-- 工具栏（不打印） -->
    <div class="no-print rx-toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" :disabled="!report" @click="doPrint">打印报告单</el-button>
      <span v-if="!hasResult" style="color:#e6a23c;margin-left:10px">尚未出结果，打印仅为空白样张</span>
    </div>

    <div v-if="report" class="rx-sheet">
      <div class="rx-title">{{ report.type === 'image' ? '影像检查报告单' : '检验报告单' }}</div>
      <div class="rx-no">编号：{{ report.orderNo }}</div>

      <!-- 前记 -->
      <table class="rx-table">
        <tbody>
          <tr>
            <td class="k">畜主</td><td class="v">{{ report.clientName }}</td>
            <td class="k">宠物名</td><td class="v">{{ report.petName }}</td>
            <td class="k">品种</td><td class="v">{{ report.petVariety || '—' }}</td>
          </tr>
          <tr>
            <td class="k">性别</td><td class="v">{{ report.petSex == null ? '—' : (report.petSex ? '公' : '母') }}</td>
            <td class="k">体重</td><td class="v">{{ report.petWeight != null ? report.petWeight + 'kg' : '—' }}</td>
            <td class="k">年龄</td><td class="v">{{ report.petBirthday ? getAge(report.petBirthday) : '—' }}</td>
          </tr>
          <tr>
            <td class="k">芯片号</td><td class="v" colspan="2">{{ report.petChip || '—' }}</td>
            <td class="k">报告日期</td><td class="v">{{ fmt(report.resultTime || report.createTime) }}</td>
          </tr>
          <tr>
            <td class="k">临床诊断</td><td class="v" colspan="4">{{ report.diagnosis || '—' }}</td>
          </tr>
        </tbody>
      </table>

      <!-- 检验：项目结果表 -->
      <template v-if="report.type !== 'image'">
        <div class="rx-rp">检验结果</div>
        <table class="rx-table rx-items">
          <thead>
            <tr>
              <th style="width: 24%">检验项</th>
              <th style="width: 16%">结果</th>
              <th style="width: 18%">参考范围</th>
              <th style="width: 10%">单位</th>
              <th style="width: 12%">提示</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(it, i) in report.items" :key="i">
              <td>{{ it.name }}</td>
              <td :class="{abn: it.abnormal}">{{ it.resultValue || '—' }}</td>
              <td>{{ it.refRange || '—' }}</td>
              <td>{{ it.unit || '—' }}</td>
              <td :class="{abn: it.abnormal}">{{ it.abnormal ? '异常 ↑' : '' }}</td>
            </tr>
            <tr v-if="!report.items.length">
              <td colspan="5" style="text-align:center;color:#999">无检验项目</td>
            </tr>
          </tbody>
        </table>
        <div class="rx-note">
          <b>结果说明：</b>{{ report.resultNote || '（无）' }}
        </div>
        <div class="rx-note" v-if="report.criticalFlag" style="color:#d03050">
          <b>危急值提示：</b>本单存在危急值，请立即联系接诊医师并留存处置记录。
        </div>
      </template>

      <!-- 影像：检查项目 + 报告文本 -->
      <template v-else>
        <div class="rx-rp">检查项目</div>
        <table class="rx-table">
          <tbody>
            <tr>
              <td class="k">项目</td><td class="v">{{ report.examPart ? report.itemName + '（' + report.examPart + '）' : report.itemName }}</td>
              <td class="k">费用</td><td class="v">￥{{ (report.fee ?? 0).toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
        <div class="rx-rp">影像报告</div>
        <div v-if="report.findings || report.impression || report.resultText">
          <div v-if="report.findings" class="rx-report-text"><b>影像所见：</b>{{ report.findings }}</div>
          <div v-if="report.impression" class="rx-report-text"><b>影像诊断：</b>{{ report.impression }}</div>
          <div v-if="report.resultText" class="rx-report-text">{{ report.resultText }}</div>
        </div>
        <div v-else class="rx-report-text">（暂无报告内容）</div>
        <div v-if="report.dicomUid || report.dicomSeries" class="rx-note">
          <b>DICOM：</b>{{ report.dicomUid || '—' }} / {{ report.dicomSeries || '—' }}
        </div>
        <div v-if="imageFileList.length" class="rx-note">
          <b>原图附件：</b>
          <span v-for="(f, i) in imageFileList" :key="i" style="margin-right:14px">{{ f }}</span>
        </div>
      </template>

      <!-- 后记 -->
      <table class="rx-table rx-sign">
        <tbody>
          <tr>
            <td class="k">报告医师</td><td class="v sign">{{ report.resultBy || '（待录入）' }}</td>
            <td class="k">审核医师</td><td class="v sign">{{ report.reviewBy || '（未复核）' }}</td>
          </tr>
          <tr>
            <td class="k">报告日期</td><td class="v">{{ fmt(report.resultTime || report.createTime) }}</td>
            <td class="k">备注</td><td class="v">{{ report.remark || '—' }}</td>
          </tr>
          <tr v-if="report.type === 'lab'">
            <td class="k">标本类型</td><td class="v">{{ report.specimenType || '—' }}</td>
            <td class="k">采样 / 接收</td><td class="v">{{ fmt(report.collectTime) || '—' }} / {{ fmt(report.receiveTime) || '—' }}</td>
          </tr>
        </tbody>
      </table>

      <div class="rx-foot">本报告仅供本院诊疗参考；异常结果请结合临床综合判断。</div>
      <!-- 打印时间仅作留痕小字 -->
      <div class="rx-print-time">打印时间：{{ nowText() }}</div>
    </div>

    <div v-else-if="!loading" class="no-print rx-empty">未找到该报告单</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {reqImageRequisition} from "@/request/ImageApi";
import {reqLabRequisition, reqLabByRecord} from "@/request/LabApi";
import {reqImageByRecord} from "@/request/ImageApi";
import {Requisition} from "@/model/entiy/Requisition";
import {getAge} from "@/utils/TimeUtil";

const route = useRoute()
const router = useRouter()
const loading = ref(true)

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

onMounted(() => {
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
})

const imageFileList = computed<string[]>(() => {
  if (!report.value?.imageFiles) return []
  try { const a = JSON.parse(report.value.imageFiles); return Array.isArray(a) ? a : [] } catch { return [] }
})
const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)
const nowText = (): string => new Date().toLocaleString('zh-CN')
const doPrint = (): void => window.print()
</script>

<style lang="scss" scoped>
.rx-page { padding: 20px; background: #f5f6fa; min-height: 100vh; }
.rx-toolbar { max-width: 720px; margin: 0 auto 16px; text-align: right; }
.rx-sheet {
  max-width: 720px; margin: 0 auto; background: #fff;
  border: 2px solid #333; padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  position: relative;
}
.rx-title { text-align: center; font-size: 24px; font-weight: 800; letter-spacing: 4px; margin-bottom: 4px; }
.rx-no { text-align: right; font-size: 13px; color: #555; margin-bottom: 12px; }
.rx-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
.rx-table td, .rx-table th { border: 1px solid #333; padding: 7px 8px; font-size: 13px; }
.rx-table .k { background: #f0f2f5; width: 76px; text-align: center; white-space: nowrap; }
.rx-table .v { min-width: 60px; }
.rx-table .sign { color: #666; letter-spacing: 1px; }
.rx-table .abn { color: #d4380d; font-weight: 700; }
.rx-items th { background: #f0f2f5; text-align: center; }
.rx-rp { font-weight: 700; margin: 6px 0; font-size: 14px; }
.rx-note {
  border: 1px solid #333; padding: 8px 10px; font-size: 12px;
  color: #303133; line-height: 1.7; margin-bottom: 12px; min-height: 20px;
}
.rx-report-text {
  border: 1px solid #333; padding: 12px 14px; font-size: 13px;
  line-height: 1.9; white-space: pre-wrap; min-height: 120px; margin-bottom: 12px;
}
.rx-sign td { height: 38px; }
.rx-foot { font-size: 11px; color: #888; margin-top: 8px; }
.rx-print-time { position: absolute; right: 28px; bottom: 8px; font-size: 9px; color: #aaa; }
.rx-empty { text-align: center; color: #999; margin-top: 60px; }

@media print {
  .rx-page { padding: 0; background: #fff; }
  .no-print { display: none !important; }
  .rx-sheet { box-shadow: none; border: 2px solid #000; max-width: 100%; margin: 0; }
}
</style>
