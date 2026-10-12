<template>
  <!--操作框-->
  <BackOpCol role="收费" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.seaName" clearable placeholder="客户姓名 / 手机号 / 宠物名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-select v-model="qp.payStatus" size="large" style="width: 120px" @change="nowQuery">
      <el-option :value="-1" label="全部"/>
      <el-option :value="0" label="待收费"/>
      <el-option :value="1" label="已收费"/>
    </el-select>
    <template #left>
      <span class="sel-tip">待收 <b>{{ unpaidCount }}</b> 笔</span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="billList.records"
            max-height="calc(100vh - 130px)"
            row-key="billId"
            :row-class-name="rowClass"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="客户" min-width="90" prop="clientName" show-overflow-tooltip/>
    <el-table-column label="宠物" prop="petName" width="90" show-overflow-tooltip/>
    <el-table-column label="接诊医生" prop="doctorName" width="90" show-overflow-tooltip/>
    <el-table-column label="挂号费" prop="feeRegister" width="80"/>
    <el-table-column label="诊疗费" prop="feeTreat" width="80"/>
    <el-table-column label="药费" prop="feeDrug" width="80"/>
    <el-table-column label="合计(元)" prop="totalMoney" sortable width="100">
      <template #default="scope">
        <b style="color: #f56c6c">￥{{ scope.row.totalMoney }}</b>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="86">
      <template #default="scope">
        <el-tag v-if="!scope.row.payStatus" type="warning">待收费</el-tag>
        <el-tag v-else type="success">{{ scope.row.payType ? '已收现金' : '已收余额' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="余额(元)" prop="clientBalance" width="90"/>
    <el-table-column label="时间" min-width="140" prop="createTime" show-overflow-tooltip>
      <template #default="scope">
        {{ scope.row.payStatus ? scope.row.payTime : scope.row.createTime }}
      </template>
    </el-table-column>
    <el-table-column fixed="right" label="操作" width="150">
      <template #default="scope">
        <el-button-group>
          <el-button v-if="!scope.row.payStatus"
                     circle title="余额收款（扣客户余额并发药）" type="primary" @click="payBill(scope.row, false)">余
          </el-button>
          <el-button v-if="!scope.row.payStatus"
                     circle title="现金收款（只发药）" type="success" @click="payBill(scope.row, true)">现
          </el-button>
          <el-button circle title="查看病历" type="info" @click="showRecord(scope.row)">历</el-button>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="billList.total" @changePu="changePuB"/>
  <!--病历详情弹窗-->
  <el-dialog v-model="recordView" title="病历详情" top="6vh" width="90%" style="max-width: 640px">
    <el-descriptions v-if="recordDetail" :column="2" border>
      <el-descriptions-item label="宠物">{{ recordDetail.record.petName }}</el-descriptions-item>
      <el-descriptions-item label="客户">{{ recordDetail.record.clientName }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="主诉">{{ recordDetail.record.chiefComplaint || '无' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="诊断">{{ recordDetail.record.diagnosis || '无' }}</el-descriptions-item>
      <el-descriptions-item :span="2" label="医嘱">{{ recordDetail.record.advice || '无' }}</el-descriptions-item>
      <el-descriptions-item label="接诊医生">{{ recordDetail.record.doctorName }}</el-descriptions-item>
      <el-descriptions-item label="就诊时间">{{ recordDetail.record.createTime }}</el-descriptions-item>
    </el-descriptions>
    <h4 style="margin: 12px 0 6px">处方</h4>
    <el-table v-if="recordDetail" :data="recordDetail.prescriptions" border size="small">
      <el-table-column label="药品" min-width="120" prop="drugName" show-overflow-tooltip/>
      <el-table-column label="规格" min-width="100" prop="drugSpec" show-overflow-tooltip/>
      <el-table-column label="数量" prop="quantity" width="60"/>
      <el-table-column label="单价" prop="drugPrice" width="70"/>
      <el-table-column label="用法" min-width="130" prop="usageInfo" show-overflow-tooltip/>
    </el-table>
    <p v-if="recordDetail && recordDetail.prescriptions.length === 0" style="color:#999">未开处方</p>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {Page} from "@/model/DO/Page";
import {BillDto} from "@/model/DO/BillDto";
import {reqBillPage, reqBillPay} from "@/request/BillApi";
import {reqRecordOne} from "@/request/RecordApi";
import {RecordDetailVo} from "@/model/DO/RecordDto";
import {ElMessage, ElMessageBox} from "element-plus";

/**
 ┌───────────────────────────────────┐
 │=============生命周期相关============│
 └───────────────────────────────────┘
 */
onMounted(() => {
  sendQuery()
})

/**
 ┌───────────────────────────────────┐
 │=============表格查询相关============│
 └───────────────────────────────────┘
 */
// 查询的参数
const qp = reactive({
  seaName: '',
  payStatus: 0,
  numPage: 1,
  pageSize: 15
})
// 列表展示
const billList = ref<Page<BillDto>>({records: [], total: 0})
// 待收笔数（当前筛选下）
const unpaidCount = computed(() => billList.value.records.filter(b => !b.payStatus).length)
// 待收费行高亮
const rowClass = ({row}: { row: BillDto }): string => row.payStatus ? '' : 'unpaid-row'
// 分页条
const changePuB = (val: PageQuery) => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
// 数据总览
const sendQuery = (): void => {
  reqBillPage(qp).then(res => {
    billList.value = res
  })
}
// 搜索：输入即查（防抖），查询回到第一页
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}
const debQuery = debounce(nowQuery, 400)

/**
 ┌───────────────────────────────────┐
 │=============收款相关============│
 └───────────────────────────────────┘
 */
const payBill = (row: BillDto, cash: boolean): void => {
  const payName = cash ? '现金' : '余额'
  let tip = `确认以【${payName}】方式收取 ${row.clientName}（${row.petName}）的 ￥${row.totalMoney} 吗？`
  if (!cash) tip += `（当前余额 ￥${row.clientBalance ?? 0}，收款后自动发药扣库存）`
  else tip += '（收款后自动发药扣库存）'
  ElMessageBox.confirm(tip, '收款确认', {
    confirmButtonText: '确认收款',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    reqBillPay(row.billId, cash).then(res => {
      if (res.code === 1) sendQuery()
    })
  }).catch(() => ElMessage.info('取消收款'))
}

/**
 ┌───────────────────────────────────┐
 │=============病历查看============│
 └───────────────────────────────────┘
 */
const recordView = ref(false)
const recordDetail = ref<RecordDetailVo | null>(null)
const showRecord = (row: BillDto): void => {
  if (!row.recordId) {
    ElMessage.info('该账单没有关联病历')
    return
  }
  reqRecordOne(row.recordId).then(res => {
    if (res) {
      recordDetail.value = res
      recordView.value = true
    }
  })
}

/**
 ┌───────────────────────────────────┐
 │=============批量删除占位============│
 └───────────────────────────────────┘
 */
const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: BillDto[]): void => {
  roleIdList.value = val.map(obj => obj.billId)
}
// 账单不允许删除，仅占位避免 BackOpCol 报错
const delBatchB = (): void => {
  ElMessage.info('账单不允许删除')
}
</script>

<style scoped>
.sel-tip {
  margin-right: 12px;
  font-size: 14px;
  color: #e6a23c;
}
.sel-tip b {
  font-size: 16px;
  margin: 0 2px;
}

:deep(.unpaid-row) {
  background: #fdf6ec !important;
}
</style>
