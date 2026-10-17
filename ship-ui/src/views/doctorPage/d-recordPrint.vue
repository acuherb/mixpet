<template>
  <div class="rp-page">
    <!-- 工具栏（不打印） -->
    <div class="no-print rp-toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-radio-group v-model="style">
        <el-radio-button value="std">国标病历</el-radio-button>
        <el-radio-button value="mono">黑白简化</el-radio-button>
        <el-radio-button value="color">彩色详情版</el-radio-button>
      </el-radio-group>
      <el-button :disabled="!full" type="primary" @click="doPrint">打印</el-button>
      <el-button :disabled="!full" :loading="pdfing" type="success" @click="exportPdf">导出 PDF 文件</el-button>
      <span class="rp-hint">「导出 PDF 文件」直接下载电子病历；打印目标选“另存为 PDF”亦可</span>
    </div>

    <!-- ═══════════ 国标病历（黑白规范，适合纸质归档；mono=黑白简化紧凑版） ═══════════ -->
    <div v-if="full && (style === 'std' || style === 'mono')" :class="['rp-sheet', {'rp-mono': style === 'mono'}]">
      <div v-if="hosp.name" class="rp-hosp-name">{{ hosp.name }}</div>
      <div class="rp-title">宠物病历记录</div>
      <div class="rp-no">编号：No.{{ full.record.recordId }}<span style="float:right">{{ fmt(full.record.createTime) }}</span></div>

      <table class="rp-table">
        <tbody>
          <tr>
            <td class="k">畜主</td><td class="v">{{ full.record.clientName }}</td>
            <td class="k">宠物名</td><td class="v">{{ full.record.petName }}</td>
            <td class="k">品种</td><td class="v">{{ petVariety || '—' }}</td>
          </tr>
          <tr>
            <td class="k">性别</td><td class="v">{{ petSex == null ? '—' : (petSex ? '公' : '母') }}</td>
            <td class="k">年龄</td><td class="v">{{ petBirthday ? getAge(petBirthday) : '—' }}</td>
            <td class="k">体重</td><td class="v">{{ full.record.weight != null ? full.record.weight + 'kg' : '—' }}</td>
          </tr>
          <tr>
            <td class="k">芯片号</td><td class="v" colspan="3">{{ petChip || '—' }}</td>
            <td class="k">科室</td><td class="v">{{ full.record.departmentName || '—' }}</td>
          </tr>
          <tr>
            <td class="k">接诊兽医</td><td class="v" colspan="3">{{ full.record.doctorName }}</td>
            <td class="k">复诊日期</td><td class="v">{{ full.record.revisitDate || '—' }}</td>
          </tr>
        </tbody>
      </table>

      <div v-for="sec in stdSections" :key="sec.label" class="rp-sec">
        <div class="rp-sec-tit">{{ sec.label }}</div>
        <div class="rp-sec-body">{{ sec.value || '—' }}</div>
      </div>

      <!-- 辅助检查 -->
      <div class="rp-sec">
        <div class="rp-sec-tit">辅助检查</div>
        <div class="rp-sec-body">
          <template v-if="full.labOrders.length || full.imageOrders.length">
            <div v-for="o in full.labOrders" :key="'l' + o.order.orderId">
              化验单 #{{ o.order.orderId }}（{{ o.order.status ? '已出结果' : '待检验' }}）：
              <span v-for="(it, i) in o.items" :key="i">{{ it.itemName }}<span v-if="it.resultValue"> {{ it.resultValue }}{{ it.itemUnit || '' }}<b v-if="it.isAbnormal" style="color:#c00">(异常)</b></span><span v-if="i < o.items.length - 1">；</span></span>
            </div>
            <div v-for="o in full.imageOrders" :key="'i' + o.orderId">
              影像 #{{ o.orderId }} {{ o.itemName }}（{{ o.status ? '已出报告' : '待检查' }}）<span v-if="o.reportText">：{{ o.reportText }}</span>
            </div>
          </template>
          <template v-else>无</template>
        </div>
      </div>

      <!-- 处方用药 -->
      <div class="rp-sec">
        <div class="rp-sec-tit">处方用药</div>
        <table v-if="full.prescriptions.length" class="rp-table">
          <thead>
            <tr><th style="width:30%">药品</th><th style="width:18%">规格</th><th style="width:12%">数量</th><th style="width:40%">用法用量</th></tr>
          </thead>
          <tbody>
            <tr v-for="(p, i) in full.prescriptions" :key="i">
              <td>{{ p.drugName }}</td><td>{{ p.drugSpec || '—' }}</td><td>{{ p.quantity }}</td><td>{{ p.usageInfo || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="rp-sec-body">无</div>
      </div>

      <table class="rp-table rp-sign">
        <tbody>
          <tr>
            <td class="k">执业兽医</td><td class="v">{{ full.record.doctorName }}</td>
            <td class="k">签字</td><td class="v">____________</td>
            <td class="k">日期</td><td class="v">{{ fmt(full.record.createTime).slice(0, 10) }}</td>
          </tr>
        </tbody>
      </table>
      <div class="rp-foot">本病历依据《兽医处方格式及应用规范》相关要求书写，仅限本院存档使用。</div>
    </div>

    <!-- ═══════════ 彩色详情版（电子病例 / 发宠主） ═══════════ -->
    <div v-else-if="full && style === 'color'" class="rp-sheet rp-color">
      <div class="rc-head">
        <div class="rc-head-l">
          <div class="rc-hosp">{{ hosp.name || 'mixpet 宠物医院' }}</div>
          <div class="rc-sub">就诊病历 · 详情版</div>
        </div>
        <div class="rc-head-r">
          <div class="rc-no">No.{{ full.record.recordId }}</div>
          <div class="rc-date">{{ fmt(full.record.createTime) }}</div>
        </div>
      </div>

      <div class="rc-pet">
        <div class="rc-pet-ava">{{ full.record.petName?.charAt(0) || '宠' }}</div>
        <div class="rc-pet-info">
          <div class="rc-pet-name">{{ full.record.petName }} <span class="rc-pet-meta">{{ petSex ? '公' : '母' }} · {{ petVariety || '—' }} · {{ petBirthday ? getAge(petBirthday) : '—' }}</span></div>
          <div class="rc-pet-meta">畜主：{{ full.record.clientName }} · 接诊兽医：{{ full.record.doctorName }} · 体重 {{ full.record.weight != null ? full.record.weight + 'kg' : '—' }}</div>
        </div>
        <el-tag :type="full.record.payStatus ? 'success' : 'warning'" effect="dark" round>
          {{ full.record.payStatus ? '已缴费' : '待缴费' }}
        </el-tag>
      </div>

      <div class="rc-secs">
        <div v-for="sec in colorSections" :key="sec.label" class="rc-card">
          <div class="rc-card-tit">{{ sec.label }}</div>
          <div class="rc-card-body">{{ sec.value || '—' }}</div>
        </div>
      </div>

      <!-- 处方 -->
      <div v-if="full.prescriptions.length" class="rc-card">
        <div class="rc-card-tit">处方用药</div>
        <table class="rc-table">
          <thead><tr><th>药品</th><th>规格</th><th>数量</th><th>用法用量</th></tr></thead>
          <tbody>
            <tr v-for="(p, i) in full.prescriptions" :key="i">
              <td>{{ p.drugName }}</td><td>{{ p.drugSpec || '—' }}</td><td>{{ p.quantity }}</td><td>{{ p.usageInfo || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 检验 -->
      <div v-if="full.labOrders.length" class="rc-card">
        <div class="rc-card-tit">检验结果</div>
        <div v-for="o in full.labOrders" :key="o.order.orderId" class="rc-lab">
          <div class="rc-lab-head">化验单 #{{ o.order.orderId }}<el-tag size="small" :type="o.order.status ? 'success' : 'warning'">{{ o.order.status ? '已出结果' : '待检验' }}</el-tag></div>
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

      <!-- 影像 -->
      <div v-if="full.imageOrders.length" class="rc-card">
        <div class="rc-card-tit">影像检查</div>
        <div v-for="o in full.imageOrders" :key="o.orderId" class="rc-lab">
          <div class="rc-lab-head">{{ o.itemName }}（{{ o.examPart || '—' }}）<el-tag size="small" :type="o.status ? 'success' : 'warning'">{{ o.status ? '已出报告' : '待检查' }}</el-tag></div>
          <div class="rc-report">{{ o.reportText || '尚未出报告' }}</div>
        </div>
      </div>

      <!-- 其他诊疗记录 -->
      <div v-if="full.surgeries.length || full.hospitals.length || full.immunes.length || full.followUps.length" class="rc-card">
        <div class="rc-card-tit">其他诊疗记录</div>
        <div v-if="full.surgeries.length" class="rc-other">手术：<span v-for="s in full.surgeries" :key="s.surgeryId">{{ s.surgeryName }}（{{ surgeryStatusText(s.status) }}）</span></div>
        <div v-if="full.hospitals.length" class="rc-other">住院：<span v-for="h in full.hospitals" :key="h.hospitalId">{{ h.roomNo || '—' }}（{{ h.status ? '已出院' : '住院中' }}）</span></div>
        <div v-if="full.immunes.length" class="rc-other">免疫：<span v-for="m in full.immunes" :key="m.immuneId">{{ m.drugName }}（{{ fmt(m.vaccinateTime).slice(0, 10) }}）</span></div>
        <div v-if="full.followUps.length" class="rc-other">随访：<span v-for="f in full.followUps" :key="f.followId">{{ f.followType }} · {{ f.status ? '已完成' : '待随访' }}</span></div>
      </div>

      <!-- 费用 -->
      <div class="rc-card">
        <div class="rc-card-tit">费用明细</div>
        <div class="rc-fees">
          <div class="rc-fee"><span>挂号费</span><b>￥{{ (full.record.feeRegister ?? 0).toFixed(2) }}</b></div>
          <div class="rc-fee"><span>诊疗费</span><b>￥{{ (full.record.feeTreat ?? 0).toFixed(2) }}</b></div>
          <div class="rc-fee"><span>药品费</span><b>￥{{ (full.record.feeDrug ?? 0).toFixed(2) }}</b></div>
          <div class="rc-fee rc-fee-total"><span>合计</span><b>￥{{ (full.record.totalMoney ?? 0).toFixed(2) }}</b></div>
        </div>
      </div>

      <div class="rc-advice">
        <div class="rc-advice-tit">医嘱</div>
        <div class="rc-advice-body">{{ full.record.advice || '遵医嘱' }}</div>
      </div>

      <div class="rc-foot">本病历由 mixpet 智慧宠物医疗管理系统生成 · 就诊问题请致电本院或在线咨询</div>
    </div>

    <div v-else-if="!loading" class="no-print rp-empty">未找到该病历</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {reqRecordFull} from "@/request/RecordApi";
import {RecordFull} from "@/model/VO/RecordFull";
import {getAge} from "@/utils/TimeUtil";
import {surgeryStatusText} from "@/model/entiy/Surgery";
import {reqPetOne} from "@/request/PetApi";
import {Pet} from "@/model/entiy/Pet";
import {ElMessage} from "element-plus";
import html2pdf from 'html2pdf.js';
import {reqHospitalSetting} from "@/request/SettingApi";
import {emptyHospital, HospitalSetting} from "@/model/entiy/HospitalSetting";

// 打印页独立渲染（window.open/iframe）：自行拉医院信息，不用全局缓存，保证每次打印最新值
const hosp = ref<HospitalSetting>(emptyHospital())
reqHospitalSetting().then(s => { if (s) hosp.value = s })

const route = useRoute()
const router = useRouter()
const full = ref<RecordFull | null>(null)
const pet = ref<Pet | null>(null)
const loading = ref(true)
const style = ref<'std' | 'mono' | 'color'>((route.query.style as string) ?? 'std')

onMounted(() => {
  const recordId = Number(route.query.recordId)
  if (!recordId) { loading.value = false; return }
  reqRecordFull(recordId).then(res => {
    full.value = res
    loading.value = false
    if (res?.record.petId) reqPetOne(res.record.petId).then(p => { pet.value = p })
    // 带 autopdf=1（从打印预览弹窗跳入）：渲染后自动导出 PDF
    if (route.query.autopdf === '1') setTimeout(() => exportPdf(), 600)
  })
})

const petVariety = computed(() => pet.value?.petVariety ?? '')
const petSex = computed(() => pet.value?.petSex ?? null)
const petBirthday = computed(() => pet.value?.petBirthday ?? null)
const petChip = computed(() => pet.value?.petChip ?? '')

const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)

// 国标病历分区（按规范病历结构）
const stdSections = computed(() => {
  const r = full.value?.record
  if (!r) return []
  return [
    {label: '主诉', value: r.chiefComplaint},
    {label: '现病史', value: r.presentIllness},
    {label: '既往病史', value: r.pastHistory},
    {label: '体格检查', value: r.physicalExam},
    {label: '诊断', value: r.diagnosis},
    {label: '治疗方案', value: r.treatment},
    {label: '医嘱', value: r.advice}
  ]
})

// 彩色详情版分区（不含辅助检查/处方——它们有专门卡片）
const colorSections = computed(() => {
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

// 导出真实 PDF 文件（html2pdf：当前模板纸张 → A4 PDF，文件名带病历号与模板名）
const pdfing = ref(false)
const exportPdf = (): void => {
  const el = document.querySelector('.rp-sheet') as HTMLElement | null
  if (!el || !full.value) return
  pdfing.value = true
  const tplName = style.value === 'std' ? '国标病历' : style.value === 'mono' ? '黑白简化' : '彩色详情版'
  html2pdf().set({
    margin: 8,
    filename: `病历-No.${full.value.record.recordId}-${tplName}.pdf`,
    image: {type: 'jpeg', quality: 0.95},
    html2canvas: {scale: 2},
    jsPDF: {unit: 'mm', format: 'a4', orientation: 'portrait'}
  }).from(el).save().catch(() => ElMessage.error('PDF 导出失败')).finally(() => { pdfing.value = false })
}
</script>

<style lang="scss" scoped>
.rp-page {
  padding: 20px;
  background: #f5f6fa;
  min-height: 100vh;
}
.rp-toolbar {
  max-width: 760px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  .rp-hint { font-size: 12px; color: #909399; }
}
.rp-sheet {
  max-width: 760px;
  margin: 0 auto 24px;
  background: #fff;
  border: 2px solid #333;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.rp-hosp-name {
  text-align: center;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 2px;
}
.rp-title {
  text-align: center;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 8px;
  margin-bottom: 4px;
}
.rp-no {
  text-align: left;
  font-size: 13px;
  color: #555;
  margin-bottom: 12px;
  overflow: hidden;
}
.rp-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 12px;
  td, th { border: 1px solid #333; padding: 7px 8px; font-size: 13px; }
  .k { background: #f0f2f5; width: 76px; text-align: center; white-space: nowrap; }
  th { background: #f0f2f5; text-align: center; }
  .v { min-width: 60px; }
}
.rp-sec {
  margin-bottom: 10px;
  .rp-sec-tit {
    font-weight: 700;
    font-size: 14px;
    margin-bottom: 3px;
    &::before { content: "■ "; color: #555; }
  }
  .rp-sec-body {
    font-size: 13px;
    line-height: 1.7;
    padding-left: 14px;
    white-space: pre-wrap;
    word-break: break-word;
  }
}
.rp-sign td { height: 42px; }
.rp-foot {
  font-size: 11px;
  color: #888;
  margin-top: 10px;
}
.rp-empty {
  text-align: center;
  color: #999;
  margin-top: 60px;
}

/* ═══ 黑白简化：灰度兜底 + 紧凑排版，适合针式打印机/普通纸归档 ═══ */
.rp-sheet.rp-mono {
  filter: grayscale(1);
  padding: 16px 18px;

  .rp-title { font-size: 20px; letter-spacing: 5px; }
  .rp-table {
    td, th { padding: 4px 6px; font-size: 12px; }
  }
  .rp-sec {
    margin-bottom: 6px;
    .rp-sec-body { font-size: 12px; line-height: 1.5; }
    .rp-sec-tit { font-size: 13px; }
  }
  .rp-sign td { height: 34px; }
}

/* ═══ 彩色详情版 ═══ */
.rp-color {
  border: none;
  border-radius: 12px;
  padding: 0;
  overflow: hidden;
}
.rc-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 26px;
  background: linear-gradient(120deg, var(--brand), #2BB6A8);
  color: #fff;
  .rc-hosp { font-size: 20px; font-weight: 800; letter-spacing: 2px; }
  .rc-sub { font-size: 12px; opacity: .85; margin-top: 2px; }
  .rc-no { font-size: 15px; font-weight: 700; text-align: right; }
  .rc-date { font-size: 12px; opacity: .85; text-align: right; margin-top: 2px; }
}
.rc-pet {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 26px;
  background: #f0f7fc;
  border-bottom: 1px solid #e4e7ed;
  .rc-pet-ava {
    width: 48px; height: 48px; border-radius: 50%;
    background: linear-gradient(135deg, var(--brand), #2BB6A8);
    color: #fff; font-size: 20px; font-weight: 700;
    display: flex; align-items: center; justify-content: center;
  }
  .rc-pet-info { flex: 1; min-width: 0; }
  .rc-pet-name { font-size: 16px; font-weight: 700; color: #1F2D3D; }
  .rc-pet-meta { font-size: 12px; color: #606266; font-weight: 400; margin-left: 6px; }
}
.rc-secs { padding: 6px 26px 0; }
.rc-card {
  margin: 14px 26px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  .rc-card-tit {
    padding: 8px 14px;
    background: #f5f7fa;
    font-size: 13px;
    font-weight: 700;
    color: #1F2D3D;
    border-left: 3px solid var(--brand);
  }
  .rc-card-body {
    padding: 10px 14px;
    font-size: 13px;
    line-height: 1.7;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-word;
  }
}
.rc-table {
  width: 100%;
  border-collapse: collapse;
  th, td { border-bottom: 1px solid #ebeef5; padding: 6px 10px; font-size: 12px; text-align: left; }
  th { background: #fafbfc; color: #606266; }
  .abn { color: #F56C6C; font-weight: 700; }
}
.rc-lab { padding: 8px 12px; }
.rc-lab-head {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; color: #606266; margin-bottom: 6px;
}
.rc-report {
  padding: 8px 10px; background: #f8f9fb; border-radius: 6px;
  font-size: 12px; color: #606266; white-space: pre-wrap; line-height: 1.6;
}
.rc-other { font-size: 12px; color: #606266; padding: 4px 14px; span { margin-right: 12px; } }
.rc-fees { display: flex; flex-wrap: wrap; gap: 8px; padding: 12px 14px; }
.rc-fee {
  flex: 1; min-width: 100px;
  background: #f8f9fb; border-radius: 8px; padding: 8px 12px;
  span { display: block; font-size: 11px; color: #909399; }
  b { font-size: 15px; color: #1F2D3D; }
}
.rc-fee-total { background: #fdf2f2; b { color: #F56C6C; } }
.rc-advice {
  margin: 14px 26px;
  padding: 12px 16px;
  background: #fdf6ec;
  border: 1px dashed #e6a23c;
  border-radius: 10px;
  .rc-advice-tit { font-size: 13px; font-weight: 700; color: #b88230; margin-bottom: 4px; }
  .rc-advice-body { font-size: 13px; color: #606266; line-height: 1.7; white-space: pre-wrap; }
}
.rc-foot {
  padding: 14px 26px 20px;
  text-align: center;
  font-size: 11px;
  color: #a8abb2;
}

@media print {
  .rp-page { padding: 0; background: #fff; }
  .no-print { display: none !important; }
  .rp-sheet { box-shadow: none; margin: 0; max-width: 100%; }
  .rp-color { border-radius: 0; }
  .rc-head { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  .rc-pet, .rc-fee-total, .rc-advice { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
}
</style>
