<template>
  <div class="rp-page" :style="{'--brand': brand}">
    <!-- 工具栏（不打印、不进 PDF） -->
    <div class="no-print rp-toolbar">
      <el-button @click="goBack">返回</el-button>
      <span class="rp-badge">就诊资料 · 现代报告</span>
      <el-button :disabled="!full" type="primary" @click="doPrint">打印 / 另存为 PDF</el-button>
      <el-button :disabled="!full" :loading="pdfing" @click="exportPdf">下载 PDF 文件</el-button>
      <span class="rp-hint">推荐「打印」并选“另存为 PDF”：文字可选、矢量清晰；「下载 PDF」为截图兼容版。本资料不含院内备注与费用信息。</span>
      <span v-if="error" class="rp-err">{{ error }}</span>
    </div>

    <!-- 彩色详情版：版式与医生端 d-recordPrint 一致，字段按顾客端白名单裁剪。
         品牌色挂在被克隆的元素自身，避免 html2pdf 只 clone .rp-sheet 时丢失 var(--brand) 导致渐变失效 -->
    <div v-if="full" class="rp-sheet rp-color" :style="{'--brand': brand}">
      <div class="rc-head">
        <div class="rc-head-l">
          <div class="rc-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
          <div class="rc-sub">就诊资料 · 病历与处方</div>
        </div>
        <div class="rc-head-r">
          <div class="rc-no">No.{{ full.record.recordId }}</div>
          <div class="rc-date">{{ fmt(full.record.createTime) }}</div>
        </div>
      </div>

      <div class="rc-pet">
        <div class="rc-pet-ava">{{ full.record.petName?.charAt(0) || '宠' }}</div>
        <div class="rc-pet-info">
          <div class="rc-pet-name">{{ full.record.petName || '—' }}</div>
          <div class="rc-pet-meta">
            畜主：{{ full.record.clientName || '—' }} · 接诊兽医：{{ full.record.doctorName || '—' }} · {{ full.record.departmentName || '—' }}
          </div>
        </div>
      </div>

      <!-- 体征 -->
      <div class="rc-vitals">
        <div class="rc-vital"><span>体重</span><b>{{ full.record.weight != null ? full.record.weight + ' kg' : '—' }}</b></div>
        <div class="rc-vital"><span>体温</span><b>{{ full.record.temperature != null ? full.record.temperature + ' ℃' : '—' }}</b></div>
        <div class="rc-vital"><span>心率</span><b>{{ full.record.heartRate != null ? full.record.heartRate + ' 次/分' : '—' }}</b></div>
        <div class="rc-vital"><span>呼吸</span><b>{{ full.record.respiratoryRate != null ? full.record.respiratoryRate + ' 次/分' : '—' }}</b></div>
        <div class="rc-vital"><span>血压</span><b>{{ full.record.bloodPressure || '—' }}</b></div>
      </div>

      <div class="rc-secs">
        <div v-for="sec in sections" :key="sec.label" class="rc-card">
          <div class="rc-card-tit">{{ sec.label }}</div>
          <div class="rc-card-body">{{ sec.value || '—' }}</div>
        </div>
      </div>

      <!-- 处方：「查看处方笺」入口滚动到这里 -->
      <div class="rc-card" :class="{'cr-focus': focusRx}">
        <div class="rc-card-tit">处方用药（共 {{ full.prescriptions?.length || 0 }} 项）</div>
        <table class="rc-table">
          <thead><tr><th>药品</th><th>规格</th><th>数量</th><th>用法用量</th></tr></thead>
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
      </div>

      <!-- 检验结果 -->
      <div v-if="full.labOrders?.length" class="rc-card">
        <div class="rc-card-tit">检验结果</div>
        <div v-for="o in full.labOrders" :key="o.order.orderId" class="rc-lab">
          <div class="rc-lab-head">化验单 #{{ o.order.orderId }}</div>
          <table class="rc-table">
            <thead><tr><th>项目</th><th>结果</th><th>参考范围</th><th>单位</th></tr></thead>
            <tbody>
              <tr v-for="(it, i) in o.items" :key="i">
                <td>{{ it.itemName }}</td>
                <td :class="{abn: it.isAbnormal}">{{ it.resultValue || '—' }}</td>
                <td>{{ it.refRange || '—' }}</td>
                <td>{{ it.itemUnit || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 影像检查 -->
      <div v-if="full.imageOrders?.length" class="rc-card">
        <div class="rc-card-tit">影像检查</div>
        <div v-for="o in full.imageOrders" :key="o.orderId" class="rc-lab">
          <div class="rc-lab-head">{{ o.itemName || '—' }}（{{ o.examPart || '—' }}）</div>
          <div class="rc-report">{{ o.reportText || (o.status ? '已出报告' : '待出报告') }}</div>
        </div>
      </div>

      <div class="rc-advice">
        <div class="rc-advice-tit">医嘱</div>
        <div class="rc-advice-body">{{ full.record.advice || '遵医嘱' }}</div>
      </div>

      <div class="rc-signbox">
        <span>接诊医师：{{ full.record.doctorName || '—' }}</span>
        <span>复诊日期：{{ fmt(full.record.revisitDate) }}</span>
        <span>打印时间：{{ nowText() }}</span>
      </div>
      <div class="rc-foot">本资料为本院诊疗记录摘要，仅供宠物主人留存与复诊参考，不含院内备注及费用信息；如有疑问请联系主诊医师。</div>
    </div>

    <div v-else-if="loaded" class="no-print rp-empty">{{ error || '未找到该就诊资料，或无权查看' }}</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import html2pdf from 'html2pdf.js'
import {reqRecordFull} from '@/request/RecordApi'
import {RecordFull} from '@/model/VO/RecordFull'
import {useBrand} from '@/utils/UseBrand'

const route = useRoute()
const router = useRouter()
// 独立打印页自行拉医院抬头与品牌色（后台改「医院资料」即时生效）
const {hosp, brand, load: loadBrand} = useBrand()
const full = ref<RecordFull | null>(null)
const loaded = ref(false)
const error = ref('')
const pdfing = ref(false)
const focusRx = computed(() => route.query.focus === 'rx')

const fmt = (t?: string | null): string => (t || '').replace('T', ' ').slice(0, 16) || '—'
const nowText = (): string => new Date().toLocaleString('zh-CN')

// 顾客端字段白名单：只给病历正文，不含费用、电子签名/版本/审核状态、院内备注
const sections = computed(() => {
  const r = full.value?.record
  if (!r) return []
  return [
    {label: '主诉', value: r.chiefComplaint},
    {label: '现病史', value: r.presentIllness},
    {label: '既往病史', value: r.pastHistory},
    {label: '体格检查', value: r.physicalExam},
    {label: '诊断', value: r.diagnosis},
    {label: '治疗方案', value: r.treatment}
  ]
})

const doPrint = (): void => window.print()

// 与医生端 d-recordPrint 同款截图版 PDF：彩色版式整体转成 A4 PDF
const exportPdf = (): void => {
  const el = document.querySelector('.rp-sheet') as HTMLElement | null
  if (!el || !full.value) return
  pdfing.value = true
  html2pdf().set({
    margin: 8,
    filename: `就诊资料-No.${full.value.record.recordId}.pdf`,
    image: {type: 'jpeg', quality: 0.95},
    html2canvas: {scale: 2},
    jsPDF: {unit: 'mm', format: 'a4', orientation: 'portrait'}
  }).from(el).save()
      .catch(() => ElMessage.error('PDF 导出失败，请改用「打印 / 另存为 PDF」'))
      .finally(() => { pdfing.value = false })
}

onMounted(() => {
  loadBrand()
  const recordId = Number(route.query.recordId)
  if (!recordId) { loaded.value = true; error.value = '缺少就诊号'; return }
  // 后端 /record/detail 已做「顾客只能看自己宠物」的归属校验，越权返回 null
  reqRecordFull(recordId).then(res => {
    if (!res || !res.record) { error.value = '未找到该就诊资料，或无权查看'; return }
    full.value = res
    if (focusRx.value) {
      setTimeout(() => document.querySelector('.cr-focus')?.scrollIntoView({block: 'center'}), 100)
    }
    // autopdf=1：顾客从「保存病历（PDF）」跳进来，渲染完直接下载；
    // auto=1 保留旧的自动打印行为（打印对话框里另存为 PDF）
    if (route.query.autopdf === '1') setTimeout(() => exportPdf(), 600)
    else if (route.query.auto === '1') setTimeout(() => window.print(), 600)
  }).catch(() => { error.value = '资料加载失败，请稍后重试' }).finally(() => { loaded.value = true })
})

const goBack = (): void => {
  if (window.history.length > 1) router.back()
  else window.close()
}
</script>

<style lang="scss" scoped>
.rp-page { padding: 20px; background: #f5f6fa; min-height: 100vh; }
.rp-toolbar {
  max-width: 760px; margin: 0 auto 16px; display: flex; align-items: center;
  gap: 10px; flex-wrap: wrap;
  .rp-hint { font-size: 12px; color: #909399; flex-basis: 100%; }
  .rp-err { font-size: 12px; color: #d03050; font-weight: 600; }
  .rp-badge {
    font-size: 12px; color: var(--brand); background: #eef6fc;
    border: 1px solid #cfe4f5; border-radius: 14px; padding: 4px 12px;
  }
}
.rp-sheet {
  max-width: 760px; margin: 0 auto 24px; background: #fff;
  border: 1px solid #e8edf5; border-radius: 12px;
  box-shadow: 0 8px 28px rgba(31, 45, 61, .1);
}
.rp-empty { text-align: center; color: #999; margin-top: 60px; }

/* ═══ 彩色详情版（与医生端 d-recordPrint 同款）═══ */
.rp-color { border: none; border-radius: 12px; padding: 0; overflow: hidden; }
.rc-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 26px; background: linear-gradient(120deg, var(--brand), #2BB6A8); color: #fff;
  .rc-hosp { font-size: 20px; font-weight: 800; letter-spacing: 2px; }
  .rc-sub { font-size: 12px; opacity: .85; margin-top: 2px; }
  .rc-no { font-size: 15px; font-weight: 700; text-align: right; }
  .rc-date { font-size: 12px; opacity: .85; text-align: right; margin-top: 2px; }
}
.rc-pet {
  display: flex; align-items: center; gap: 14px; padding: 16px 26px;
  background: #f0f7fc; border-bottom: 1px solid #e4e7ed;
  .rc-pet-ava {
    width: 48px; height: 48px; border-radius: 50%;
    background: linear-gradient(135deg, var(--brand), #2BB6A8);
    color: #fff; font-size: 20px; font-weight: 700;
    display: flex; align-items: center; justify-content: center;
  }
  .rc-pet-info { flex: 1; min-width: 0; }
  .rc-pet-name { font-size: 16px; font-weight: 700; color: #1F2D3D; }
  .rc-pet-meta { font-size: 12px; color: #606266; line-height: 1.7; }
}
.rc-vitals { display: flex; flex-wrap: wrap; gap: 8px; padding: 12px 26px 0; }
.rc-vital {
  flex: 1; min-width: 100px; background: #f8f9fb; border-radius: 8px; padding: 8px 12px;
  span { display: block; font-size: 11px; color: #909399; }
  b { font-size: 13px; color: #1F2D3D; }
}
.rc-secs { padding: 6px 26px 0; }
.rc-card {
  margin: 14px 26px; border: 1px solid #ebeef5; border-radius: 10px; overflow: hidden;
  .rc-card-tit {
    padding: 8px 14px; background: #f5f7fa; font-size: 13px; font-weight: 700;
    color: #1F2D3D; border-left: 3px solid var(--brand);
  }
  .rc-card-body {
    padding: 10px 14px; font-size: 13px; line-height: 1.7;
    color: #303133; white-space: pre-wrap; word-break: break-word;
  }
}
.cr-focus > .rc-card-tit { background: #fff7e6; }
.rc-table {
  width: 100%; border-collapse: collapse;
  th, td { border-bottom: 1px solid #ebeef5; padding: 6px 10px; font-size: 12px; text-align: left; }
  th { background: #fafbfc; color: #606266; }
  .abn { color: #F56C6C; font-weight: 700; }
}
.cr-empty { text-align: center; color: #8a93a6; padding: 8px; }
.rc-lab { padding: 8px 12px; }
.rc-lab-head { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #606266; margin-bottom: 6px; }
.rc-report {
  padding: 8px 10px; background: #f8f9fb; border-radius: 6px;
  font-size: 12px; color: #606266; white-space: pre-wrap; line-height: 1.6;
}
.rc-advice {
  margin: 14px 26px; padding: 12px 16px;
  background: #fdf6ec; border: 1px dashed #e6a23c; border-radius: 10px;
  .rc-advice-tit { font-size: 13px; font-weight: 700; color: #b88230; margin-bottom: 4px; }
  .rc-advice-body { font-size: 13px; color: #606266; line-height: 1.7; white-space: pre-wrap; }
}
.rc-signbox {
  display: flex; flex-wrap: wrap; gap: 8px 24px; margin: 14px 0 6px;
  padding: 10px 12px; border-radius: 8px; background: #f4f8fb;
  font-size: 12px; color: #42536b; font-weight: 600;
}
.rc-foot { padding: 14px 26px 20px; text-align: center; font-size: 11px; color: #a8abb2; }

@page { size: A4 portrait; margin: 10mm; }
@media print {
  .rp-page { padding: 0; background: #fff; }
  .no-print { display: none !important; }
  .rp-sheet { box-shadow: none; margin: 0; max-width: 100%; }
  .rp-color { border-radius: 0; }
  .rc-head, .rc-pet, .rc-pet-ava, .rc-card-tit, .rc-advice, .rc-signbox, .cr-focus > .rc-card-tit {
    -webkit-print-color-adjust: exact; print-color-adjust: exact;
  }
}
</style>
