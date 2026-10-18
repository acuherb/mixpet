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
      <!-- 按联次渲染纸张：copies=1 仅第三联(动物主人留存)；copies=3 三联全打 -->
      <div v-for="pg in pages" :key="pg.no" class="rx-sheet">
        <!-- 联次标识：右侧竖排（734号样式，文字标注不用颜色区分） -->
        <div class="rx-copy">{{ pg.label }}</div>

        <!-- 标题：机构名 + 处方笺（734号样式1） -->
        <div class="rx-title">{{ hosp.name || 'XXXXXXXX' }} 处方笺</div>

        <!-- 前记：下划线横排填写式 -->
        <div class="rx-front">
          <p>动物主人/饲养单位 <span class="fill">{{ rx.clientName }}</span> 病历号 <span class="fill">No.{{ rx.recordId }}</span></p>
          <p>动物种类 <span class="fill">{{ rx.species || '　' }}</span> 动物性别 <span class="fill">{{ rx.petSex == null ? '　' : (rx.petSex ? '公' : '母') }}</span> 动物毛色 <span class="fill">{{ rx.hairColor || '　' }}</span></p>
          <p>体重 <span class="fill">{{ rx.petWeight != null ? rx.petWeight + 'kg' : '　' }}</span> 年（日）龄 <span class="fill">{{ rx.petBirthday ? getAge(rx.petBirthday) : '　' }}</span> 开具日期 <span class="fill">{{ fmt(rx.prescriptionTime || rx.createTime) }}</span></p>
        </div>

        <!-- 正文：左「诊断：」右「Rp:」分栏大框（734号样式1） -->
        <div class="rx-body">
          <div class="rx-diag-col">
            <span class="col-tit">诊断：</span>
            <div class="diag-text">{{ rx.diagnosis || '　' }}</div>
          </div>
          <div class="rx-rp-col">
            <span class="col-tit">Rp:</span>
            <table class="rx-items">
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
            <div class="rx-amount">金额（小写）：￥{{ (rx.totalAmount ?? 0).toFixed(2) }}</div>
          </div>
        </div>

        <!-- 后记：下划线式（执业兽医师 / 发药人） -->
        <div class="rx-sign">
          <span>执业兽医师 <span class="fill sign-line">（{{ rx.doctorName }}）</span></span>
          <span>注册号 <span class="fill">{{ rx.doctorCert || '　　　　' }}</span></span>
          <span>发药人 <span class="fill sign-line">（签名/盖章）</span></span>
        </div>

        <div class="rx-foot">注：“{{ hosp.name || 'xxxxxxx' }} 处方笺”中，“{{ hosp.name || 'xxxxxxx' }}”为从事动物诊疗活动的单位名称。</div>
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
  border: 1px solid #000;
  padding: 14px 18px 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
  position: relative;
}
/* 联次标识：右侧竖排（734号样式） */
.rx-copy {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  writing-mode: vertical-rl;
  font-size: 11px;
  letter-spacing: 2px;
  color: #333;
}
.rx-title {
  text-align: center;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 3px;
  margin: 2px 0 10px;
}
/* 前记：下划线横排填写式（无表格线，贴近官方样式） */
.rx-front {
  padding: 0 14px;
  p {
    margin: 0 0 10px;
    font-size: 12px;
    color: #222;
    line-height: 1.9;
  }
  .fill {
    display: inline-block;
    min-width: 70px;
    border-bottom: 1px solid #333;
    text-align: center;
    padding: 0 4px;
  }
}
/* 正文：诊断 | Rp 分栏大框 */
.rx-body {
  display: flex;
  border: 1px solid #000;
  min-height: 88mm;
  margin: 0 14px;
}
.rx-diag-col {
  width: 34%;
  border-right: 1px solid #000;
  padding: 6px 10px;
  .diag-text {
    font-size: 12px;
    line-height: 1.8;
    color: #222;
    white-space: pre-wrap;
  }
}
.rx-rp-col {
  flex: 1;
  padding: 6px 10px;
  min-width: 0;
}
.col-tit {
  font-size: 13px;
  font-weight: 700;
  color: #111;
}
.rx-items {
  width: 100%;
  border-collapse: collapse;
  margin-top: 4px;
  th, td {
    border: 1px solid #333;
    padding: 3px 5px;
    font-size: 11px;
  }
  th { background: #f0f2f5; text-align: center; }
}
/* Rp 后空白处：打印前自动划斜线（斜线条纹，无假数据行） */
.rx-blank td {
  height: 20px;
  background: repeating-linear-gradient(45deg, transparent 0 10px, #ddd 10px 11px);
}
.rx-blank-end td {
  text-align: center;
  font-size: 11px;
  color: #666;
  height: 16px;
}
.rx-amount {
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  margin: 6px 2px 0;
}
/* 后记：下划线式 */
.rx-sign {
  display: flex;
  gap: 28px;
  margin: 10px 14px 6px;
  font-size: 12px;
  color: #222;
  .fill {
    display: inline-block;
    min-width: 90px;
    border-bottom: 1px solid #333;
    text-align: center;
    padding: 0 4px;
  }
  .sign-line { color: #666; }
}
.rx-foot {
  font-size: 10px;
  color: #666;
  margin: 4px 14px 0;
}
.rx-print-time {
  position: absolute;
  right: 18px;
  bottom: 4px;
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
    border: 1px solid #000;
    margin: 0;
    page-break-after: always;
  }
  .rx-sheet:last-child { page-break-after: auto; }
}
</style>
