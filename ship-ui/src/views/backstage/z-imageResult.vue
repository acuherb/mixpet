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
    <el-table-column label="操作" width="170" fixed="right">
      <template #default="scope">
        <el-button size="small" @click="printReq(scope.row)">申请单</el-button>
        <el-button :type="scope.row.status ? 'info' : 'primary'" plain size="small" @click="showDialog(scope.row)">
          {{ scope.row.status ? '查看报告' : '录报告' }}
        </el-button>
      </template>
    </el-table-column>
  </el-table>

  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>

  <!--报告录入/查看弹窗-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="560px">
    <div v-if="current" class="order-head">
      单号 #{{ current.orderId }} · {{ current.itemName }} · 宠物：{{ current.petName }}
    </div>
    <el-form label-width="70px">
      <el-form-item label="影像报告">
        <el-input v-model="resultText" :disabled="readonly" :rows="6" maxlength="2000" placeholder="描述影像所见及诊断结论" type="textarea"/>
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
      <el-button v-if="!readonly" type="primary" @click="formSubmit">提交报告</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {ImageOrder} from "@/model/entiy/ImageOrder";
import {Page} from "@/model/DO/Page";
import {reqImageOrderPage, reqSaveImageReport} from "@/request/ImageApi";
import UpImg from "@/components/UpImg.vue";
import {ElMessage} from "element-plus";
import {Res} from "@/request/Res";

const qp = ref<{ numPage: number; pageSize: number; status: boolean | null }>({numPage: 1, pageSize: 15, status: null})
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
  })
}
const nowQuery = (): void => {
  qp.value.numPage = 1
  sendQuery()
}
const router = useRouter()
// 打印影像申请单（国标）
const printReq = (row: ImageOrder): void => {
  router.push('/backstage/z-requisition?type=image&orderId=' + row.orderId)
}

const modalView = ref(false)
const modalTit = ref('录报告')
const readonly = ref(false)
const current = ref<ImageOrder | null>(null)
const resultText = ref('')
// 报告附件下载前缀
const fileBase = window.location.origin + '/api/common/download?name='
const onReportUpload = (f: string): void => { if (current.value) current.value.reportFile = f }
const removeReport = (): void => { if (current.value) current.value.reportFile = undefined }

const showDialog = (row: ImageOrder): void => {
  current.value = row
  resultText.value = row.result || ''
  readonly.value = !!row.status
  modalView.value = true
  modalTit.value = row.status ? '查看报告' : '录入影像报告'
}

const formSubmit = (): void => {
  if (!current.value) return
  if (!resultText.value.trim()) {
    ElMessage.warning('请填写影像报告内容')
    return
  }
  reqSaveImageReport({orderId: current.value.orderId, result: resultText.value, reportFile: current.value.reportFile ?? undefined}).then((res: Res) => {
    if (res.code === 1) {
      ElMessage.success('影像报告已提交')
      modalView.value = false
      sendQuery()
    }
  })
}

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
.order-head {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
</style>