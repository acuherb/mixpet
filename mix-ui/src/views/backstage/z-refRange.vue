<template>
  <el-card class="filter-card" shadow="never">
    <el-select v-model="qp.testId" clearable filterable placeholder="选择检验项" size="large" style="width: 260px" @change="nowQuery">
      <el-option v-for="t in testOpts" :key="t.testId" :label="t.testName" :value="t.testId"/>
    </el-select>
    <span class="tip">参考区间与危急阈值随仪器/试剂/物种而异，请由检验科核对后录入；数值区间为开单时快照，历史单据不受影响</span>
  </el-card>

  <el-table :data="list.records" max-height="calc(100vh - 200px)" stripe>
    <el-table-column label="检验项" min-width="150">
      <template #default="scope">{{ testNameOf(scope.row.testId) }}</template>
    </el-table-column>
    <el-table-column label="物种" width="80" prop="species">
      <template #default="scope"><span v-if="scope.row.species">{{ scope.row.species }}</span><span v-else>通用</span></template>
    </el-table-column>
    <el-table-column label="性别" width="70" prop="sex">
      <template #default="scope"><span v-if="scope.row.sex">{{ scope.row.sex }}</span><span v-else>不限</span></template>
    </el-table-column>
    <el-table-column label="月龄区间" width="130">
      <template #default="scope">
        {{ scope.row.ageMinMonth ?? '—' }} ~ {{ scope.row.ageMaxMonth ?? '—' }}
      </template>
    </el-table-column>
    <el-table-column label="参考下限" width="100" prop="refLow"/>
    <el-table-column label="参考上限" width="100" prop="refHigh"/>
    <el-table-column label="危急低" width="100" prop="criticalLow"/>
    <el-table-column label="危急高" width="100" prop="criticalHigh"/>
    <el-table-column label="展示文本" min-width="100" prop="refText" show-overflow-tooltip/>
    <el-table-column label="状态" width="80">
      <template #default="scope">
        <el-tag v-if="scope.row.active" size="small" type="success">启用</el-tag>
        <el-tag v-else size="small" type="info">停用</el-tag>
      </template>
    </el-table-column>
    <el-table-column fixed="right" label="操作" width="120">
      <template #default="scope">
        <el-button-group>
          <el-button :icon="Edit" circle title="编辑" type="warning" @click="showDialog(scope.row)"/>
          <el-button :icon="Delete" circle title="停用" type="danger" @click="delOne(scope.row)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>

  <BackPage :total="list.total" @changePu="changePuB"/>

  <el-dialog v-model="modalView" :title="modalTit" draggable width="560px" :close-on-click-modal="false">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="检验项" prop="testId">
        <el-select v-model="form.testId" filterable placeholder="选择检验项" style="width: 100%">
          <el-option v-for="t in testOpts" :key="t.testId" :label="t.testName" :value="t.testId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="适用物种">
        <el-select v-model="form.species" clearable placeholder="通用" style="width: 100%">
          <el-option label="通用" value=""/>
          <el-option label="犬" value="犬"/>
          <el-option label="猫" value="猫"/>
        </el-select>
      </el-form-item>
      <el-form-item label="性别">
        <el-select v-model="form.sex" clearable placeholder="不限" style="width: 100%">
          <el-option label="不限" value=""/>
          <el-option label="公" value="公"/>
          <el-option label="母" value="母"/>
        </el-select>
      </el-form-item>
      <el-form-item label="月龄(含端点)">
        <el-input-number v-model="form.ageMinMonth" :min="0" :controls="false" placeholder="最小" style="width: 120px"/>
        <span style="margin: 0 8px">至</span>
        <el-input-number v-model="form.ageMaxMonth" :min="0" :controls="false" placeholder="最大" style="width: 120px"/>
      </el-form-item>
      <el-form-item label="参考下限" prop="refLow">
        <el-input-number v-model="form.refLow" :precision="4" :controls="false" style="width: 160px"/>
      </el-form-item>
      <el-form-item label="参考上限" prop="refHigh">
        <el-input-number v-model="form.refHigh" :precision="4" :controls="false" style="width: 160px"/>
      </el-form-item>
      <el-form-item label="危急下限">
        <el-input-number v-model="form.criticalLow" :precision="4" :controls="false" style="width: 160px"/>
      </el-form-item>
      <el-form-item label="危急上限">
        <el-input-number v-model="form.criticalHigh" :precision="4" :controls="false" style="width: 160px"/>
      </el-form-item>
      <el-form-item label="展示文本">
        <el-input v-model="form.refText" maxlength="64" placeholder="如 6.0-17.0（展示用）" style="width: 100%"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button round @click="modalView = false">取消</el-button>
      <el-button round type="primary" @click="formSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from "vue"
