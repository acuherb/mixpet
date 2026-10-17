<template>
  <div v-if="full" class="record-full">
    <!-- 病历概要 -->
    <el-descriptions :column="2" border size="small" title="病历概要">
      <el-descriptions-item label="宠物">{{ full.record.petName }}</el-descriptions-item>
      <el-descriptions-item label="客户">{{ full.record.clientName }}</el-descriptions-item>
      <el-descriptions-item label="接诊医生">{{ full.record.doctorName }}（{{ full.record.departmentName }}）</el-descriptions-item>
      <el-descriptions-item label="就诊时间">{{ getDisplayTime(full.record.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="体重">{{ full.record.weight != null ? full.record.weight + 'kg' : '—' }}</el-descriptions-item>
      <el-descriptions-item label="血压">{{ full.record.bloodPressure || '—' }}</el-descriptions-item>
      <el-descriptions-item label="体温(℃)">{{ full.record.temperature ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="心率(次/分)">{{ full.record.heartRate ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="呼吸(次/分)">{{ full.record.respiratoryRate ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="分诊">{{ {1: '急诊', 2: '普通', 3: '复诊'}[full.record.triageLevel!] || '—' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="主诉">{{ full.record.chiefComplaint || '无' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="现病史">{{ full.record.presentIllness || '无' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="既往病史">{{ full.record.pastHistory || '无' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="体格检查">{{ full.record.physicalExam || '无' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="诊断">{{ full.record.diagnosis || '无' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="治疗方案">{{ full.record.treatment || '无' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="医嘱">{{ full.record.advice || '无' }}</el-descriptions-item>
      <el-descriptions-item v-if="full.record.internalNote" :span="2" label="内部备注">{{ full.record.internalNote }}</el-descriptions-item>
      <el-descriptions-item label="复诊日期">{{ full.record.revisitDate || '—' }}</el-descriptions-item>
      <el-descriptions-item label="复诊链">
        <span v-if="full.record.originRecordId">关联 #{{ full.record.originRecordId }}（{{ full.record.originDiagnosis || '—' }}）</span>
        <span v-else>—</span>
      </el-descriptions-item>
      <el-descriptions-item label="费用合计">￥{{ full.record.totalMoney ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="支付">
        <el-tag v-if="full.record.payStatus" size="small" type="success">已支付</el-tag>
        <el-tag v-else size="small" type="warning">待支付</el-tag>
      </el-descriptions-item>
    </el-descriptions>

    <!-- 处方 -->
    <h4 class="rf-tit">处方用药</h4>
    <el-table v-if="full.prescriptions.length" :data="full.prescriptions" border size="small">
      <el-table-column label="药品" min-width="120" prop="drugName" show-overflow-tooltip/>
      <el-table-column label="规格" min-width="100" prop="drugSpec" show-overflow-tooltip/>
      <el-table-column label="数量" prop="quantity" width="60"/>
      <el-table-column label="单价" prop="drugPrice" width="70"/>
      <el-table-column label="用法" min-width="130">
        <template #default="s">{{ usageText(s.row) }}</template>
      </el-table-column>
    </el-table>
    <p v-else class="rf-empty">未开处方</p>

    <!-- 套餐 -->
    <template v-if="full.packages.length">
      <h4 class="rf-tit">套餐服务</h4>
      <el-table :data="full.packages" border size="small">
        <el-table-column label="套餐" min-width="140" prop="packageName" show-overflow-tooltip/>
        <el-table-column label="价格" prop="price" width="90"/>
      </el-table>
    </template>

    <!-- 化验单 -->
    <template v-if="full.labOrders.length">
      <h4 class="rf-tit">化验检验</h4>
      <div v-for="(o, i) in full.labOrders" :key="i" class="rf-block">
        <div class="rf-block-head">
          <span>化验单 #{{ o.order.orderId }} · ￥{{ o.order.fee }}</span>
          <el-tag size="small" :type="o.order.status ? 'success' : 'warning'">
            {{ o.order.status ? '已出结果' : '待检验' }}
          </el-tag>
        </div>
        <el-table :data="o.items" border size="small">
          <el-table-column label="检验项" min-width="120" prop="itemName" show-overflow-tooltip/>
          <el-table-column label="结果" min-width="90" prop="resultValue"/>
          <el-table-column label="参考范围" min-width="120" prop="refRange" show-overflow-tooltip/>
          <el-table-column label="单位" prop="itemUnit" width="60"/>
          <el-table-column label="异常" width="70">
            <template #default="s">
              <el-tag v-if="s.row.isAbnormal" size="small" type="danger">异常</el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 影像单 -->
    <template v-if="full.imageOrders.length">
      <h4 class="rf-tit">影像检查</h4>
      <div v-for="(o, i) in full.imageOrders" :key="i" class="rf-block">
        <div class="rf-block-head">
          <span>{{ o.itemName }}（{{ o.examPart || '—' }}）· ￥{{ o.fee ?? 0 }}</span>
          <el-tag size="small" :type="o.status ? 'success' : 'warning'">
            {{ o.status ? '已出报告' : '待检查' }}
          </el-tag>
        </div>
        <div class="rf-report">{{ o.reportText || '尚未出报告' }}</div>
      </div>
    </template>

    <!-- 住院 -->
    <template v-if="full.hospitals.length">
      <h4 class="rf-tit">住院</h4>
      <el-table :data="full.hospitals" border size="small">
        <el-table-column label="病房" prop="roomNo" width="100"/>
        <el-table-column label="日均费" prop="dailyFee" width="90"/>
        <el-table-column label="入院" prop="inTime" min-width="150"/>
        <el-table-column label="出院" min-width="150">
          <template #default="s">{{ s.row.outTime || '住院中' }}</template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 手术 -->
    <template v-if="full.surgeries.length">
      <h4 class="rf-tit">手术</h4>
      <el-table :data="full.surgeries" border size="small">
        <el-table-column label="手术" min-width="140" prop="surgeryName" show-overflow-tooltip/>
        <el-table-column label="类型" prop="surgeryType" width="100"/>
        <el-table-column label="麻醉" prop="anesthesiaType" width="100"/>
        <el-table-column label="费用" prop="fee" width="80"/>
        <el-table-column label="状态" width="90">
          <template #default="s">
            <el-tag size="small" :type="surgeryStatusType(s.row.status)">{{ surgeryStatusText(s.row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 免疫接种 -->
    <template v-if="full.immunes.length">
      <h4 class="rf-tit">免疫接种</h4>
      <el-table :data="full.immunes" border size="small">
        <el-table-column label="疫苗" min-width="120" prop="drugName" show-overflow-tooltip/>
        <el-table-column label="针次" prop="times" width="60"/>
        <el-table-column label="接种时间" prop="vaccinateTime" min-width="150"/>
        <el-table-column label="下一针" prop="nextTime" min-width="150"/>
        <el-table-column label="接种医生" prop="doctorName" width="100"/>
      </el-table>
    </template>

    <!-- 随访 -->
    <template v-if="full.followUps.length">
      <h4 class="rf-tit">随访</h4>
      <el-table :data="full.followUps" border size="small">
        <el-table-column label="方式" prop="followType" width="80"/>
        <el-table-column label="内容" min-width="140" prop="content" show-overflow-tooltip/>
        <el-table-column label="计划时间" prop="planTime" min-width="150"/>
        <el-table-column label="结果" min-width="140" prop="result" show-overflow-tooltip/>
        <el-table-column label="状态" width="80">
          <template #default="s">
            <el-tag size="small" :type="s.row.status ? 'success' : 'warning'">{{ s.row.status ? '已随访' : '待随访' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 后续复诊 -->
    <template v-if="full.revisits.length">
      <h4 class="rf-tit">后续复诊</h4>
      <el-table :data="full.revisits" border size="small">
        <el-table-column label="时间" min-width="150">
          <template #default="s">{{ getDisplayTime(s.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="诊断" min-width="160" prop="diagnosis" show-overflow-tooltip/>
        <el-table-column label="医生" prop="doctorName" width="100"/>
      </el-table>
    </template>

    <div class="rf-foot">
      <el-button size="small" type="primary" @click="printView = true">打印预览</el-button>
    </div>

    <!-- 打印预览模态：模板宫格选择 + 实时预览 + 打印/另存PDF -->
    <el-dialog v-model="printView" append-to-body title="打印预览" top="4vh" width="94%" class="print-dlg">
      <div class="pv-body">
        <div class="pv-tpls">
          <div v-for="t in printTpls" :key="t.style" :class="['pv-tpl', {active: printStyle === t.style}]" @click="printStyle = t.style">
            <div class="pv-tpl-ico" :style="{background: t.color}">
              <el-icon :size="22"><component :is="t.icon"/></el-icon>
            </div>
            <div class="pv-tpl-name">{{ t.name }}</div>
            <div class="pv-tpl-desc">{{ t.desc }}</div>
          </div>
        </div>
        <div class="pv-preview">
          <iframe v-if="printSrc" :src="printSrc" class="pv-frame" title="打印预览"/>
        </div>
      </div>
      <template #footer>
        <span class="pv-tip">「导出 PDF 文件」直接下载；打印目标选“另存为 PDF”亦可</span>
        <el-button @click="printView = false">关闭</el-button>
        <el-button @click="doFramePrint">打印</el-button>
        <el-button type="success" @click="exportPdfFile">导出 PDF 文件</el-button>
      </template>
    </el-dialog>
  </div>
  <div v-else v-loading="true" class="rf-loading">加载中...</div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from "vue";
import {Document, Picture, Printer, Tickets} from "@element-plus/icons-vue";
import {useRouter} from "vue-router";
import {reqRecordFull} from "@/request/RecordApi";
import {RecordFull} from "@/model/VO/RecordFull";
import {getDisplayTime} from "@/utils/TimeUtil";
import {surgeryStatusText, surgeryStatusType} from "@/model/entiy/Surgery";

const props = defineProps<{ recordId: number }>()
const router = useRouter()
const full = ref<RecordFull | null>(null)

onMounted(() => {
  if (props.recordId) reqRecordFull(props.recordId).then(res => { full.value = res })
})

// 打印预览：模板宫格（国标病历 / 彩色详情版 / 处方笺），iframe 实时预览
/** 用法展示：有结构化单次剂量时展示「单次X×每日Y次」，否则回退自由文本 */
const usageText = (p: any): string => {
  if (p.singleDose != null) {
    const s = `单次${p.singleDose}${p.singleDoseUnit || ''}×每日${p.dailyTimes || 1}次`
    return p.usageInfo ? `${s}（${p.usageInfo}）` : s
  }
  return p.usageInfo || '—'
}

const printView = ref(false)
const printStyle = ref<'std' | 'mono' | 'color' | 'rx'>('std')
const printTpls = [
  {style: 'std' as const, name: '国标病历', desc: '黑白规范 · 纸质归档', color: '#1F2D3D', icon: Document},
  {style: 'mono' as const, name: '黑白简化', desc: '单色紧凑 · 针打友好', color: '#7f8c98', icon: Printer},
  {style: 'color' as const, name: '彩色详情版', desc: '完整内容 · 发宠主', color: 'var(--brand)', icon: Picture},
  {style: 'rx' as const, name: '处方笺', desc: '国标处方笺', color: '#2BB6A8', icon: Tickets}
]
const printSrc = computed(() => {
  const base = printStyle.value === 'rx'
      ? '/#/doctorPage/d-rx?recordId=' + props.recordId
      : '/#/doctorPage/d-recordPrint?recordId=' + props.recordId + '&style=' + printStyle.value
  return base
})
const doFramePrint = (): void => {
  const f = document.querySelector('.pv-frame') as HTMLIFrameElement | null
  f?.contentWindow?.print()
}
// 导出真实 PDF：新窗口打开打印页（带 autopdf=1 自动触发下载）
const exportPdfFile = (): void => {
  window.open(printSrc.value + '&autopdf=1', '_blank')
}
</script>

<style lang="scss" scoped>
.record-full {
  .rf-tit {
    margin: 16px 0 8px;
    font-size: 15px;
    color: #1F2D3D;
    border-left: 3px solid var(--brand);
    padding-left: 8px;
  }

  .rf-empty { margin: 4px 0; color: #909399; font-size: 13px; }

  .rf-block { margin-bottom: 12px; }

  .rf-block-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 6px;
    font-size: 13px;
    color: #606266;
  }

  .rf-report {
    padding: 8px 12px;
    background: #f5f7fa;
    border-radius: 6px;
    font-size: 13px;
    color: #606266;
    white-space: pre-wrap;
    line-height: 1.6;
  }

  .rf-foot { margin-top: 16px; text-align: right; }
}

/* 打印预览模态 */
.print-dlg {
  .pv-body {
    display: flex;
    gap: 16px;
    height: 78vh;
  }
  .pv-tpls {
    width: 190px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
    overflow-y: auto;
  }
  .pv-tpl {
    border: 2px solid #ebeef5;
    border-radius: 10px;
    padding: 14px 10px;
    text-align: center;
    cursor: pointer;
    transition: all .2s;
    &:hover { border-color: #c6e2ff; }
    &.active { border-color: var(--brand); background: #f0f7fc; }
    .pv-tpl-ico {
      width: 40px; height: 40px; margin: 0 auto 8px;
      border-radius: 10px; color: #fff;
      display: flex; align-items: center; justify-content: center;
    }
    .pv-tpl-name { font-size: 13px; font-weight: 700; color: #1F2D3D; }
    .pv-tpl-desc { font-size: 11px; color: #909399; margin-top: 2px; }
  }
  .pv-preview { flex: 1; min-width: 0; border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden; background: #f5f6fa; }
  .pv-frame { width: 100%; height: 100%; border: none; }
  .pv-tip { float: left; font-size: 12px; color: #909399; line-height: 32px; }
}

.rf-loading { min-height: 200px; }
</style>
