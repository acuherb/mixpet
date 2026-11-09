<template>
  <!--操作框-->
  <BackOpCol :can-add="false" :can-delete="false" role="收费" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.seaName" clearable placeholder="客户姓名 / 手机号 / 宠物名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-select v-model="qp.payStatus" size="large" style="width: 120px" @change="nowQuery">
      <el-option :value="-1" label="全部"/>
      <el-option :value="0" label="待收费"/>
      <el-option :value="1" label="已收费"/>
    </el-select>
    <template #left>
      <span class="sel-tip">待收 <b>{{ unpaidCount }}</b> 笔</span>
      <el-button size="large" plain round type="success" style="margin-left: 12px" @click="exportBills">导出</el-button>
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
    <el-table-column label="化验费" prop="feeLab" width="80">
      <template #default="scope">
        <span v-if="scope.row.feeLab > 0">￥{{ scope.row.feeLab }}</span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="影像费" prop="feeImage" width="80">
      <template #default="scope">
        <span v-if="scope.row.feeImage > 0">￥{{ scope.row.feeImage }}</span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="住院费" prop="feeHospital" width="80">
      <template #default="scope">
        <span v-if="scope.row.feeHospital > 0">￥{{ scope.row.feeHospital }}</span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="套餐费" prop="feePackage" width="80">
      <template #default="scope">
        <span v-if="scope.row.feePackage > 0">￥{{ scope.row.feePackage }}</span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="手术费" prop="feeSurgery" width="80">
      <template #default="scope">
        <span v-if="scope.row.feeSurgery > 0">￥{{ scope.row.feeSurgery }}</span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="疫苗费" prop="feeImmune" width="80">
      <template #default="scope">
        <span v-if="scope.row.feeImmune > 0">￥{{ scope.row.feeImmune }}</span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="原价(元)" prop="totalMoney" sortable width="100">
      <template #default="scope">
        ￥{{ scope.row.totalMoney }}
      </template>
    </el-table-column>
    <el-table-column label="优惠" width="80">
      <template #default="scope">
        <span v-if="scope.row.discountMoney > 0" style="color: #e6a23c">-￥{{ scope.row.discountMoney }}</span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column label="应付(元)" width="100">
      <template #default="scope">
        <b style="color: #f56c6c">￥{{ realMoneyOf(scope.row) }}</b>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="86">
      <template #default="scope">
        <el-tag v-if="!scope.row.payStatus" type="warning">待收费</el-tag>
        <el-tag v-else type="success">{{ scope.row.payType ? '已收现金' : '已收余额' }}</el-tag>
        <el-tag v-if="scope.row.refundStatus" style="margin-left: 4px" type="info">已退</el-tag>
        <el-tag v-else-if="scope.row.payStatus && scope.row.dispenseStatus" style="margin-left: 4px" type="info">已发药</el-tag>
        <el-tag v-else-if="scope.row.payStatus" style="margin-left: 4px" type="danger">待发药</el-tag>
      </template>
    </el-table-column>
    <!-- 收银者：谁登录谁收款即记录（忙时医生代收也可追溯） -->
    <el-table-column label="收银员" width="90" show-overflow-tooltip>
      <template #default="scope">
        <span v-if="scope.row.payBy">{{ scope.row.payBy }}</span>
        <span v-else-if="scope.row.payStatus" style="color:#909399">—（历史）</span>
        <span v-else style="color:#c0c4cc">—</span>
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
        <!-- 主操作：收款（未付）/ 发药核对（已付未发药） -->
        <el-button v-if="!scope.row.payStatus" size="small" type="primary" @click="payChoice(scope.row)">收款</el-button>
        <el-button v-if="scope.row.payStatus && !scope.row.dispenseStatus" size="small" type="danger"
                   @click="dispenseBill(scope.row)">发药核对</el-button>
        <!-- 次要操作折叠：打印/充值/退款/病历 -->
        <!-- 次要操作折叠：打印/充值/退款/病历（点击展开；表格内需显式 trigger=click） -->
        <el-dropdown style="margin-left: 6px" trigger="click" @command="(cmd: string) => onMore(cmd, scope.row)">
          <el-button size="small">
            更多<el-icon style="margin-left: 2px"><ArrowDown/></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="scope.row.payStatus" command="ticket">打印小票</el-dropdown-item>
              <el-dropdown-item v-if="scope.row.payStatus" command="invoice">打印收费凭证</el-dropdown-item>
              <el-dropdown-item v-if="!scope.row.payStatus" command="recharge">充值余额</el-dropdown-item>
              <el-dropdown-item v-if="scope.row.payStatus && !scope.row.refundStatus" command="refund" divided>
                退款冲正
              </el-dropdown-item>
              <el-dropdown-item command="record" divided>查看病历</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </el-table-column>
  </el-table>

  <!-- 收款方式选择弹窗（余额/现金，逻辑差异明示） -->
  <el-dialog v-model="payView" append-to-body title="收款" width="92%" style="max-width: 420px">
    <p style="margin: 0 0 12px">账单应付 <b style="color:#f56c6c">￥{{ payRow ? realMoneyOf(payRow) : '0.00' }}</b>
      （客户余额 ￥{{ payRow?.clientBalance ?? 0 }}）</p>
    <div class="pay-choice">
      <div class="pay-card" @click="payBill(payRow!, false)">
        <h4>余额收款</h4>
        <p>扣客户余额；收款后需完成发药核对</p>
      </div>
      <div class="pay-card" @click="payBill(payRow!, true)">
        <h4>现金收款</h4>
        <p>线下收现金；收款后需完成发药核对</p>
      </div>
    </div>
    <template #footer>
      <el-button @click="payView = false">取消</el-button>
    </template>
  </el-dialog>
  <!--分页条-->
  <BackPage :total="billList.total" @changePu="changePuB"/>
  <!--病历详情弹窗（完整病例档案）-->
  <el-dialog v-model="recordView" title="病历详情" top="6vh" width="90%" style="max-width: 900px">
    <RecordDetailFull v-if="recordId" :record-id="recordId"/>
  </el-dialog>

  <!-- 发药核对：对照处方逐项勾选后发放 -->
  <el-dialog v-model="dispenseView" append-to-body title="发药核对" width="90%" style="max-width: 520px"
             :close-on-click-modal="false">
    <el-alert :closable="false" show-icon style="margin-bottom:10px" type="warning"
              title="请对照处方逐项核对药品（名称/规格/数量/用法），全部勾选后方可确认发放"/>
    <el-table :data="dispenseRx" border size="small">
      <el-table-column width="50">
        <template #default="scope">
          <el-checkbox v-model="dispenseChecks[scope.$index]"/>
        </template>
      </el-table-column>
      <el-table-column label="药品" min-width="110" prop="drugName" show-overflow-tooltip/>
      <el-table-column label="规格" min-width="80" prop="drugSpec" show-overflow-tooltip/>
      <el-table-column label="数量" width="70">
        <template #default="scope">{{ scope.row.quantity }}{{ scope.row.drugUnit }}</template>
      </el-table-column>
      <el-table-column label="用法" min-width="120" prop="usageInfo" show-overflow-tooltip/>
    </el-table>
    <p v-if="!dispenseRx.length" style="color:#909399;margin:8px 0">该账单无处方药品（纯诊疗/检验费用可直接核对发放）</p>
    <template #footer>
      <span style="float:left;font-size:12px;color:#909399;line-height:32px">
        已核对 {{ checkedCount }} / {{ dispenseRx.length }} 项 · 发药人：当前登录人
      </span>
      <el-button @click="dispenseView = false">取消</el-button>
      <el-button :disabled="!allChecked" type="primary" @click="submitDispense">确认发放</el-button>
    </template>
  </el-dialog>

  <!--退款冲正：可选处方行部分退（默认全选=整单退）；应退金额实时计算-->
  <el-dialog v-model="refundView" append-to-body title="退款冲正" width="92%" style="max-width: 560px"
             :close-on-click-modal="false">
    <el-alert :closable="false" show-icon style="margin-bottom:10px" type="warning"
              :title="`将${refundRow?.payType ? '退还现金（请线下退）' : '退还余额'}并回滚已扣库存；操作人：当前登录人`"/>
    <div v-if="refundRow" class="refund-head">
      <span>账单 #{{ refundRow.billId }} · {{ refundRow.petName }}（{{ refundRow.clientName }}）</span>
      <span>待退总额 ￥{{ refundRow.totalMoney }}</span>
    </div>
    <!-- 处方行勾选（无处方行的单：整单退） -->
    <div v-if="refundRx.length" class="refund-rx">
      <div class="refund-rx-head">
        <span>选择要退的药品行（{{ refundChecks.length }}/{{ refundRx.length }}）</span>
        <el-button size="small" link type="primary" @click="selectAllRefund">全选</el-button>
        <el-button size="small" link @click="refundChecks = []">全不选</el-button>
      </div>
      <el-checkbox-group v-model="refundChecks" class="refund-checks">
        <div v-for="p in refundRx" :key="p.prescriptionId" class="refund-check">
          <el-checkbox :label="p.prescriptionId">
            {{ p.drugName }}（{{ p.drugSpec || '—' }}） ×{{ p.quantity }}{{ p.drugUnit }}
            <span class="refund-fee">￥{{ ((p.drugPrice ?? 0) * (p.quantity ?? 0)).toFixed(2) }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <div class="refund-total">
        <span>应退金额（{{ refundIsFull() ? '整单' : '部分' }}）</span>
        <b :style="{color: '#f56c6c'}">￥{{ refundFee().toFixed(2) }}</b>
      </div>
      <div class="refund-tip">部分退：勾选行标记已退、库存回滚、账单金额相应扣减；剩余行照常发药收款</div>
    </div>
    <el-form v-else label-width="70px" style="margin-top:8px">
      <el-form-item label="说明">
        <span style="color:#909399;font-size:12px">该单无处方药品，退款将冲正整单金额</span>
      </el-form-item>
    </el-form>
    <el-form label-width="70px" style="margin-top:10px">
      <el-form-item label="退款原因" required>
        <el-input v-model="refundReason" :rows="2" maxlength="120" placeholder="如：顾客退药/开单错误/重复收费（必填，财务留痕）" type="textarea"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button round @click="refundView = false">取消</el-button>
      <el-button round type="danger" :loading="refundSaving" @click="submitRefund">确认退款</el-button>
    </template>
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
import {reqBillDispense, reqBillPage, reqBillPay, reqBillRefund} from "@/request/BillApi";
import {exportCsv} from "@/utils/ExportUtil";
import {reqRechargeClient} from "@/request/ClientApi";
import {reqRecordOne} from "@/request/RecordApi";
import {Prescription} from "@/model/entiy/MedicalRecord";
import {reqHospitalSetting} from "@/request/SettingApi";
import {HospitalSetting} from "@/model/entiy/HospitalSetting";
import RecordDetailFull from "@/components/record/RecordDetailFull.vue";
import {useRoute} from "vue-router";
import {ArrowDown} from "@element-plus/icons-vue";
import {ElMessage, ElMessageBox} from "element-plus";

const route = useRoute()

/**
 ┌───────────────────────────────────┐
 │=============生命周期相关============│
 └───────────────────────────────────┘
 */
onMounted(() => {
  // 从接诊结算步「去收银台」带入待收费筛选
  const ps = route.query.payStatus
  if (ps != null && ps !== '') qp.payStatus = Number(ps)
  // 从诊疗流卡片带入患者：直接预填搜索词，不用再手输一次宠物名
  const sn = route.query.seaName
  if (sn != null && sn !== '') qp.seaName = String(sn)
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
  pageSize: 20
})
// 列表展示
const billList = ref<Page<BillDto>>({records: [], total: 0})
// 待收笔数（当前筛选下）
const unpaidCount = computed(() => billList.value.records.filter(b => !b.payStatus).length)

// 导出账单（当前筛选结果）
const exportBills = (): void => exportCsv('收费账单', [
  {label: '账单号', prop: 'billId'},
  {label: '客户', prop: 'clientName'},
  {label: '宠物', prop: 'petName'},
  {label: '合计(元)', prop: 'totalMoney'},
  {label: '状态', prop: 'payStatus'},
  {label: '支付方式', prop: 'payType'},
  {label: '接诊时间', prop: 'createTime'},
  {label: '收款时间', prop: 'payTime'}
], billList.value.records.map(b => ({
  ...b,
  payStatus: !b.payStatus ? '待收费' : (b.refundStatus ? '已退' : '已收费'),
  payType: b.payType ? '现金' : '余额'
})))

// 折后应付（旧账单无 realMoney 时按原价）
const realMoneyOf = (row: BillDto): number => row.realMoney ?? row.totalMoney
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
  let tip = `确认以【${payName}】方式收取 ${row.clientName}（${row.petName}）的 ￥${realMoneyOf(row)} 吗？`
  if (!cash) tip += `（当前余额 ￥${row.clientBalance ?? 0}；收款后请到「发药核对」确认出库）`
  else tip += '（收款后请到「发药核对」确认出库）'
  ElMessageBox.confirm(tip, '收款确认', {
    confirmButtonText: '确认收款',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    reqBillPay(row.billId, cash).then(res => {
      if (res.code === 1) {
        sendQuery()
        ElMessage.success('收款成功')
        // 收款成功自动弹出小票打印
        printTicket(row, cash, true)
      } else {
        ElMessage.error(res.msg || '收款失败')
      }
    })
  }).catch(() => ElMessage.info('取消收款'))
}