import BackPage from "@/components/row/BackPage.vue";
import {Page} from "@/model/DO/Page";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Edit, Delete} from "@element-plus/icons-vue";
import {reqLabTestList} from "@/request/LabApi";
import {LabTest} from "@/model/entiy/LabTest";
import {
  reqAddRefRange, reqDelRefRange, reqRefRangePage, reqUpdateRefRange,
  type LabRefRange
} from "@/request/RefRangeApi";
import {Res} from "@/request/Res";

const qp = reactive<{ numPage: number; pageSize: number; testId?: number }>({numPage: 1, pageSize: 20, testId: undefined})
const list = ref<Page<LabRefRange>>({records: [], total: 0})
const testOpts = ref<LabTest[]>([])

const testNameOf = (id?: number): string => testOpts.value.find(t => t.testId === id)?.testName || ('#' + id)

const sendQuery = (): void => {
  reqRefRangePage({...qp}).then(res => { list.value = res })
}
const changePuB = (val: { numPage: number; pageSize: number }): void => {
  qp.numPage = val.numPage; qp.pageSize = val.pageSize; sendQuery()
}
const nowQuery = (): void => { qp.numPage = 1; sendQuery() }

const modalView = ref(false)
const modalTit = ref('新增参考区间')
const myFormRef = ref<FormInstance>()
const form = ref<LabRefRange>(blank())
function blank(): LabRefRange {
  return {testId: testOpts.value[0]?.testId ?? 0, species: null, sex: null, ageMinMonth: null, ageMaxMonth: null, refLow: null, refHigh: null, criticalLow: null, criticalHigh: null, refText: null, active: true}
}
const rules = reactive<FormRules>({
  testId: [{required: true, message: '请选择检验项', trigger: 'change'}]
})
const showDialog = (row?: LabRefRange): void => {
  modalTit.value = row?.rangeId ? '编辑参考区间' : '新增参考区间'
  form.value = row?.rangeId ? {...row} : blank()
  modalView.value = true
}
const formSubmit = (): void => {
  myFormRef.value?.validate((valid: boolean): void => {
    if (!valid) return
    const api = form.value.rangeId ? reqUpdateRefRange(form.value) : reqAddRefRange(form.value)
    api.then((res: Res) => {
      if (res.code === 1) { ElMessage.success(res.msg || '已保存'); modalView.value = false; sendQuery() }
      else ElMessage.error(res.msg || '保存失败')
    })
  })
}
const delOne = (row: LabRefRange): void => {
  ElMessageBox.confirm(`确认停用该参考区间？历史单据不受影响。`, '停用确认', {
    confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqDelRefRange(row.rangeId!).then((res: Res) => {
      if (res.code === 1) { ElMessage.success('已停用'); sendQuery() } else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => {})
}

onMounted(() => {
  reqLabTestList().then(list => { testOpts.value = list || [] }).finally(sendQuery)
})
</script>

<style scoped>
.filter-card { margin-bottom: 8px; }
.tip { margin-left: 16px; font-size: 12px; color: #909399; }
</style>
