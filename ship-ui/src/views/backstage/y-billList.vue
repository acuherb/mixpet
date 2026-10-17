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
    <el-table-column label="余额(元)" prop="clientBalance" width="90"/>
    <el-table-column label="时间" min-width="140" prop="createTime" show-overflow-tooltip>
      <template #default="scope">
        {{ scope.row.payStatus ? scope.row.payTime : scope.row.createTime }}
      </template>
    </el-table-column>
    <el-table-column fixed="right" label="操作" width="220">
      <template #default="scope">
        <el-button-group>
          <el-button v-if="!scope.row.payStatus"
                     circle title="余额收款（扣客户余额并发药）" type="primary" @click="payBill(scope.row, false)">余
          </el-button>
          <el-button v-if="!scope.row.payStatus"
                     circle title="现金收款（只发药）" type="success" @click="payBill(scope.row, true)">现
          </el-button>
          <el-button v-if="!scope.row.payStatus"
                     circle title="充值余额（余额不足时先充值）" type="warning" @click="recharge(scope.row)">充
          </el-button>
          <el-button v-if="scope.row.payStatus"
                     circle title="打印小票（80mm收据，重打）" type="success" @click="printTicket(scope.row)">票
          </el-button>
          <el-button v-if="scope.row.payStatus"
                     circle title="打印正规发票（A4）" type="warning" @click="printInvoice(scope.row)">发
          </el-button>
          <el-button v-if="scope.row.payStatus && !scope.row.dispenseStatus"
                     circle title="发药核对（确认已将药品发给客户）" type="danger" @click="dispenseBill(scope.row)">药
          </el-button>
          <el-button v-if="scope.row.payStatus && !scope.row.refundStatus"
                     circle size="small" title="退款冲正（退还余额/回滚库存）" type="warning" @click="refundBill(scope.row)">退
          </el-button>
          <el-button circle title="查看病历" type="info" @click="showRecord(scope.row)">历</el-button>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="billList.total" @changePu="changePuB"/>
  <!--病历详情弹窗（完整病例档案）-->
  <el-dialog v-model="recordView" title="病历详情" top="6vh" width="90%" style="max-width: 900px">
    <RecordDetailFull v-if="recordId" :record-id="recordId"/>
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
  let tip = `确认以【${payName}】方式收取 ${row.clientName}（${row.petName}）的 ￥${row.totalMoney} 吗？`
  if (!cash) tip += `（当前余额 ￥${row.clientBalance ?? 0}，收款后自动发药扣库存）`
  else tip += '（收款后自动发药扣库存）'
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
 │=============发药核对============│
 └───────────────────────────────────┘
 */
