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
        <el-form-item label="检验套组">
          <div class="panel-area">
            <el-checkbox-group v-model="form.panelIds">
              <el-checkbox v-for="p in panels" :key="p.panel.testId" :label="p.panel.testId" border>
                {{ p.panel.testName }}<span class="price">￥{{ p.panel.panelPrice ?? '-' }}</span>
              </el-checkbox>
            </el-checkbox-group>
            <div v-for="p in panels" v-show="form.panelIds.includes(p.panel.testId)" :key="'m' + p.panel.testId" class="panel-members">
              <span class="pm-tit">含 {{ p.members.length }} 项：</span>
              <span v-for="m in p.members" :key="m.testId" class="m-chip">{{ m.testName }}</span>
            </div>
            <p v-if="!panels.length" class="panel-tip">暂无套组，可在「系统设置 / 检验项字典」配置生化30项等成套项目</p>
          </div>
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
import {useRoute} from 'vue-router'
import {RecordDto} from "@/model/DO/RecordDto";
import {LabTest} from "@/model/entiy/LabTest";
import {LabOrderDto} from "@/model/DO/LabOrderDto";
import {reqCreateLabOrder, reqDoctorRecords, reqLabByRecord, reqLabPanels, reqLabTestList} from "@/request/LabApi";
import {LabPanelVo} from "@/model/VO/LabPanel";
import {ElMessage} from "element-plus";

/**
 ┌───────────────────────────────────┐
 │=============初始化与数据============│
 └───────────────────────────────────┘
 */
const records = ref<RecordDto[]>([])
const tests = ref<LabTest[]>([])
const panels = ref<LabPanelVo[]>([])
const orders = ref<LabOrderDto[]>([])
const loading = ref(false)

const form = reactive<{ recordId: number | null; testIds: number[]; panelIds: number[]; orderNote: string }>({
  recordId: null,
  testIds: [],
  panelIds: [],
  orderNote: ''
})

const recordLabel = (r: RecordDto): string =>
    `#${r.recordId} ${r.petName || ''}（${r.clientName || ''}）`

// 单项（非套组）按分组聚合
const singleTests = computed(() => tests.value.filter(t => !t.isPanel))
const byGroup = computed(() => {
  const map: Record<string, LabTest[]> = {}
  singleTests.value.forEach(t => {
    const g = t.testGroup || '其他'
    if (!map[g]) map[g] = []
    map[g].push(t)
  })
  return map
})
const groupKeys = computed(() => Object.keys(byGroup.value))

// 合计 = 独立单项单价之和 + 套组开单价之和（套组只收一次费）
const totalFee = computed(() => {
  let s = form.testIds.reduce((sum, id) => {
    const t = singleTests.value.find(x => x.testId === id)
    return sum + (t?.testPrice ?? 0)
  }, 0)
  s += form.panelIds.reduce((sum, id) => {
    const p = panels.value.find(x => x.panel.testId === id)
    return sum + (p?.panel.panelPrice ?? 0)
  }, 0)
  return s
})

/**
 ┌───────────────────────────────────┐
 │=============开单相关============───│
 └───────────────────────────────────┘
 */
const submitOrder = (): void => {
  if (!form.recordId) { ElMessage.warning('请先选择病历'); return }
  if (form.testIds.length === 0 && form.panelIds.length === 0) {
    ElMessage.warning('请至少勾选一项检验或套组'); return
  }
  loading.value = true
  reqCreateLabOrder({
    recordId: form.recordId,
    orderNote: form.orderNote,
    testIds: form.testIds,
    panelIds: form.panelIds
  }).then(res => {
    if (res.code === 1) {
      ElMessage.success(res.msg || '开单成功')
      form.orderNote = ''
      form.testIds = []
      form.panelIds = []
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

const route = useRoute()
onMounted(() => {
  reqDoctorRecords().then(res => {
    records.value = res || []
  })
  reqLabTestList().then(res => {
    tests.value = res || []
  })
  reqLabPanels().then(res => {
    panels.value = res || []
  })
  // 从就诊汇总跳转过来：预填本次病历
  const rid = Number(route.query.recordId)
  if (rid) {
    form.recordId = rid
    loadOrders()
  }
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
.panel-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.panel-area .el-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.panel-members {
  padding: 6px 10px;
  background: #f5f9ff;
  border: 1px dashed #cfe0ff;
  border-radius: 8px;
  font-size: 12px;
  color: #5a6b85;
}
.pm-tit {
  color: #2f5597;
  font-weight: 600;
  margin-right: 4px;
}
.m-chip {
  display: inline-block;
  margin: 2px 6px 2px 0;
  padding: 1px 8px;
  background: #eaf2ff;
  border-radius: 10px;
  color: #3a6ea5;
}
.panel-tip {
  font-size: 12px;
  color: #909399;
  margin: 0;
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