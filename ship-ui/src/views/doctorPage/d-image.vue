<template>
  <div class="img-wrap">
    <el-card shadow="never">
      <template #header>
        <b>影像检查开单</b>
      </template>
      <el-form :model="form" label-width="90px" @submit.prevent>
        <el-form-item label="选择病历" prop="recordId">
          <el-select v-model="form.recordId" filterable placeholder="按宠物/客户选择本次就诊病历" style="width: 100%" @change="loadOrders">
            <el-option v-for="r in records" :key="r.recordId" :label="recordLabel(r)" :value="r.recordId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="检查项目" prop="itemId">
          <el-select v-model="form.itemId" filterable placeholder="选择影像项目" style="width: 100%">
            <template v-for="g in groupKeys" :key="g">
              <el-option-group :label="g">
                <el-option v-for="t in byGroup[g]" :key="t.itemId" :label="`${t.itemName}（￥${t.price ?? '-'}）`" :value="t.itemId"/>
              </el-option-group>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="部位/目的">
          <el-input v-model="form.examPart" maxlength="64" placeholder="如：右前肢X光" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" :rows="2" maxlength="255" placeholder="临床备注，可留空" type="textarea"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="submitOrder">
            确认开单并计入账单（￥{{ selectedFee }}）
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <b>已开影像单</b>
      </template>
      <el-table :data="orders" row-key="orderId" stripe>
        <el-table-column label="单号" prop="orderId" width="70"/>
        <el-table-column label="项目" prop="itemName" min-width="130" show-overflow-tooltip/>
        <el-table-column label="部位/目的" prop="examPart" min-width="110" show-overflow-tooltip/>
        <el-table-column label="费用(元)" prop="fee" width="100"/>
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag v-if="scope.row.status" size="small" type="success">已出报告</el-tag>
            <el-tag v-else size="small" type="warning">待检查</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开单时间" prop="createTime" width="160"/>
        <el-table-column label="操作" width="90">
          <template #default="scope">
            <el-button size="small" :type="scope.row.status ? 'info' : 'primary'" plain @click="viewOrder(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>

  <!--查看影像报告弹窗-->
  <el-dialog v-model="dialogView" title="影像申请单" width="560px">
    <div class="order-head">单号 #{{ current?.orderId }} · {{ current?.itemName }} · 宠物：{{ current?.petName }}</div>
    <div class="report-box">
      <p class="report-tit">报告内容</p>
      <p v-if="current?.result" class="report-text">{{ current?.result }}</p>
      <el-tag v-else size="small" type="info">尚未出报告</el-tag>
    </div>
    <template #footer>
      <el-button @click="dialogView = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {RecordDto} from "@/model/DO/RecordDto";
import {ImageOrder} from "@/model/entiy/ImageOrder";
import {ImageTest} from "@/model/entiy/ImageTest";
import {reqCreateImageOrder, reqImageByRecord, reqImgGroup} from "@/request/ImageApi";
import {reqDoctorRecords} from "@/request/LabApi";
import {ElMessage} from "element-plus";

const records = ref<RecordDto[]>([])
const tests = ref<ImageTest[]>([])
const orders = ref<ImageOrder[]>([])
const loading = ref(false)

const form = reactive<{ recordId: number | null; itemId: number | null; examPart: string; note: string }>({
  recordId: null,
  itemId: null,
  examPart: '',
  note: ''
})

const recordLabel = (r: RecordDto): string =>
    `#${r.recordId} ${r.petName || ''}（${r.clientName || ''}）`

const byGroup = computed(() => {
  const map: Record<string, ImageTest[]> = {}
  tests.value.forEach(t => {
    const g = t.groupName || '其他'
    if (!map[g]) map[g] = []
    map[g].push(t)
  })
  return map
})
const groupKeys = computed(() => Object.keys(byGroup.value))

const selectedFee = computed(() => {
  const t = tests.value.find(x => x.itemId === form.itemId)
  return t?.price ?? 0
})

const submitOrder = (): void => {
  if (!form.recordId) { ElMessage.warning('请先选择病历'); return }
  if (!form.itemId) { ElMessage.warning('请选择影像项目'); return }
  loading.value = true
  reqCreateImageOrder({
    recordId: form.recordId,
    itemId: form.itemId,
    examPart: form.examPart,
    note: form.note
  }).then(res => {
    if (res.code === 1) {
      ElMessage.success(res.msg || '开单成功')
      form.note = ''
      loadOrders()
    }
  }).finally(() => {
    loading.value = false
  })
}

const dialogView = ref(false)
const current = ref<ImageOrder | null>(null)
const viewOrder = (row: ImageOrder): void => {
  current.value = row
  dialogView.value = true
}

const loadOrders = (): void => {
  if (!form.recordId) return
  reqImageByRecord(form.recordId).then(list => {
    orders.value = list || []
  })
}

onMounted(() => {
  reqDoctorRecords().then(res => {
    records.value = res || []
  })
  reqImgGroup().then(res => {
    tests.value = res || []
  })
})
</script>

<style scoped>
.img-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.order-head {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.report-box {
  background: #f6f6f6;
  border-radius: 6px;
  padding: 12px;
}
.report-tit {
  font-size: 13px;
  color: #909399;
  margin: 0 0 6px;
}
.report-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}
</style>