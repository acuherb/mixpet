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
        <!-- 品牌头：医院名 + 品牌渐变（后台医院资料可改）；联次与编号右侧角标 -->
        <div class="rx-head" :style="{background: `linear-gradient(135deg, ${brand} 0%, #4ba3e0 100%)`}">
          <div>
            <div class="rx-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
            <div class="rx-doc-tit">兽 医 处 方 笺</div>
          </div>
          <div class="rx-meta">
            <span class="rx-copy-tag">{{ pg.label }}</span>
            <div class="rx-no">编号 {{ rx.printNo || ('No.' + (rx.rxNo || rx.recordId)) }}</div>
          </div>
        </div>

        <!-- 前记：字段网格（下划线填写式改为现代网格，字段一个不少——734号要求） -->
        <div class="rx-sec">
          <div class="rx-sec-tit">前 记</div>
          <div class="rx-grid">
            <div class="rx-field"><span class="rx-k">动物主人</span><span class="rx-v">{{ rx.clientName }}</span></div>
            <div class="rx-field"><span class="rx-k">病历号</span><span class="rx-v">No.{{ rx.recordId }}</span></div>
            <div class="rx-field"><span class="rx-k">开具日期</span><span class="rx-v">{{ fmt(rx.prescriptionTime || rx.createTime) }}</span></div>
            <div class="rx-field"><span class="rx-k">动物种类</span><span class="rx-v">{{ rx.species || '—' }}</span></div>
            <div class="rx-field"><span class="rx-k">动物性别</span><span class="rx-v">{{ rx.petSex == null ? '—' : (rx.petSex ? '公' : '母') }}</span></div>
            <div class="rx-field"><span class="rx-k">动物毛色</span><span class="rx-v">{{ rx.hairColor || '—' }}</span></div>
            <div class="rx-field"><span class="rx-k">体重</span><span class="rx-v">{{ rx.petWeight != null ? rx.petWeight + ' kg' : '—' }}</span></div>
            <div class="rx-field"><span class="rx-k">年（日）龄</span><span class="rx-v">{{ rx.petBirthday ? getAge(rx.petBirthday) : '—' }}</span></div>
            <div class="rx-field"><span class="rx-k">诊费合计</span><span class="rx-v">￥{{ (rx.totalAmount ?? 0).toFixed(2) }}</span></div>
          </div>
        </div>

        <!-- 正文：诊断卡片 | Rp 药品表卡片 -->
        <div class="rx-body">
          <div class="rx-diag-col">
            <div class="rx-sec-tit">诊 断</div>
            <div class="diag-text">{{ rx.diagnosis || '—' }}</div>
          </div>
          <div class="rx-rp-col">
            <div class="rx-sec-tit">Rp: 药 品 处 方</div>
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
                <!-- Rp 后空白处打印前自动划斜线（禁止假数据填充——734号合规保留） -->
                <tr class="rx-blank"><td colspan="5"/></tr>
                <tr class="rx-blank"><td colspan="5"/></tr>
                <tr class="rx-blank-end"><td colspan="5">以 下 为 空 白</td></tr>
                <tr class="rx-amount-row"><td colspan="5">金额（小写）：￥{{ (rx.totalAmount ?? 0).toFixed(2) }}</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 后记：签名区 -->
        <div class="rx-sec">
          <div class="rx-sec-tit">后 记</div>
          <div class="rx-sign">
            <span class="rx-sign-item"><span class="rx-k">执业兽医师</span><span class="rx-v">{{ rx.doctorName }}</span></span>
            <span class="rx-sign-item"><span class="rx-k">注册号</span><span class="rx-v">{{ rx.doctorCert || '　　　　' }}</span></span>
            <span class="rx-sign-item"><span class="rx-k">发药人</span><span class="rx-v">（签名/盖章）</span></span>
          </div>
        </div>

        <div class="rx-foot">注：“{{ hosp.name || 'xxxxxxx' }} 处方笺”中，“{{ hosp.name || 'xxxxxxx' }}”为从事动物诊疗活动的单位名称。</div>
        <!-- 打印时间仅作留痕小字 -->
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
import {useBrand} from "@/utils/UseBrand";
import {getAge} from "@/utils/TimeUtil";
import {ElMessage} from "element-plus";
import html2pdf from 'html2pdf.js';

const route = useRoute()
const router = useRouter()
const rx = ref<RxSheet | null>(null)
const loading = ref(true)
// 品牌信息（医院名+品牌色）：后台「医院资料」改即时生效，与系统/邮件同源
const {hosp, brand, load: loadBrand} = useBrand()

// 联次定义：文字标注，不用颜色区分（734号：一式三联，机构/药房/动物主人）
const allCopies = [
  {no: 1, label: '第一联 动物诊疗机构留存'},
  {no: 2, label: '第二联 兽药房留存'},
  {no: 3, label: '第三联 动物主人留存'}
]
const pages = ref(allCopies.filter(c => c.no === 3))

onMounted(() => {
  const recordId = Number(route.query.recordId)
  loadBrand()
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

<style lang="scss" scoped>
/* 法定单据现代设计语言：品牌头/卡片分区/字段网格/现代药品表（rx-modern.scss 共享） */
@import "@/styles/rx-modern.scss";

/* 处方笺私有：小规格横版 210mm × 148mm（S1：先只做小规格） */
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
  padding-bottom: 8px;
}
</style>