/**
 ┌───────────────────────────────────┐
 │=============充值相关============│
 └───────────────────────────────────┘
 */
/**
 ┌───────────────────────────────────┐
 │=============发药核对（逐项勾选）============│
 └───────────────────────────────────┘
 */
const dispenseView = ref(false)
const dispenseRow = ref<BillDto | null>(null)
const dispenseRx = ref<Prescription[]>([])
const dispenseChecks = ref<boolean[]>([])
// 打开发药核对：拉该账单处方明细，逐项勾选核对（发药人取当前登录人，后端留痕）
const dispenseBill = (row: BillDto): void => {
  dispenseRow.value = row
  dispenseRx.value = []
  dispenseChecks.value = []
  dispenseView.value = true
  if (row.recordId) {
    reqRecordOne(row.recordId).then(res => {
      dispenseRx.value = res?.prescriptions || []
      dispenseChecks.value = dispenseRx.value.map(() => false)
    })
  }
}
const checkedCount = computed(() => dispenseChecks.value.filter(Boolean).length)
// 无处方明细时允许直接核对发放（纯诊疗/检验费用账单）
const allChecked = computed(() => dispenseRx.value.length === 0 || dispenseChecks.value.every(Boolean))
const submitDispense = (): void => {
  if (!dispenseRow.value) return
  if (!allChecked.value) { ElMessage.warning('请逐项核对全部药品后再确认发放'); return }
  // 发药人取当前登录人（后端留痕，不再前端填写）
  reqBillDispense(dispenseRow.value.billId).then(res => {
    if (res.code === 1) {
      dispenseView.value = false
      sendQuery()
    } else ElMessage.error(res.msg || '操作失败')
  })
}

