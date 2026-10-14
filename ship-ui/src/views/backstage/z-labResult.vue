<template>
  <!--顶部：状态筛选-->
  <el-card class="filter-card" shadow="never">
    <el-select v-model="statusFilter" placeholder="全部状态" size="large" style="width: 180px" clearable @change="nowQuery">
      <el-option label="待检验" :value="0"/>
      <el-option label="已出结果" :value="1"/>
    </el-select>
    <span class="tip">化验费已计入对应账单，检验后由化验室在此录入结果</span>
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
    <el-table-column label="状态" width="90">
      <template #default="scope">
        <el-tag v-if="scope.row.status" size="small" type="success">已出结果</el-tag>
        <el-tag v-else size="small" type="warning">待检验</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="220" fixed="right">
      <template #default="scope">
        <el-button size="small" @click="printReq(scope.row)">申请单</el-button>
        <el-button :type="scope.row.status ? 'info' : 'primary'" plain size="small" @click="showDialog(scope.row)">
          {{ scope.row.status ? '查看结果' : '录入结果' }}
        </el-button>
        <el-button v-if="!scope.row.status" size="small" type="danger" @click="cancelOrder(scope.row)">作废</el-button>
      </template>
    </el-table-column>
  </el-table>

  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>

  <!--结果录入/查看弹窗-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="620px">
    <div v-if="current" class="order-head">
      单号 #{{ current.orderId }} · 宠物：{{ current.petName }} · 客户：{{ current.clientName }}
    </div>
    <el-table :data="itemForms" size="small" border>
      <el-table-column label="检验项" min-width="150" prop="itemName"/>
      <el-table-column label="参考范围" width="120" prop="refRange"/>
      <el-table-column label="结果值" width="150">
        <template #default="scope">
          <el-input v-if="!readonly" v-model="scope.row.resultValue" placeholder="填写结果值"/>
          <span v-else>{{ scope.row.resultValue || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="异常" width="90">
        <template #default="scope">
          <el-switch v-if="!readonly" v-model="scope.row.isAbnormal" active-text="是" inactive-text="否"/>
          <el-tag v-else-if="scope.row.isAbnormal" size="small" type="danger">异常</el-tag>
          <el-tag v-else size="small" type="success">正常</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-form class="result-form" label-width="70px">
      <el-form-item label="小结">
        <el-input v-model="resultNote" :rows="2" maxlength="255" :disabled="readonly" placeholder="化验结论，可留空" type="textarea"/>
      </el-form-item>
      <el-form-item label="报告附件">
        <UpImg v-if="!current?.reportFile" :photo-nm="''" @upPhoto="onReportUpload"/>
        <div v-else style="display: flex; align-items: center; gap: 10px">
          <el-link type="primary" :href="fileBase + encodeURIComponent(current?.reportFile ?? '')" target="_blank">查看报告附件</el-link>
          <el-button v-if="!readonly" link type="danger" @click="removeReport">移除</el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="modalView = false">关闭</el-button>
      <el-button v-if="!readonly" type="primary" @click="formSubmit">保存结果</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {LabOrderItem} from "@/model/entiy/LabOrderItem";
import {LabOrderDto} from "@/model/DO/LabOrderDto";
import {Page} from "@/model/DO/Page";
import {reqLabByRecord, reqLabCancel, reqLabOrderPage, reqSaveLabResult} from "@/request/LabApi";
import UpImg from "@/components/UpImg.vue";
import {ElMessage, ElMessageBox} from "element-plus";
import {Res} from "@/request/Res";

/**
 ┌───────────────────────────────────┐
 │=============列表查询相关============│
 └───────────────────────────────────┘
 */
const qp = ref<{ numPage: number; pageSize: number; status: number | null }>({numPage: 1, pageSize: 15, status: null})
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
  })
}
const nowQuery = (): void => {
  qp.value.numPage = 1
  sendQuery()
}
const router = useRouter()
// 打印检验申请单（国标）
const printReq = (row: LabOrderDto): void => {
  router.push('/backstage/z-requisition?type=lab&orderId=' + row.orderId)
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
// 报告附件下载前缀
const fileBase = window.location.origin + '/api/common/download?name='
const onReportUpload = (f: string): void => { if (current.value) current.value.reportFile = f }
const removeReport = (): void => { if (current.value) current.value.reportFile = undefined }

const showDialog = (row: LabOrderDto): void => {
  reqLabByRecord(row.recordId).then(orders => {
    const match = orders.find(o => o.orderId === row.orderId)
    current.value = row
    if (match && match.items) {
      itemForms.value = match.items.map((i: LabOrderItem) => ({...i, isAbnormal: i.isAbnormal ?? false}))
    } else {
      itemForms.value = []
    }
    resultNote.value = match?.resultNote || ''
    readonly.value = !!row.status
    modalView.value = true
    modalTit.value = row.status ? '查看结果' : '录入结果'
  })
}

const formSubmit = (): void => {
  if (!current.value) return
  reqSaveLabResult({
    orderId: current.value.orderId,
    resultNote: resultNote.value,
    reportFile: current.value.reportFile ?? undefined,
    items: itemForms.value.map(i => ({
      itemId: i.itemId,
      resultValue: i.resultValue || '',
      isAbnormal: !!i.isAbnormal
    }))
  }).then((res: Res) => {
    if (res.code === 1) {
      ElMessage.success('检验结果已保存，化验单已计费待收款')
      modalView.value = false
      sendQuery()
    }
  })
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
  sendQuery()
})
</script>

<style scoped>
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