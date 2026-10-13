<template>
  <!--操作框-->
  <BackOpCol role="检验项" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.testName" clearable placeholder="检验项名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-select v-model="qp.testGroup" clearable placeholder="全部分组" size="large" style="width: 110px" @change="nowQuery">
      <el-option v-for="t in labTestGroups" :key="t" :label="t" :value="t"/>
    </el-select>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">
        已选 <b>{{ roleIdList.length }}</b> 项
      </span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="list.records"
            max-height="calc(100vh - 130px)"
            row-key="testId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="检验项" min-width="150" prop="testName" show-overflow-tooltip/>
    <el-table-column label="分组" width="90">
      <template #default="scope">
        <el-tag size="small">{{ scope.row.testGroup || '未分组' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="单位" width="90" prop="testUnit" show-overflow-tooltip/>
    <el-table-column label="参考范围" min-width="120" prop="refRange" show-overflow-tooltip/>
    <el-table-column label="单价(元)" prop="testPrice" sortable width="100"/>
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
          <el-button :icon="Delete" circle title="删除" type="danger" @click="delOne(scope.row)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="500px">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="检验项" prop="testName">
        <el-input v-model="form.testName" clearable maxlength="64" placeholder="如：白细胞计数"/>
      </el-form-item>
      <el-form-item label="分组" prop="testGroup">
        <el-select v-model="form.testGroup" allow-create filterable placeholder="选择或输入分组">
          <el-option v-for="t in labTestGroups" :key="t" :label="t" :value="t"/>
        </el-select>
      </el-form-item>
      <el-form-item label="单位" prop="testUnit">
        <el-input v-model="form.testUnit" clearable maxlength="32" placeholder="如：10^9/L"/>
      </el-form-item>
      <el-form-item label="参考范围" prop="refRange">
        <el-input v-model="form.refRange" clearable maxlength="64" placeholder="如：6.0-17.0"/>
      </el-form-item>
      <el-form-item label="单价(元)" prop="testPrice">
        <el-input-number v-model="form.testPrice" :max="99999" :min="0" :precision="2" :step="1" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.active" active-text="启用" inactive-text="停用"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="modalView = false">取消</el-button>
      <el-button type="primary" @click="formSubmit">确认</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {Delete, Edit} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {exampleLabTest, labTestGroups, LabTest} from "@/model/entiy/LabTest";
import {Page} from "@/model/DO/Page";
import {reqAddLabTest, reqDelLabTest, reqLabTestPage, reqUpdateLabTest} from "@/request/LabApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

/**
 ┌───────────────────────────────────┐
 │=============表格查询相关============│
 └───────────────────────────────────┘
 */
const qp = reactive({
  testName: '',
  testGroup: '',
  numPage: 1,
  pageSize: 15
})
const list = ref<Page<LabTest>>({records: [], total: 0})
const changePuB = (val: PageQuery): void => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  reqLabTestPage(qp).then(res => {
    list.value = res
  })
}
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}
const debQuery = debounce(nowQuery, 400)

/**
 ┌───────────────────────────────────┐
 │=============数据删除相关============│
 └───────────────────────────────────┘
 */
const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: LabTest[]): void => {
  roleIdList.value = val.map(obj => obj.testId)
}
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm(
      '确定删除选中的检验项吗？已出化验单保留历史快照不受影响。',
      '删除多个确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    batchDel(roleIdList.value)
  }).catch(() => ElMessage.info('删除取消'))
}
const delOne = (row: LabTest): void => {
  ElMessageBox.confirm(
      row.testId !== undefined && row.testId > 0
          ? '确定删除该检验项吗？已出化验单保留历史快照不受影响。'
          : '该项已被历史化验单引用，将改为停用。',
      '删除确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDelLabTest(row.testId).then(res => sureFlush(res))
  }).catch(() => ElMessage.info('删除取消'))
}
const batchDel = (ids: number[]): void => {
  Promise.all(ids.map(id => reqDelLabTest(id))).then(() => sendQuery())
}
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}

/**
 ┌───────────────────────────────────┐
 │=============新增修改按钮============│
 └───────────────────────────────────┘
 */
const modalView = ref(false)
const modalTit = ref<"新增检验项" | "修改检验项">("修改检验项")
const addRoleB = (): void => {
  form.value = exampleLabTest()
  myFormRef.value?.resetFields()
  modalTit.value = "新增检验项"
  modalView.value = true
}
const showDialog = (row: LabTest): void => {
  myFormRef.value?.clearValidate()
  form.value = {...row}
  modalView.value = true
  modalTit.value = "修改检验项"
}

/**
 ┌───────────────────────────────────┐
 │=============表单校验相关========────│
 └───────────────────────────────────┘
 */
const form = ref<LabTest>(exampleLabTest())
const myFormRef = ref<FormInstance>()
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      modalView.value = false
      if (modalTit.value == "新增检验项") reqAddLabTest(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改检验项") reqUpdateLabTest(form.value).then(res => sureFlush(res))
    }
  })
}
const rules = reactive<FormRules>({
  "testName": [
    {required: true, message: '请输入检验项名', trigger: 'blur'},
  ]
})

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
.sel-tip {
  margin-right: 12px;
  font-size: 14px;
  color: #409eff;
}
.sel-tip b {
  font-size: 16px;
  margin: 0 2px;
}
</style>