/**
 ┌───────────────────────────────────┐
 │=============退款冲正==============│
 └───────────────────────────────────┘
 */
// 退款弹窗：可选处方行部分退（不勾选=整单退，兼容旧行为）；应退金额实时计算
const refundView = ref(false)
const refundRow = ref<BillDto | null>(null)
const refundRx = ref<Prescription[]>([])
const refundChecks = ref<number[]>([])
const refundReason = ref('')
const refundSaving = ref(false)
const refundBill = (row: BillDto): void => {
  refundRow.value = row
  refundReason.value = ''
  refundChecks.value = []
  refundRx.value = []
  refundView.value = true
  // 拉该单未退处方行（后端 selectByRecord 已过滤 is_cancel；无处方行的单走整单退）
  reqRecordOne(row.recordId || 0).then(res => {
    refundRx.value = res?.prescriptions || []
    // 默认全选（多数场景是整单退；要部分退就取消勾选）
    selectAllRefund()
  }).catch(() => ElMessage.error('拉取处方明细失败'))
}
// 全选/全不选（模板调用，prescriptionId 非空过滤）
const selectAllRefund = (): void => {
  refundChecks.value = refundRx.value
      .map(p => p.prescriptionId)
      .filter((id): id is number => id != null)
}
// 应退金额 = 勾选行 单价×数量
const refundFee = (): number =>
    refundRx.value.filter(p => p.prescriptionId != null && refundChecks.value.includes(p.prescriptionId))
        .reduce((s, p) => s + (p.drugPrice ?? 0) * (p.quantity ?? 0), 0)
