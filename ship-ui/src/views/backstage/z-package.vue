<template>
  <!--操作框-->
  <BackOpCol role="套餐" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.packageName" clearable placeholder="套餐名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-select v-model="qp.category" clearable placeholder="全部分类" size="large" style="width: 120px" @change="nowQuery">
      <el-option v-for="c in packageCategories" :key="c" :label="c" :value="c"/>
    </el-select>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">已选 <b>{{ roleIdList.length }}</b> 项</span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="list.records"
            max-height="calc(100vh - 130px)"
            row-key="packageId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="套餐名" min-width="150" prop="packageName" show-overflow-tooltip/>
    <el-table-column label="分类" width="90">
      <template #default="scope">
        <el-tag size="small">{{ scope.row.category || '未分类' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="价格(元)" prop="price" sortable width="100"/>
    <el-table-column label="包含内容" min-width="160" prop="content" show-overflow-tooltip/>
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
      <el-form-item label="套餐名" prop="packageName">
        <el-input v-model="form.packageName" clearable maxlength="64" placeholder="如：基础体检套餐"/>
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="form.category" allow-create filterable placeholder="选择或输入分类">
          <el-option v-for="c in packageCategories" :key="c" :label="c" :value="c"/>
        </el-select>
      </el-form-item>
      <el-form-item label="价格(元)" prop="price">
        <el-input-number v-model="form.price" :max="99999" :min="0" :precision="2" :step="10" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="包含内容">
        <el-input v-model="form.content" :rows="3" maxlength="255" type="textarea" placeholder="如：体格检查+血常规+驱虫"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.active" active-text="启用" inactive-text="停用"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit">确认</el-button>
      </span>
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
import {examplePackage, packageCategories, ServicePackage} from "@/model/entiy/ServicePackage";
import {Page} from "@/model/DO/Page";
import {reqAddPackage, reqDelPackage, reqPackagePage, reqUpdatePackage} from "@/request/PackageApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

onMounted(() => {
  sendQuery()
})

const qp = reactive({packageName: '', category: '', numPage: 1, pageSize: 15})
const list = ref<Page<ServicePackage>>({records: [], total: 0})

const changePuB = (val: PageQuery): void => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  reqPackagePage(qp).then(res => {
    list.value = res
  })
}
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}
const debQuery = debounce(nowQuery, 400)

const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: ServicePackage[]): void => {
  roleIdList.value = val.map(obj => obj.packageId)
}
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm('确定删除选中的套餐吗？历史就诊记录不受影响。', '删除多个确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    Promise.all(roleIdList.value.map(id => reqDelPackage(id))).then(() => sendQuery())
  }).catch(() => ElMessage.info('删除取消'))
}
const delOne = (row: ServicePackage): void => {
  ElMessageBox.confirm(`确定删除【${row.packageName}】吗？历史就诊记录不受影响。`, '删除确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDelPackage(row.packageId).then(res => { if (res.code === 1) sendQuery() })
  }).catch(() => ElMessage.info('删除取消'))
}

const modalView = ref(false)
const modalTit = ref<"新增套餐" | "修改套餐">("修改套餐")
const addRoleB = (): void => {
  form.value = examplePackage()
  myFormRef.value?.resetFields()
  modalTit.value = "新增套餐"
  modalView.value = true
}
const showDialog = (row: ServicePackage): void => {
  myFormRef.value?.clearValidate()
  form.value = {...row}
  modalView.value = true
  modalTit.value = "修改套餐"
}

const form = ref<ServicePackage>(examplePackage())
const myFormRef = ref<FormInstance>()
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      modalView.value = false
      if (modalTit.value == "新增套餐") reqAddPackage(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改套餐") reqUpdatePackage(form.value).then(res => sureFlush(res))
      else ElMessageBox.alert('模块框出错')
    }
  })
}
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}
const rules = reactive<FormRules>({
  "packageName": [
    {required: true, message: '请输入套餐名', trigger: 'blur'},
  ],
  "price": [
    {required: true, message: '请输入价格', trigger: 'blur'},
  ]
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
