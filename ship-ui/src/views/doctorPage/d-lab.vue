<template>
  <div class="lab-wrap">
    <el-card shadow="never">
      <template #header>
        <b>检验开单</b>
      </template>
      <el-form :model="form" label-width="86px" @submit.prevent>
        <el-form-item label="选择病历" prop="recordId">
          <el-select v-model="form.recordId" filterable placeholder="按宠物/客户选择本次就诊病历" style="width: 100%" @change="loadOrders">
            <el-option v-for="r in records" :key="r.recordId" :label="recordLabel(r)" :value="r.recordId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="检验项目" prop="testIds">
          <el-checkbox-group v-model="form.testIds">
            <template v-for="g in groupKeys" :key="g">
              <div class="group-row">
                <span class="group-name">{{ g }}</span>
                <el-checkbox v-for="t in byGroup[g]" :key="t.testId" :label="t.testId" border>
                  {{ t.testName }}<span class="price">￥{{ t.testPrice ?? '-' }}</span>
                </el-checkbox>
              </div>
            </template>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="检验目的">
          <el-input v-model="form.orderNote" :rows="2" maxlength="255" placeholder="填写检验目的/临床备注，可留空" type="textarea"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="submitOrder">
            确认开单并计入账单（合计 ￥{{ totalFee }}）
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <b>已开化验单</b>
      </template>
      <el-table :data="orders" row-key="orderId" stripe>
        <el-table-column label="单号" prop="orderId" width="70"/>
        <el-table-column label="宠物" prop="petName" width="110"/>
        <el-table-column label="客户" prop="clientName" width="120"/>
        <el-table-column label="费用(元)" prop="fee" width="100"/>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.status" size="small" type="success">已出结果</el-tag>
            <el-tag v-else size="small" type="warning">待检验</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="录入人" prop="resultBy" width="100"/>
        <el-table-column label="开单时间" prop="createTime" width="160"/>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button size="small" :type="scope.row.status ? 'info' : 'primary'" plain @click="viewOrder(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <!--查看化验单结果弹窗-->
  <el-dialog v-model="dialogView" title="化验单明细" width="640px">
    <div class="order-head">单号 #{{ current?.orderId }} · 宠物：{{ current?.petName }} · 客户：{{ current?.clientName }}</div>
    <el-table :data="current?.items || []" size="small" border>
      <el-table-column label="检验项" min-width="160" prop="itemName"/>
      <el-table-column label="结果值" width="120">
        <template #default="scope">
          <span v-if="scope.row.resultValue">{{ scope.row.resultValue }}</span>
          <el-tag v-else size="small" type="info">未录入</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="参考范围" width="110" prop="refRange"/>
      <el-table-column label="单位" width="70" prop="itemUnit"/>
      <el-table-column label="异常" width="70">
        <template #default="scope">
          <el-tag v-if="scope.row.isAbnormal" size="small" type="danger">异常</el-tag>
          <el-tag v-else-if="scope.row.resultValue" size="small" type="success">正常</el-tag>
          <span v-else>/</span>
        </template>
      </el-table-column>
    </el-table>
    <p v-if="current?.resultBy" class="order-note">
      小结：{{ current?.resultNote || '无' }}（录入人：{{ current?.resultBy }}）
    </p>
    <template #footer>
      <el-button @click="dialogView = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {RecordDto} from "@/model/DO/RecordDto";
import {LabTest} from "@/model/entiy/LabTest";
import {LabOrderDto} from "@/model/DO/LabOrderDto";
import {reqCreateLabOrder, reqDoctorRecords, reqLabByRecord, reqLabTestList} from "@/request/LabApi";
import {ElMessage} from "element-plus";

/**
 ┌───────────────────────────────────┐
 │=============初始化与数据============│
 └───────────────────────────────────┘
 */
const records = ref<RecordDto[]>([])
const tests = ref<LabTest[]>([])
const orders = ref<LabOrderDto[]>([])
const loading = ref(false)

const form = reactive<{ recordId: number | null; testIds: number[]; orderNote: string }>({
  recordId: null,
  testIds: [],
  orderNote: ''
})

const recordLabel = (r: RecordDto): string =>
    `#${r.recordId} ${r.petName || ''}（${r.clientName || ''}）`

// 检验项按分组聚合
const byGroup = computed(() => {
  const map: Record<string, LabTest[]> = {}
  tests.value.forEach(t => {
    const g = t.testGroup || '其他'
    if (!map[g]) map[g] = []
    map[g].push(t)
  })
  return map
})
const groupKeys = computed(() => Object.keys(byGroup.value))

const totalFee = computed(() =>
    form.testIds.reduce((sum, id) => {
      const t = tests.value.find(x => x.testId === id)
      return sum + (t?.testPrice ?? 0)
    }, 0)
)

/**
 ┌───────────────────────────────────┐
 │=============开单相关============───│
 └───────────────────────────────────┘
 */
const submitOrder = (): void => {
  if (!form.recordId) { ElMessage.warning('请先选择病历'); return }
  if (form.testIds.length === 0) { ElMessage.warning('请至少勾选一项检验'); return }
  loading.value = true
  reqCreateLabOrder({
    recordId: form.recordId,
    orderNote: form.orderNote,
    testIds: form.testIds
  }).then(res => {
    if (res.code === 1) {
      ElMessage.success(res.msg || '开单成功')
      form.orderNote = ''
      form.testIds = []
      loadOrders()
    }
  }).finally(() => {
    loading.value = false
  })
}

/**
 ┌───────────────────────────────────┐
 │=============查看结果相关===========│
 └───────────────────────────────────┘
 */
const dialogView = ref(false)
const current = ref<LabOrderDto | null>(null)
const viewOrder = (row: LabOrderDto): void => {
  if (row.items && row.items.length) {
    current.value = row
    dialogView.value = true
    return
  }
  reqLabByRecord(row.recordId).then(list => {
    const m = list.find(o => o.orderId === row.orderId)
    if (m) row.items = m.items
    current.value = row
    dialogView.value = true
  })
}

const loadOrders = (): void => {
  if (!form.recordId) return
  reqLabByRecord(form.recordId).then(list => {
    orders.value = list
  })
}

onMounted(() => {
  reqDoctorRecords().then(res => {
    records.value = res || []
  })
  reqLabTestList().then(res => {
    tests.value = res || []
  })
})
</script>

<style scoped>
.lab-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.group-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 4px 0;
}
.group-name {
  width: 90px;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}
.price {
  color: #e6a23c;
  margin-left: 4px;
}
.order-head {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.order-note {
  font-size: 13px;
  color: #606266;
  margin: 10px 0 0;
}
</style>