// 是否整单退（无处方行，或勾选了全部行）
const refundIsFull = (): boolean =>
    refundRx.value.length === 0 || refundChecks.value.length === refundRx.value.length
const submitRefund = (): void => {
  if (!refundRow.value) return
  if (!refundReason.value.trim()) { ElMessage.warning('请填写退款原因'); return }
  if (refundRx.value.length > 0 && refundChecks.value.length === 0) {
    ElMessage.warning('请至少勾选一行要退的药品（不勾选任何行请直接关闭，改用整单退需全选）')
    return
  }
  refundSaving.value = true
  // 整单退传空数组（走后端兼容路径）；部分退传勾选行 id
  const ids = refundIsFull() ? [] : refundChecks.value
  reqBillRefund(refundRow.value.billId, refundReason.value.trim(), ids).then(res => {
    refundSaving.value = false
    if (res.code === 1) {
      ElMessage.success(res.msg || '退款成功')
      refundView.value = false
      sendQuery()
    } else ElMessage.error(res.msg || '退款失败')
  }).catch(() => { refundSaving.value = false })
}
/** 收款方式选择：余额/现金 */
const payView = ref(false)
const payRow = ref<BillDto | null>(null)
const payChoice = (row: BillDto): void => {
  payRow.value = row
  payView.value = true
}
/** 更多操作分发 */
const onMore = (cmd: string, row: BillDto): void => {
  if (cmd === 'ticket') printTicket(row)
  else if (cmd === 'invoice') printInvoice(row)
  else if (cmd === 'recharge') recharge(row)
  else if (cmd === 'refund') refundBill(row)
  else if (cmd === 'record') showRecord(row)
}

