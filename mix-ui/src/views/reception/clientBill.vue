<template>
  <div class="bill-self">
    <div class="bs-head">
      <h2>我的账单</h2>
      <p>本院就诊产生的费用明细；收款、退款与发药仍由前台办理，如有疑问可在线咨询。</p>
    </div>
    <div class="bs-summary">
      <div class="bs-stat"><span>账单总数</span><b>{{ bills.length }}</b></div>
      <div class="bs-stat"><span>待缴费</span><b class="warn">{{ unpaid.length }}</b></div>
      <div class="bs-stat"><span>累计实付</span><b>￥{{ paidSum.toFixed(2) }}</b></div>
      <div class="bs-stat"><span>累计退款</span><b>￥{{ refundSum.toFixed(2) }}</b></div>
    </div>
    <el-table :data="bills" v-loading="loading" border stripe size="small" empty-text="暂无账单">
      <el-table-column type="expand" width="42">
        <template #default="s">
          <div class="fee-grid">
            <div v-for="f in feesOf(s.row)" :key="f.label" v-show="f.value > 0">
              <span>{{ f.label }}</span><b>￥{{ f.value.toFixed(2) }}</b>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="账单号" prop="billId" width="90"/>
      <el-table-column label="就诊号" prop="recordId" width="90"/>
      <el-table-column label="账单金额" width="120">
        <template #default="s">￥{{ (s.row.totalMoney ?? 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="折后实付" width="120">
        <template #default="s">￥{{ (s.row.realMoney ?? s.row.totalMoney ?? 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="退款" width="100">
        <template #default="s">￥{{ (s.row.refundMoney ?? 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="s">
          <el-tag v-if="s.row.refundStatus" size="small" type="info">已退款</el-tag>
          <el-tag v-else-if="s.row.payStatus" size="small" type="success">已缴费</el-tag>
          <el-tag v-else size="small" type="warning">待缴费</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发药" width="90">
        <template #default="s">
          <el-tag v-if="s.row.dispenseStatus" size="small" type="success">已发药</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="时间" min-width="150">
        <template #default="s">{{ (s.row.payTime || s.row.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
    </el-table>
    <p class="bs-tip">点击左侧箭头展开，可查看挂号、诊疗、药品、化验、影像、住院、套餐、手术等分项金额。</p>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {reqClientBill} from "@/request/BillApi";
import {Bill} from "@/model/entiy/Bill";

const bills = ref<Bill[]>([])
const loading = ref(true)
const unpaid = computed(() => bills.value.filter(b => !b.payStatus))
const paidSum = computed(() => bills.value
    .filter(b => b.payStatus)
    .reduce((s, b) => s + (b.realMoney ?? b.totalMoney ?? 0) - (b.refundMoney ?? 0), 0))
const refundSum = computed(() => bills.value.reduce((s, b) => s + (b.refundMoney ?? 0), 0))
const feesOf = (b: Bill): { label: string; value: number }[] => [
  {label: '挂号费', value: b.feeRegister ?? 0},
  {label: '诊疗费', value: b.feeTreat ?? 0},
  {label: '药品费', value: b.feeDrug ?? 0},
  {label: '检验费', value: b.feeLab ?? 0},
  {label: '影像费', value: b.feeImage ?? 0},
  {label: '住院费', value: b.feeHospital ?? 0},
  {label: '套餐费', value: b.feePackage ?? 0},
  {label: '手术费', value: b.feeSurgery ?? 0}
]
onMounted(() => {
  reqClientBill().then(list => { bills.value = list || [] })
      .catch(() => ElMessage.error('账单加载失败，请稍后重试'))
      .finally(() => { loading.value = false })
})
</script>

<style lang="scss" scoped>
.bill-self { padding: 4px 0; }
.bs-head {
  h2 { margin: 0 0 6px; font-size: 20px; color: #253858; }
  p { margin: 0 0 18px; font-size: 13px; color: #8a96a8; }
}
.bs-summary { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 18px; }
.bs-stat {
  flex: 1; min-width: 140px; padding: 12px 16px; border-radius: 10px; background: #f5f8fc;
  display: flex; flex-direction: column; gap: 6px;
  span { font-size: 12px; color: #8a96a8; }
  b { font-size: 18px; color: #253858; }
  b.warn { color: #e6a23c; }
}
.fee-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 8px 20px;
  padding: 10px 16px;
  div { display: flex; justify-content: space-between; font-size: 12px; color: #5b6b82; }
  b { color: #253858; }
}
.bs-tip { margin: 12px 0 0; font-size: 12px; color: #98a3b3; }
</style>
