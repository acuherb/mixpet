<template>
  <!--顶部：状态筛选-->
  <el-card class="filter-card" shadow="never">
    <el-select v-model="statusFilter" placeholder="全部状态" size="large" style="width: 180px" clearable @change="nowQuery">
      <el-option label="待检查" :value="false"/>
      <el-option label="已出报告" :value="true"/>
    </el-select>
    <span class="tip">影像费已计入对应账单，检查后在此填写影像报告</span>
  </el-card>

  <!--列表展示-->
  <el-table :data="list.records"
            max-height="calc(100vh - 200px)"
            row-key="orderId"
            stripe>
    <el-table-column label="单号" prop="orderId" width="70"/>
    <el-table-column label="项目" min-width="130" prop="itemName" show-overflow-tooltip/>
    <el-table-column label="分组" width="80">
      <template #default="scope">
        <el-tag size="small">{{ scope.row.groupName || '—' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="宠物" min-width="110" prop="petName" show-overflow-tooltip/>
    <el-table-column label="客户" min-width="110" prop="clientName" show-overflow-tooltip/>
    <el-table-column label="开单医生" min-width="100" prop="doctorName" show-overflow-tooltip/>
    <el-table-column label="诊断" min-width="120" prop="diagnosis" show-overflow-tooltip/>
    <el-table-column label="部位/目的" min-width="110" prop="examPart" show-overflow-tooltip/>
    <el-table-column label="费用(元)" prop="fee" width="100" sortable/>
    <el-table-column label="状态" width="90">
      <template #default="scope">
        <el-tag v-if="scope.row.status" size="small" type="success">已出报告</el-tag>
        <el-tag v-else size="small" type="warning">待检查</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="330" fixed="right">
      <template #default="scope">
        <el-button size="small" @click="printReq(scope.row)">申请单</el-button>
        <el-button v-if="scope.row.status" size="small" type="success" @click="printReport(scope.row)">报告单</el-button>
        <el-button v-if="scope.row.status" size="small" plain @click="printLegacy(scope.row, false)">B超/X光单</el-button>
        <el-button v-if="scope.row.status" size="small" plain @click="printLegacy(scope.row, true)">含图版</el-button>
        <el-button :type="scope.row.status ? 'info' : 'primary'" plain size="small" @click="showDialog(scope.row)">
          {{ scope.row.status ? '查看报告' : '录报告' }}
        </el-button>
        <el-button v-if="!scope.row.status" size="small" type="danger" @click="cancelOrder(scope.row)">作废</el-button>
      </template>
    </el-table-column>
  </el-table>

  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>

  <!--报告录入/查看弹窗-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="640px" :close-on-click-modal="false">
    <div v-if="current" class="case-doc">
      <!-- 文档头：单号 + 项目/宠物 -->
      <div class="case-head">
        <div class="case-hosp">影像单 <span class="case-sub">#{{ current.orderId }}</span></div>
        <div class="case-meta">{{ current.itemName }} · 宠物：{{ current.petName }}</div>
      </div>
      <!-- 报告内容 -->
      <div class="case-sec">
        <div class="case-sec-tit">
          影 像 报 告
          <el-tag v-if="hasTpl && !readonly" size="small" type="success" effect="plain" style="margin-left:8px">模板录入</el-tag>
        </div>
        <!-- 有报告模板：按预置项结构化填写 -->
        <template v-if="hasTpl && !readonly">
          <div class="case-para" v-for="(label, i) in tplLabels" :key="i">
            <label>{{ label }}</label>
            <el-input v-model="tplValues[i]" :rows="2" maxlength="500" type="textarea" :placeholder="`请填写${label}`"/>
          </div>
          <div class="case-para">
            <label>补充说明</label>
            <el-input v-model="extraText" :rows="2" maxlength="1000" placeholder="其他需要说明的情况（选填）" type="textarea"/>
          </div>
        </template>
        <!-- 无模板：自由文本录入 -->
        <div class="case-para" v-else-if="!readonly">
          <label>报告内容</label>
          <el-input v-model="extraText" :rows="6" maxlength="2000" placeholder="描述影像所见及诊断结论" type="textarea"/>
        </div>
        <!-- 查看模式：直接展示报告文本 -->
        <div class="case-para" v-else>
          <label>报告内容</label>
          <p>{{ current?.result || '（无报告内容）' }}</p>
        </div>
        <!-- 结构化：影像所见 / 影像诊断 / DICOM（录入时填写，与模板自由文本并存） -->
        <template v-if="!readonly">
          <div class="case-para">
            <label>影像所见</label>
            <el-input v-model="findings" :rows="3" maxlength="2000" type="textarea" placeholder="客观描述征象"/>
          </div>
          <div class="case-para">
            <label>影像诊断</label>
            <el-input v-model="impression" :rows="2" maxlength="1000" type="textarea" placeholder="结论性诊断"/>
          </div>
          <div class="case-para">
            <label>DICOM</label>
            <el-input v-model="dicomUid" maxlength="128" placeholder="Study UID" style="width: 220px; margin-right: 8px"/>
            <el-input v-model="dicomSeries" maxlength="128" placeholder="Series UID" style="width: 220px"/>
          </div>
        </template>
        <template v-else-if="current?.findings || current?.impression">
          <div class="case-para"><label>影像所见</label><p>{{ current.findings || '—' }}</p></div>
          <div class="case-para"><label>影像诊断</label><p>{{ current.impression || '—' }}</p></div>
          <div class="case-para" v-if="current.dicomUid || current.dicomSeries">
            <label>DICOM</label><p>{{ current.dicomUid || '—' }} / {{ current.dicomSeries || '—' }}</p>
          </div>
        </template>
      </div>
      <!-- 附件 -->
      <div class="case-sec">
        <div class="case-sec-tit">报 告 附 件 / 原 图</div>
        <div class="case-file" v-if="!readonly">
          <div v-for="(f, i) in imgFiles" :key="i" class="img-row">
            <el-link type="primary" :href="fileBase + encodeURIComponent(f)" target="_blank">
              <el-icon><Paperclip/></el-icon> {{ f }}
            </el-link>
            <el-button link type="danger" size="small" @click="imgFiles.splice(i, 1)">移除</el-button>
          </div>
          <UpImg :photo-nm="''" mode="report" @upPhoto="onImgUpload"/>
          <span class="tip" style="margin-left:10px">支持多张原图/报告 PDF，存为原图附件</span>
        </div>
        <div v-else-if="displayFiles.length" class="case-file">
          <el-link v-for="(f, i) in displayFiles" :key="i" type="primary" :href="fileBase + encodeURIComponent(f)" target="_blank" style="margin-right:14px">
            <el-icon><Paperclip/></el-icon> {{ f }}
          </el-link>
        </div>
        <p v-else class="case-empty">暂无附件</p>
      </div>
    </div>
    <template #footer>
      <el-button round @click="closeModal">关闭</el-button>
      <el-button v-if="!readonly" round type="primary" @click="formSubmit">提交报告</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref, defineEmits} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {ImageOrder} from "@/model/entiy/ImageOrder";
import {Page} from "@/model/DO/Page";
import {reqImageCancel, reqImageOrderPage, reqImageTestOne, reqSaveImageReport} from "@/request/ImageApi";
import UpImg from "@/components/UpImg.vue";
import {ElMessage, ElMessageBox} from "element-plus";
import {Res} from "@/request/Res";

const emit = defineEmits<{
  (e: 'pending-count', count: number): void
}>()

const qp = ref<{ numPage: number; pageSize: number; status: boolean | null }>({numPage: 1, pageSize: 20, status: null})
const statusFilter = ref<boolean | null>(null)
const list = ref<Page<ImageOrder>>({records: [], total: 0})

const changePuB = (val: PageQuery): void => {
  qp.value.numPage = val.numPage
  qp.value.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  qp.value.status = (statusFilter.value === false || statusFilter.value === true) ? statusFilter.value : null
  reqImageOrderPage(qp.value).then(res => {
    list.value = res
    // 向上层汇报待检查数量
    emit('pending-count', res.records.filter(r => !r.status).length)
  })
}
const nowQuery = (): void => {
  qp.value.numPage = 1
  sendQuery()
}
const router = useRouter()
const route = useRoute()
// 打印影像申请单（国标）
const printReq = (row: ImageOrder): void => {
  router.push('/doctorPage/z-requisition?type=image&orderId=' + row.orderId)
}
// 打印影像报告单（检查项目+报告文本）
const printReport = (row: ImageOrder): void => {
  router.push('/doctorPage/z-examReport?type=image&recordId=' + row.recordId + '&orderId=' + row.orderId)
}
// 老系统版式 B超/X光 报告单（TypeB/TypeX，A5 横；含图版 A4）
const printLegacy = (row: ImageOrder, full: boolean): void => {
  const name = row.itemName + ' ' + (row.groupName || '')
  const isB = /B超|超声|彩超|腹部超声/i.test(name) && !/X光|DR|拍片|放射/i.test(name)
  const tpl = (isB ? 'TypeB' : 'TypeX') + (full ? 'Full' : '')
  router.push('/doctorPage/z-mrtPrint?tpl=' + tpl + '&recordId=' + row.recordId + '&orderId=' + row.orderId)
}

const modalView = ref(false)
const modalTit = ref('录报告')
const readonly = ref(false)
const current = ref<ImageOrder | null>(null)
// 报告模板（来自影像项目）：结构化填写
const tplLabels = ref<string[]>([])
const tplValues = ref<string[]>([])
const extraText = ref('')
// 结构化报告字段（所见/诊断/DICOM 与原图附件）
const findings = ref('')
const impression = ref('')
const dicomUid = ref('')
const dicomSeries = ref('')
const imgFiles = ref<string[]>([])
const displayFiles = computed<string[]>(() => {
  if (!current.value?.imageFiles) return []
  try { const a = JSON.parse(current.value.imageFiles); return Array.isArray(a) ? a : [] } catch { return [] }
})
const hasTpl = computed(() => tplLabels.value.length > 0)
const parseTpl = (tpl: string | null | undefined): string[] => {
  if (!tpl) return []
  try {
    const arr = JSON.parse(tpl)
    return Array.isArray(arr) ? arr.map((x: unknown) => String(x)) : []
  } catch { return [] }
}
// 按模板拼接报告文本（后端仍存纯文本，零接口改动）
const buildResult = (): string => {
  if (!hasTpl.value) return extraText.value.trim()
  const lines = tplLabels.value.map((label, i) => `【${label}】${(tplValues.value[i] || '').trim()}`)
  const extra = extraText.value.trim()
  if (extra) lines.push('', `补充：${extra}`)
  return lines.join('\n')
}
// 报告附件下载前缀（走独立的报告接口，与头像分离）
const fileBase = window.location.origin + '/api/common/downReport?name='
const onImgUpload = (f: string): void => { if (f) { imgFiles.value.push(f); ElMessage.success('原图已上传：' + f) } }

const showDialog = (row: ImageOrder): void => {
  current.value = row
  readonly.value = !!row.status
  modalView.value = true
  modalTit.value = row.status ? '查看报告' : '录入影像报告'
  // 拉取项目的报告模板，有则按项填写
  tplLabels.value = []
  tplValues.value = []
  extraText.value = ''
  findings.value = row.findings ?? ''
  impression.value = row.impression ?? ''
  dicomUid.value = row.dicomUid ?? ''
  dicomSeries.value = row.dicomSeries ?? ''
  imgFiles.value = row.imageFiles ? (() => { try { return JSON.parse(row.imageFiles) } catch { return [] } })() : []
  reqImageTestOne(row.itemId).then(t => {
    const labels = parseTpl(t?.reportTpl)
    if (labels.length) {
      tplLabels.value = labels
      tplValues.value = labels.map(() => '')
    }
  }).catch(() => { /* 模板接口不可用(如后端未重启)时回落自由文本，不挡录报告 */ })
}

// 关闭前防丢：有未提交内容时确认
const closeModal = (): void => {
  if (!readonly.value && (buildResult().trim() || current.value?.reportFile)) {
    ElMessageBox.confirm('有未提交的报告内容，关闭将丢弃，确定关闭吗？', '提示', {
      confirmButtonText: '丢弃并关闭', cancelButtonText: '继续编辑', type: 'warning'
    }).then(() => { modalView.value = false }).catch(() => {})
  } else {
    modalView.value = false
  }
}

const formSubmit = (): void => {
  if (!current.value) return
  const result = buildResult()
  if (!result.trim()) {
    ElMessage.warning('请填写影像报告内容')
    return
  }
  reqSaveImageReport({
    orderId: current.value.orderId, result, reportFile: current.value.reportFile ?? undefined,
    findings: findings.value, impression: impression.value,
    dicomUid: dicomUid.value, dicomSeries: dicomSeries.value,
    imageFiles: JSON.stringify(imgFiles.value)
  }).then((res: Res) => {
    if (res.code === 1) {
      ElMessage.success('影像报告已提交')
      modalView.value = false
      sendQuery()
    }
  })
}

// 作废申请单（未出报告，冲减未付账单）
const cancelOrder = (row: ImageOrder): void => {
  ElMessageBox.confirm(`确认作废申请单 #${row.orderId}？将冲减未付账单的影像费。`, '作废确认', {
    confirmButtonText: '确认作废', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqImageCancel(row.orderId).then((res: Res) => {
      if (res.code === 1) {
        ElMessage.success('已作废')
        sendQuery()
      } else ElMessage.error(res.msg || '作废失败')
    })
  }).catch(() => ElMessage.info('取消'))
}

onMounted(() => {
  // 从医生工作台「待出报告」进入：默认筛选待检查(status=0待检查 1已出；兼容旧 false/true)
  const s = route.query.status
  if (s === '0' || s === 'false') statusFilter.value = false
  else if (s === '1' || s === 'true') statusFilter.value = true
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
.order-head {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.report-view {
  width: 100%;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  color: #303133;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>