const dispenseBill = (row: BillDto): void => {
  ElMessageBox.prompt('发药核对：请输入核对人姓名', '发药核对', {
    confirmButtonText: '确认发药',
    cancelButtonText: '取消',
    inputPattern: /^.+$/,
    inputErrorMessage: '请输入核对人姓名'
  }).then(({value}) => {
    reqBillDispense(row.billId, value).then(res => {
      if (res.code === 1) {
        sendQuery()
        ElMessage.success('发药核对完成')
      } else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => ElMessage.info('取消发药'))
}

/**
 ┌───────────────────────────────────┐
 │=============退款冲正==============│
 └───────────────────────────────────┘
 */
const refundBill = (row: BillDto): void => {
  ElMessageBox.confirm(
      `确认对该账单退款冲正？将${row.payType ? '退还现金（请线下退）' : '退还余额'}并回滚已扣库存。`,
      '退款确认',
      {confirmButtonText: '确认退款', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    ElMessageBox.prompt('输入退款操作人姓名', '退款', {
      confirmButtonText: '确认', cancelButtonText: '取消',
      inputPattern: /^.+$/, inputErrorMessage: '请输入操作人姓名'
    }).then(({value}) => {
      reqBillRefund(row.billId, value).then(res => {
        if (res.code === 1) {
          sendQuery()
          ElMessage.success('退款成功')
        } else ElMessage.error(res.msg || '退款失败')
      })
    }).catch(() => ElMessage.info('取消退款'))
  }).catch(() => ElMessage.info('取消退款'))
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
    buildPrintWindow(row, res?.prescriptions ?? [], hosp)
  }).catch(() => buildPrintWindow(row, [], hosp))
}

const money = (v: number | null | undefined): string => (v ?? 0).toFixed(2)

const buildPrintWindow = (row: BillDto, prescriptions: Prescription[], hosp: HospitalSetting | null): void => {
  // 支付方式文案
  const payTypeText = row.payType ? '现金' : '余额'
  const payTime = row.payTime || row.createTime || ''
  // 处方明细行
  const drugRows = prescriptions.map(p => `
    <tr>
      <td>${p.drugName || ''}${p.drugSpec ? ' ' + p.drugSpec : ''}</td>
      <td class="r">${p.quantity}${p.drugUnit || ''}</td>
      <td class="r">${money(p.drugPrice)}</td>
    </tr>`).join('')
  const drugSection = prescriptions.length > 0 ? `
    <div class="sec">
      <div class="t">处方明细</div>
      <table>
        <tr><th class="l">药品</th><th>数量</th><th class="r">单价</th></tr>
        ${drugRows}
      </table>
    </div>` : ''
  // 备注：诊断/医嘱（可选）
  const html = `<!DOCTYPE html>
<html lang="zh">
<head><meta charset="UTF-8"><title>收银小票</title>
<style>
  body{font-family: Consolas, monospace; width: 72mm; margin: 8mm auto; color:#000;
       font-size: 12px; line-height: 1.5;}
  h1{font-size: 16px; text-align:center; margin:0 0 2px;}
  .sub{text-align:center; font-size:11px; color:#333; margin-bottom:8px;}
  .line{border:none; border-top:1px dashed #000; margin:8px 0;}
  .row{display:flex; justify-content:space-between;}
  .row .k{color:#333;}
  table{width:100%; border-collapse:collapse; font-size:12px;}
  th,td{text-align:left; padding:2px 0;}
  th{font-weight:bold;}
  .r{text-align:right;}
  .sec{margin-top:4px;}
  .t{font-weight:bold; margin:4px 0;}
  .total{font-size:14px; font-weight:bold;}
  .center{text-align:center; margin-top:10px; font-size:11px;}
  @media print {body{margin:0; width:72mm;}}
</style></head>
<body>
  <h1>${hosp?.name || '宠物医院管理系统'}</h1>
  <div class="sub">收费小票${hosp?.phone ? ' · ' + hosp.phone : ''}</div>
  <hr class="line">
  <div class="row"><span class="k">账单号</span><span>${row.billId}</span></div>
  <div class="row"><span class="k">客户</span><span>${row.clientName || ''}${row.clientTel ? ' (' + row.clientTel + ')' : ''}</span></div>
  <div class="row"><span class="k">宠物</span><span>${row.petName || ''}</span></div>
  <div class="row"><span class="k">医生</span><span>${row.doctorName || ''}</span></div>
  <hr class="line">
  <div class="row"><span>挂号费</span><span>￥${money(row.feeRegister)}</span></div>
  <div class="row"><span>诊疗费</span><span>￥${money(row.feeTreat)}</span></div>
  <div class="row"><span>药费</span><span>￥${money(row.feeDrug)}</span></div>
  <div class="row"><span>化验费</span><span>￥${money(row.feeLab)}</span></div>
  <div class="row"><span>影像费</span><span>￥${money(row.feeImage)}</span></div>
  <div class="row"><span>住院费</span><span>￥${money(row.feeHospital)}</span></div>
  <div class="row"><span>套餐费</span><span>￥${money(row.feePackage)}</span></div>
  <div class="row"><span>手术费</span><span>￥${money(row.feeSurgery)}</span></div>
  ${drugSection}
  <hr class="line">
  <div class="row total"><span>合计</span><span>￥${money(row.totalMoney)}</span></div>
  <hr class="line">
  <div class="row"><span class="k">支付方式</span><span>${payTypeText}</span></div>
  <div class="row"><span class="k">支付时间</span><span>${payTime}</span></div>
  <div class="center">谢谢惠顾，祝宠物早日康复！<br>（本小票仅作收据凭证）</div>
<script>window.onload = function(){ window.print(); }<\/script>
</body></html>`
  const w = window.open('', '_blank', 'width=380,height=640')
  if (w) {
    w.document.write(html)
    w.document.close()
  } else {
    ElMessage.warning('浏览器拦截了弹窗，请允许本站弹出窗口')
  }
}

/** 在独立窗口打印 HTML */
const openPrintWindow = (_title: string, html: string): void => {
  const w = window.open('', '_blank', 'width=820,height=960')
  if (w) {
    w.document.write(html)
    w.document.close()
  } else {
    ElMessage.warning('浏览器拦截了弹窗，请允许本站弹出窗口')
  }
}

/** 数字金额转中文大写（元角分） */
const moneyCN = (n: number | null | undefined): string => {
  let num = Math.round((Number(n) || 0) * 100)
  if (num === 0) return '零元整'
  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const unit = ['', '拾', '佰', '仟']
  const bigUnit = ['', '万', '亿', '兆']
  const yi = Math.floor(num / 100); const fen = num % 100
  let str = ''
  if (yi > 0) {
    let yiStr = ''; let lastZero = false
    const yiStrRaw = String(yi)
    for (let i = 0; i < yiStrRaw.length; i++) {
      const d = Number(yiStrRaw[i])
      const pos = yiStrRaw.length - 1 - i
      if (d === 0) {
        if (!lastZero && yiStr) yiStr += '零'
        lastZero = true
      } else {
        yiStr += digits[d] + unit[pos % 4] + (pos >= 4 ? bigUnit[Math.floor(pos / 4)] : '')
        lastZero = false
      }
    }
    str += yiStr + '元'
  }
  const jiao = Math.floor(fen / 10) % 10
  const f = fen % 10
  if (jiao > 0) str += digits[jiao] + '角'
  else if (yi > 0 && f > 0) str += '零'
  if (f > 0) str += digits[f] + '分'
  if (jiao === 0 && f === 0) str += '整'
  return str
}

/**
 ┌───────────────────────────────────┐
 │=============打印正式发票============│
 └───────────────────────────────────┘
 * A4 增值税普通发票版式（870 主要内容打印，非税务联网）
 * 仅对已结算账单可用
 */
const printInvoice = async (row: BillDto): Promise<void> => {
  if (!row.payStatus) {
    ElMessage.warning('该账单尚未结算，请先收款')
    return
  }
  // 打印前拉医院信息（发票抬头/销售方/电话地址，保证最新配置）
  const hosp = await reqHospitalSetting().catch(() => null)
  reqRecordOne(row.recordId || 0).then(res => {
    buildInvoiceWindow(row, res?.prescriptions ?? [], hosp)
  }).catch(() => buildInvoiceWindow(row, [], hosp))
}

const buildInvoiceWindow = (row: BillDto, prescriptions: Prescription[], hosp: HospitalSetting | null): void => {
  const payTypeText = row.payType ? '现金' : '余额'
  const payTime = row.payTime || row.createTime || ''
  // 明细行：挂号/诊疗/药费 + 处方药品逐项
  const rows: Array<[string, string, string, number]> = [
    ['门诊挂号服务', '次', '1', row.feeRegister || 0],
    ['诊疗处置服务', '次', '1', row.feeTreat || 0]
  ]
  if (row.feeDrug > 0) {
    if (prescriptions.length > 0) {
      prescriptions.forEach(p => rows.push(['药品·' + (p.drugName || ''), p.drugUnit || '盒', String(p.quantity), (p.drugPrice || 0) * p.quantity]))
    } else {
      rows.push(['药品费', '项', '1', row.feeDrug])
    }
  }
  if (row.feeLab > 0) {
    rows.push(['化验检查费', '项', '1', row.feeLab])
  }
  if (row.feeImage > 0) {
    rows.push(['影像检查费', '项', '1', row.feeImage])
  }
  if (row.feeHospital > 0) {
    rows.push(['住院费', '天', '1', row.feeHospital])
  }
  if (row.feePackage > 0) {
    rows.push(['套餐费', '项', '1', row.feePackage])
  }
  if (row.feeSurgery > 0) {
    rows.push(['手术费', '项', '1', row.feeSurgery])
  }
  const totalMoney = row.totalMoney || 0
  const trs = rows.map(r => `
    <tr>
      <td class="l">${r[0]}</td>
      <td class="c">${r[2]}</td>
      <td class="c">${r[1]}</td>
      <td class="r">★</td>
      <td class="r">${money(r[3])}</td>
      <td class="r">****</td>
      <td class="r">${money(r[3])}</td>
      <td class="r">0.00</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html>
<html lang="zh">
<head><meta charset="UTF-8"><title>发票</title>
<style>
  body{font-family: SimSun, serif; width: 210mm; min-height: 297mm; margin: 0 auto; padding: 12mm 10mm; color:#000; box-sizing:border-box;}
  .head{display:flex; justify-content:space-between; align-items:flex-start; border-bottom:2px solid #000; padding-bottom:6px;}
  .head h1{font-size:26px; margin:0; letter-spacing:2px;}
  .no{text-align:right; font-size:13px; line-height:1.8;}
  .t2{display:flex; justify-content:space-between; font-size:13px; margin:8px 0;}
  table{width:100%; border-collapse:collapse; font-size:12px; margin-top:8px;}
  th,td{border:1px solid #000; padding:4px 5px; text-align:center;}
  th{background:#f2f2f2; font-weight:bold;}
  .l{text-align:left;} .r{text-align:right;} .c{text-align:center;}
  .total-row{font-weight:bold; font-size:13px;}
  .bottom{margin-top:10px; font-size:12px;}
  .sign{display:flex; justify-content:space-between; margin-top:6px; font-size:12px;}
  .foot{margin-top:16px; font-size:11px; color:#333; text-align:center; border-top:1px solid #999; padding-top:6px;}
  @media print{body{margin:0; padding:10mm;}}
</style></head>
<body>
  <div class="head">
    <h1>${hosp?.name || '宠物医院管理系统'}</h1>
    <div class="no">发票代码：21300000000${String(row.billId).padStart(5, '0')}<br>发票号码：${String(row.billId).padStart(8, '0')}<br>开票日期：${payTime}</div>
  </div>
  <div class="t2">
    <span>购买方名称：${row.clientName || ''}</span>
    <span>密码区：******************</span>
  </div>
  <div class="t2">
    <span>购买方电话：${row.clientTel || ''}</span>
    <span>销售方名称：${hosp?.name || '宠物医院管理系统'}</span>
  </div>
  <table>
    <tr><th class="l" style="width:30%">货物或应税劳务名称</th><th style="width:8%">数量</th><th style="width:8%">单位</th><th style="width:10%">单价</th><th style="width:13%">金额</th><th style="width:10%">税率</th><th style="width:13%">税额</th><th style="width:13%">价税合计</th></tr>
    ${trs}
    <tr class="total-row"><td class="l" colspan="4">合计（大写）：${moneyCN(totalMoney)}</td><td class="r">${money(totalMoney)}</td><td>★</td><td>0.00</td><td class="r">${money(totalMoney)}</td></tr>
  </table>
  <div class="bottom">
    <p>价税合计（大写）：${moneyCN(totalMoney)}　（小写）：￥${money(totalMoney)}</p>
    <p>备注：本单为宠物诊疗服务费。宠物：${row.petName || ''}，医生：${row.doctorName || ''}，支付方式：${payTypeText}。</p>
  </div>
  <div class="sign">
    <span>收款人：________</span>
    <span>复核：________</span>
    <span>开票人：________</span>
    <span>销售方(盖章)：</span>
  </div>
  <div class="foot">${hosp?.phone ? '电话：' + hosp.phone + '　' : ''}${hosp?.address || ''}</div>
  <div class="foot">本发票样式为系统内置格式，不联网，仅供内部记账/报销凭证使用</div>
<script>window.onload = function(){ window.print(); }<\/script>
</body></html>`
  openPrintWindow('发票_' + row.billId, html)
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