const recharge = (row: BillDto): void => {
  const lack = row.totalMoney - (row.clientBalance ?? 0)
  const tip = lack > 0
      ? `待收 ￥${row.totalMoney}，余额 ￥${row.clientBalance ?? 0}，还差 ￥${lack.toFixed(2)}，输入充值金额：`
      : `客户【${row.clientName}】当前余额 ￥${row.clientBalance ?? 0}，输入充值金额：`
  ElMessageBox.prompt(tip, '充值余额', {
    confirmButtonText: '充值',
    cancelButtonText: '取消',
    inputPattern: /^([1-9]\d{0,4})(\.\d{1,2})?$/,
    inputErrorMessage: '请输入 0.01~99999 之间的金额'
  }).then(({value}) => {
    reqRechargeClient(row.clientId, Number(value)).then(res => {
      if (res.code === 1) sendQuery()
    })
  }).catch(() => ElMessage.info('取消充值'))
}

/**
 ┌───────────────────────────────────┐
 │=============病历查看============│
 └───────────────────────────────────┘
 */
const recordView = ref(false)
const recordId = ref(0)
const showRecord = (row: BillDto): void => {
  if (!row.recordId) {
    ElMessage.info('该账单没有关联病历')
    return
  }
  recordId.value = row.recordId
  recordView.value = true
}

/**
 ┌───────────────────────────────────┐
 │=============打印小票============│
 └───────────────────────────────────┘
 * 打印 80mm 收据小票，用独立窗口打印，不影响主页面样式
 * @param row 账单行
 * @param justPaidType undefined=重打已结算；false/true=刚用余额/现金收款，取当前时间
 */
