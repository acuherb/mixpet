<template>
  <!--顶部：状态筛选-->
  <el-card class="filter-card" shadow="never">
    <el-select v-model="statusFilter" placeholder="全部状态" size="large" style="width: 180px" clearable @change="nowQuery">
      <el-option label="待检验" :value="0"/>
      <el-option label="已出结果" :value="1"/>
    </el-select>
    <span class="tip">异常与危急值由服务端按宠物物种/月龄匹配的参考区间复算，录入端只做预检</span>
  </el-card>

  <!--列表展示-->
  <el-table :data="list.records"
            max-height="calc(100vh - 200px)"
            row-key="orderId"
            stripe
            @expand-change="loadDetail">
    <el-table-column type="expand" width="40">
      <template #default="scope">
        <div class="rows">
          <p v-if="scope.row.items && scope.row.items.length" class="lab-tit">检验明细</p>
          <el-table :data="scope.row.items || []" size="small" border>
            <el-table-column label="检验项" min-width="160" prop="itemName"/>
            <el-table-column label="结果值" width="130">
              <template #default="it">
                <span v-if="it.row.resultValue">{{ it.row.resultValue }}</span>
                <el-tag v-else size="small" type="info">未录入</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="参考范围" width="120" prop="refRange"/>
            <el-table-column label="单位" width="80" prop="itemUnit"/>
            <el-table-column label="异常" width="80">
              <template #default="it">
                <el-tag v-if="it.row.isAbnormal" size="small" type="danger">异常</el-tag>
                <el-tag v-else-if="it.row.resultValue" size="small" type="success">正常</el-tag>
                <span v-else>/</span>
              </template>
            </el-table-column>
            <el-table-column label="危急" width="80">
              <template #default="it">
                <el-tag v-if="it.row.criticalFlag" size="small" type="danger" effect="dark">危急值</el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
          </el-table>
          <p v-if="scope.row.resultBy" class="lab-note">
            小结：<span>{{ scope.row.resultNote || '无' }}</span>
            <em>（录入人：{{ scope.row.resultBy }}）</em>
          </p>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="单号" prop="orderId" width="70"/>
    <el-table-column label="宠物" min-width="110" prop="petName" show-overflow-tooltip/>
    <el-table-column label="客户" min-width="110" prop="clientName" show-overflow-tooltip/>
    <el-table-column label="开单医生" min-width="100" prop="doctorName" show-overflow-tooltip/>
    <el-table-column label="诊断" min-width="120" prop="diagnosis" show-overflow-tooltip/>
    <el-table-column label="检验费(元)" prop="fee" width="100" sortable/>
    <el-table-column label="开单时间" width="160" prop="createTime"/>
    <el-table-column label="状态" width="110">
      <template #default="scope">
        <el-tag v-if="scope.row.criticalFlag && !scope.row.criticalHandled" size="small" type="danger" effect="dark">危急值·待处置</el-tag>
        <el-tag v-else-if="scope.row.criticalFlag && scope.row.criticalHandled" size="small" type="warning" effect="dark">危急值·已处置</el-tag>
        <el-tag v-else-if="scope.row.reviewBy" size="small" type="success">已复核</el-tag>
        <el-tag v-else-if="scope.row.status" size="small" type="success">已出结果</el-tag>
        <el-tag v-else-if="isOverdue(scope.row)" size="small" type="danger">超时未检</el-tag>
        <el-tag v-else size="small" type="warning">待检验</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="300" fixed="right">
      <template #default="scope">
        <el-button size="small" @click="printReq(scope.row)">申请单</el-button>
        <el-button v-if="scope.row.status" size="small" type="success" @click="printReport(scope.row)">报告单</el-button>
        <el-button :type="scope.row.status ? 'info' : 'primary'" plain size="small" @click="showDialog(scope.row)">
          {{ !scope.row.status ? '录入结果' : (scope.row.reviewBy ? '查看结果' : '修订/复核') }}
        </el-button>
        <el-button v-if="scope.row.status && !scope.row.reviewBy" size="small" type="warning" @click="reviewOrder(scope.row)">复核</el-button>
        <el-button v-if="scope.row.criticalFlag && scope.row.status && !scope.row.criticalHandled" size="small" type="danger" @click="handleCritical(scope.row)">危急处置</el-button>
        <el-button v-if="scope.row.reviewBy" size="small" type="info" plain @click="revokeReview(scope.row)">撤销复核</el-button>
        <el-button v-if="!scope.row.status" size="small" type="danger" @click="cancelOrder(scope.row)">作废</el-button>
      </template>
    </el-table-column>
  </el-table>

  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>

  <!--结果录入/查看弹窗-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="640px">
    <div v-if="current" class="case-doc">
      <!-- 文档头：单号 + 宠物/客户 -->
      <div class="case-head">
        <div class="case-hosp">化验单 <span class="case-sub">#{{ current.orderId }}</span></div>
        <div class="case-meta">宠物：{{ current.petName }} · 客户：{{ current.clientName }}</div>
      </div>
      <!-- 标本与时间轴：采样 → 接收 → 出结果 → 复核，全程可追溯 -->
      <div class="case-sec">
        <div class="case-sec-tit">标 本 与 时 间 轴</div>
        <div class="case-para">
          <label>标本类型</label>
          <el-select v-if="!readonly" v-model="specimenType" placeholder="选择标本类型" size="small" style="width: 200px" clearable>
            <el-option v-for="s in specimenOptions" :key="s" :label="s" :value="s"/>
          </el-select>
          <p v-else>{{ specimenType || '—' }}</p>
        </div>
        <div class="case-para">
          <label>采样 / 接收</label>
          <template v-if="!readonly">
            <el-date-picker v-model="collectTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="采样时间" size="small" style="width: 190px"/>
            <el-date-picker v-model="receiveTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="接收时间" size="small" style="width: 190px; margin-left: 8px"/>
          </template>
          <p v-else>{{ collectTime || '—' }} → {{ receiveTime || '—' }}</p>
        </div>
        <div class="case-para" v-if="current?.reviewBy">
          <label>复核</label>
          <p>{{ current.reviewBy }} · {{ current.reviewAt || '' }}</p>
        </div>
        <div class="case-para" v-if="current?.criticalFlag">
          <label>危急值</label>
          <p v-if="current.criticalHandled" style="color:#e6a23c">
            已处置 · {{ current.criticalHandleBy }} · {{ current.criticalHandleAt || '' }}
          </p>
          <p v-else style="color:#f56c6c">待处置（已通知接诊医师：{{ current.criticalNotifyAt || '' }}）</p>
        </div>
      </div>
      <!-- 结果表：现代样式 -->
      <div class="case-sec">
        <div class="case-sec-tit">检 验 结 果</div>
        <el-table :data="itemForms" size="small" class="case-table">
          <el-table-column label="检验项" min-width="150" prop="itemName"/>
          <el-table-column label="参考范围" width="120" prop="refRange"/>
          <el-table-column label="结果值" width="150">
            <template #default="scope">
              <el-input v-if="!readonly" v-model="scope.row.resultValue" placeholder="填写结果值" @blur="autoAbnormal(scope.row)"/>
              <span v-else :class="{ 'abnormal-text': scope.row.isAbnormal }">{{ scope.row.resultValue || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="异常（预检）" width="120">
            <template #default="scope">
              <el-switch v-if="!readonly" v-model="scope.row.isAbnormal" active-text="是" inactive-text="否"/>
              <el-tag v-else-if="scope.row.isAbnormal" size="small" type="danger">异常</el-tag>
              <el-tag v-else size="small" type="success">正常</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="危急" width="80">
            <template #default="scope">
              <el-tag v-if="scope.row.criticalFlag" size="small" type="danger" effect="dark">危急</el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>
        <p class="lab-note">最终异常/危急判定以服务端按参考区间复算结果为准，保存后不可手工覆盖。</p>
      </div>
      <!-- 小结与附件 -->
      <div class="case-sec">
        <div class="case-sec-tit">小 结 与 附 件</div>
        <div class="case-para" v-if="isRevise">
          <label>修订原因</label>
          <el-input v-model="reviseReason" maxlength="100" placeholder="已出结果的报告修订必填，写入留痕" size="small"/>
        </div>
        <div class="case-para">
          <label>小结</label>
          <p v-if="readonly">{{ resultNote || '—' }}</p>
          <el-input v-else v-model="resultNote" :rows="2" maxlength="255" placeholder="化验结论，可留空" type="textarea"/>
        </div>
        <div class="case-file" v-if="current?.reportFile">
          <el-link type="primary" :href="fileBase + encodeURIComponent(current?.reportFile ?? '')" target="_blank">
            <el-icon><Paperclip/></el-icon> 查看报告附件
          </el-link>
          <el-button v-if="!readonly" link type="danger" size="small" @click="removeReport">移除</el-button>
        </div>
        <div v-else-if="!readonly" class="case-file">
          <UpImg :photo-nm="''" mode="report" @upPhoto="onReportUpload"/>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button round @click="modalView = false">关闭</el-button>
      <el-button v-if="current?.status && !current?.reviewBy" round type="warning" @click="reviewOrder(current)">复核报告</el-button>
      <el-button v-if="!readonly" round type="primary" @click="formSubmit">{{ isRevise ? '保存修订' : '保存结果' }}</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {onMounted, ref, defineEmits} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {LabOrderItem} from "@/model/entiy/LabOrderItem";
import {LabOrderDto} from "@/model/DO/LabOrderDto";
import {Page} from "@/model/DO/Page";
import {reqLabByRecord, reqLabCancel, reqLabOrderPage, reqHandleCritical, reqReviewLabResult, reqRevokeReview, reqSaveLabResult} from "@/request/LabApi";
import UpImg from "@/components/UpImg.vue";
import {ElMessage, ElMessageBox} from "element-plus";
import {Res} from "@/request/Res";

const emit = defineEmits<{
  (e: 'pending-count', count: number): void
}>()

// 判断是否超过 24 小时未出结果（仅待检验单，createTime 为开单时间）
const isOverdue = (row: LabOrderDto): boolean => {
  if (row.status || !row.createTime) return false
  const t = new Date(row.createTime).getTime()
  if (isNaN(t)) return false
  return Date.now() - t > 24 * 60 * 60 * 1000
}

/**
 ┌───────────────────────────────────┐
 │=============列表查询相关============│
 └───────────────────────────────────┘
 */
const qp = ref<{ numPage: number; pageSize: number; status: number | null }>({numPage: 1, pageSize: 20, status: null})
const statusFilter = ref<number | null>(null)
const list = ref<Page<LabOrderDto>>({records: [], total: 0})

const changePuB = (val: PageQuery): void => {
  qp.value.numPage = val.numPage
  qp.value.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  // 将"全部/清空"统一归为 null，避免向后端传空串
  qp.value.status = (statusFilter.value === 0 || statusFilter.value === 1) ? statusFilter.value : null
  reqLabOrderPage(qp.value).then(res => {
    list.value = res
    // 预置明细为空对象，便于展开表格渲染
    res.records.forEach(r => { if (!r.items) r.items = [] })
    // 向上层汇报待检验数量
    emit('pending-count', res.records.filter(r => !r.status).length)
    // 超期预警：有多张化验单超过 24 小时未录结果，弹一次性提示
    const overdue = res.records.filter(isOverdue)
    if (overdue.length) {
      ElMessage.warning(`有 ${overdue.length} 张化验单开单超 24 小时仍未出结果，请优先处理`)
    }
  })
}
const nowQuery = (): void => {
  qp.value.numPage = 1
  sendQuery()
}
const router = useRouter()
const route = useRoute()
// 打印检验申请单（国标）
const printReq = (row: LabOrderDto): void => {
  router.push('/doctorPage/z-requisition?type=lab&orderId=' + row.orderId)
}
// 打印检验报告单（含结果/参考范围/异常标记）
const printReport = (row: LabOrderDto): void => {
  router.push('/doctorPage/z-examReport?type=lab&recordId=' + row.recordId + '&orderId=' + row.orderId)
}
// 展开某行时自动加载质检结果明细
const loadDetail = (row: LabOrderDto, expandedRows: LabOrderDto[]): void => {
  if (expandedRows.includes(row)) {
    reqLabByRecord(row.recordId).then(orders => {
      const match = orders.find(o => o.orderId === row.orderId)
      if (match && match.items) row.items = match.items
    })
  }
}

/**
 ┌───────────────────────────────────┐
 │=============结果录入相关============│
 └───────────────────────────────────┘
 */
const modalView = ref(false)
const modalTit = ref('录入结果')
const readonly = ref(false)
const current = ref<LabOrderDto | null>(null)
// 明细编辑表单（带响应性，避免直接改列表行）
const itemForms = ref<LabOrderItem[]>([])
const resultNote = ref('')
// 标本与时间轴 / 修订留痕
const specimenOptions = ['全血', '血清', '血浆', '尿液', '粪便', '拭子', '穿刺液', '其他']
const specimenType = ref<string | null>(null)
const collectTime = ref<string | null>(null)
const receiveTime = ref<string | null>(null)
const reviseReason = ref('')
/** 已出结果但未复核：允许带原因修订，修订会保存变更前整单快照 */
const isRevise = ref(false)
// 报告附件下载前缀（走独立的报告接口，与头像分离）
const fileBase = window.location.origin + '/api/common/downReport?name='
const onReportUpload = (f: string): void => { if (current.value) current.value.reportFile = f }
const removeReport = (): void => { if (current.value) current.value.reportFile = undefined }

const showDialog = (row: LabOrderDto): void => {
  reqLabByRecord(row.recordId).then(orders => {
    const detail = orders.find(o => o.orderId === row.orderId) || row
    current.value = detail
    itemForms.value = (detail.items || []).map((i: LabOrderItem) => ({...i, isAbnormal: i.isAbnormal ?? false}))
    resultNote.value = detail.resultNote || ''
    specimenType.value = detail.specimenType ?? null
    collectTime.value = detail.collectTime ?? null
    receiveTime.value = detail.receiveTime ?? null
    reviseReason.value = ''
    // 已出结果但尚未复核 → 允许带原因修订（服务端留变更前快照）；已复核 → 只读
    isRevise.value = !!detail.status && !detail.reviewBy
    readonly.value = !!detail.reviewBy
    modalView.value = true
    modalTit.value = !detail.status ? '录入结果' : (isRevise.value ? '修订结果' : '查看结果（已复核）')
  })
}

const formSubmit = (): void => {
  if (!current.value) return
  if (isRevise.value && !reviseReason.value.trim()) {
    ElMessage.warning('修订已出具的报告必须填写修订原因')
    return
  }
  reqSaveLabResult({
    orderId: current.value.orderId,
    resultNote: resultNote.value,
    reportFile: current.value.reportFile ?? undefined,
    specimenType: specimenType.value,
    collectTime: collectTime.value,
    receiveTime: receiveTime.value,
    reviseReason: isRevise.value ? reviseReason.value : undefined,
    items: itemForms.value.map(i => ({
      itemId: i.itemId,
      resultValue: i.resultValue || '',
      // 非数值结果的录入预检；数值结果一律由服务端按参考区间复算
      isAbnormal: !!i.isAbnormal
    }))
  }).then((res: Res) => {
    if (res.code === 1) {
      ElMessage.success(res.msg || '检验结果已保存')
      modalView.value = false
      sendQuery()
    }
  })
}

/** 报告复核：录入人之外再签审核人，复核后锁定，不可再直接修订 */
const reviewOrder = (row: LabOrderDto | null): void => {
  if (!row) return
  ElMessageBox.confirm(`确认复核化验单 #${row.orderId} 的检验报告？复核后结果锁定。`, '报告复核', {
    confirmButtonText: '确认复核', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqReviewLabResult(row.orderId).then((res: Res) => {
      if (res.code === 1) { ElMessage.success(res.msg || '复核完成'); sendQuery() }
      else ElMessage.error(res.msg || '复核失败')
    })
  }).catch(() => { /* 取消 */ })
}

/** 危急值处置闭环：标记已处置并记录处置人 */
const handleCritical = (row: LabOrderDto | null): void => {
  if (!row) return
  ElMessageBox.confirm(`确认处置化验单 #${row.orderId} 的危急值？处置后需同步电话通知接诊医师并留存处置记录。`, '危急值处置', {
    confirmButtonText: '确认已处置', cancelButtonText: '取消', type: 'error'
  }).then(() => {
    reqHandleCritical(row.orderId).then((res: Res) => {
      if (res.code === 1) { ElMessage.success(res.msg || '已处置'); sendQuery() }
      else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => { /* 取消 */ })
}

/** 撤销复核（管理层）：解除复核锁，允许再次修订 */
const revokeReview = (row: LabOrderDto | null): void => {
  if (!row) return
  ElMessageBox.confirm(`确认撤销化验单 #${row.orderId} 的复核？撤销后报告重新开放修订。`, '撤销复核', {
    confirmButtonText: '确认撤销', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqRevokeReview(row.orderId).then((res: Res) => {
      if (res.code === 1) { ElMessage.success(res.msg || '已撤销复核'); sendQuery() }
      else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => { /* 取消 */ })
}

/**
 * 录入时的异常预检（失焦触发，仅给操作者即时反馈）：
 * 支持 "10-20" / "10~20" 区间与 ">5" / "<3" 等常见格式；解析不出则不干预。
 * 最终判定以服务端 LabResultEvaluator 复算为准（含危急值），保存后前端开关不生效。
 */
const autoAbnormal = (row: LabOrderItem): void => {
  const val = parseFloat((row.resultValue || '').trim())
  const ref = (row.refRange || '').trim()
  if (isNaN(val) || !ref) return
  let abnormal: boolean | null = null
  const range = ref.match(/^(-?[\d.]+)\s*[-~]\s*(-?[\d.]+)$/)
  if (range) {
    abnormal = val < parseFloat(range[1]) || val > parseFloat(range[2])
  } else {
    const gt = ref.match(/^[>≥]\s*(-?[\d.]+)$/)
    const lt = ref.match(/^[<≤]\s*(-?[\d.]+)$/)
    if (gt) abnormal = val > parseFloat(gt[1])
    else if (lt) abnormal = val < parseFloat(lt[1])
  }
  if (abnormal != null) row.isAbnormal = abnormal
}

// 作废化验单（未出结果，冲减未付账单）
const cancelOrder = (row: LabOrderDto): void => {
  ElMessageBox.confirm(`确认作废化验单 #${row.orderId}？将冲减未付账单的检验费。`, '作废确认', {
    confirmButtonText: '确认作废', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqLabCancel(row.orderId).then((res: Res) => {
      if (res.code === 1) {
        ElMessage.success('已作废')
        sendQuery()
      } else ElMessage.error(res.msg || '作废失败')
    })
  }).catch(() => ElMessage.info('取消'))
}

/**
 ┌───────────────────────────────────┐
 │=============生命周期相关============│
 └───────────────────────────────────┘
 */
onMounted(() => {
  // 从医生工作台「待出结果」进入：默认筛选待出结果(status=0)
  const s = route.query.status
  if (s != null && s !== '') statusFilter.value = Number(s)
  sendQuery()
})
</script>

<style scoped>
/* 现代病历文档样式（与顾客端就诊详情同一套） */
@import "@/styles/case-doc.scss";

.filter-card {
  margin-bottom: 8px;
}
.tip {
  margin-left: 20px;
  font-size: 12px;
  color: #909399;
}
.rows {
  padding: 8px 40px;
}
.lab-tit {
  font-size: 13px;
  color: #606266;
  margin: 0 0 6px;
}
.lab-note {
  font-size: 13px;
  color: #606266;
  margin: 8px 0 0;
}
.lab-note em {
  font-style: normal;
  color: #909399;
  margin-left: 8px;
}
.order-head {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.result-form {
  margin-top: 12px;
}
</style>