const printTicket = async (row: BillDto, justPaidType?: boolean, justPaid = false): Promise<void> => {
  // 刚收款时没有处方（recordId 在），重打时也已结算，统一尝试取处方明细
  // 打印前拉医院信息（小票抬头，保证最新配置）
  const hosp = await reqHospitalSetting().catch(() => null)
  reqRecordOne(row.recordId || 0).then(res => {
    if (justPaid) {
      // 刚收款：payStatus 用 true，payType 用刚才的方式，payTime 用当前时刻
      row = {...row, payStatus: true, payType: justPaidType!, payTime: new Date().toLocaleString('zh-CN')}
    }
    openReportPrint('ticket', row, res?.prescriptions ?? [], hosp)
  }).catch(() => openReportPrint('ticket', row, [], hosp))
}

/**
 * 收费凭证统一交给 Vue 打印页渲染（y-billPrint.vue）。
 * 数据一次性经 localStorage 传递、读完即删；不再用字符串模板拼整页 HTML
 * ——那样既无法复用统一报告样式，还要往新窗口注入脚本触发打印。
 */
const openReportPrint = (type: 'ticket' | 'receipt', row: BillDto,
                         prescriptions: Prescription[], hosp: HospitalSetting | null): void => {
  const key = `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  const storageKey = 'mixpet:print:' + key
  try {
    localStorage.setItem(storageKey, JSON.stringify({row, prescriptions, hosp}))
  } catch {
    ElMessage.error('打印数据准备失败，请重试')
    return
  }
  const url = '/#/backstage/y-billPrint?type=' + type + '&key=' + key
  if (!window.open(url, '_blank')) {
    localStorage.removeItem(storageKey)
    ElMessage.warning('浏览器拦截了弹窗，请允许本站弹出窗口')
  }
}

/** A4 收费凭证（院内收据，非税务发票），仅对已结算账单可用 */
const printInvoice = async (row: BillDto): Promise<void> => {
  if (!row.payStatus) {
    ElMessage.warning('该账单尚未结算，请先收款')
    return
  }
  // 打印前拉医院信息（发票抬头/销售方/电话地址，保证最新配置）
  const hosp = await reqHospitalSetting().catch(() => null)
  reqRecordOne(row.recordId || 0).then(res => {
    openReportPrint('receipt', row, res?.prescriptions ?? [], hosp)
  }).catch(() => openReportPrint('receipt', row, [], hosp))
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
// 账单不可删除：保留回调以兼容通用操作栏组件，明确引导走退款冲正
const delBatchB = (): void => {
  ElMessage.warning('账单不允许删除；如需冲正请使用「退款」流程')
}
</script>

<style lang="scss" scoped>
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

/* 收款方式选择 */
.pay-choice {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.pay-card {
  border: 2px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 14px;
  cursor: pointer;
  transition: border-color .2s, background .2s;
  h4 { margin: 0 0 6px; font-size: 15px; color: #1F2D3D; }
  p { margin: 0; font-size: 12px; color: #909399; line-height: 1.6; }
  &:hover { border-color: var(--brand); background: #f0f7fc; }
}

/* 退款冲正弹窗 */
.refund-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px 12px;
  margin-bottom: 10px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
  color: #1F2D3D;
}
.refund-rx {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;

  .refund-rx-head {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12px;
    color: #606266;
    margin-bottom: 6px;
  }

  .refund-checks { display: flex; flex-direction: column; gap: 2px; }

  .refund-check {
    margin: 0;
    width: 100%;
    font-size: 13px;

    .refund-fee { color: #E6A23C; font-size: 12px; margin-left: 8px; }
  }

  .refund-total {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px dashed #ebeef5;
    font-size: 14px;
    font-weight: 700;

    b { font-size: 17px; }
  }

  .refund-tip {
    margin-top: 6px;
    font-size: 12px;
    color: #909399;
    line-height: 1.6;
  }
}
</style>