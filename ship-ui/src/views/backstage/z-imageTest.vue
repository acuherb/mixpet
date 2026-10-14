<template>
  <!--操作框-->
  <BackOpCol role="影像项" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.itemName" clearable placeholder="项目名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-select v-model="qp.groupName" clearable placeholder="全部分组" size="large" style="width: 110px" @change="nowQuery">
      <el-option v-for="g in imgGroupOpts" :key="g.dictId" :label="g.dictKey" :value="g.dictKey"/>
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
            row-key="itemId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="项目名" min-width="150" prop="itemName" show-overflow-tooltip/>
    <el-table-column label="分组" width="90">
      <template #default="scope">
        <el-tag size="small">{{ scope.row.groupName || '未分组' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="单价(元)" prop="price" sortable width="100"/>
    <el-table-column label="说明" min-width="140" prop="detail" show-overflow-tooltip/>
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
  <!--新增/修改弹窗-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="500px">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="项目名" prop="itemName">
        <el-input v-model="form.itemName" clearable maxlength="64" placeholder="如：胸部X光片"/>
      </el-form-item>
      <el-form-item label="分组" prop="groupName">
        <el-select v-model="form.groupName" filterable placeholder="选择分组（在系统设置维护）">
          <el-option v-for="g in imgGroupOpts" :key="g.dictId" :label="g.dictKey" :value="g.dictKey"/>
        </el-select>
      </el-form-item>
      <el-form-item label="单价(元)" prop="price">
        <el-input-number v-model="form.price" :max="99999" :min="0" :precision="2" :step="10" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="form.detail" clearable maxlength="255" type="textarea" :rows="2" placeholder="检查范围/注意事项"/>
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
import {exampleImageTest, ImageTest} from "@/model/entiy/ImageTest";
import {reqDictByType} from "@/request/DictApi";
import {SysDict} from "@/model/entiy/SysDict";
import {Page} from "@/model/DO/Page";
import {reqImageTestPage, reqUpdateImageTest, reqAddImageTest, reqDelImageTest} from "@/request/ImageApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

// 影像分组字典（来自系统设置）
const imgGroupOpts = ref<SysDict[]>([])
reqDictByType('img_group').then(r => { imgGroupOpts.value = r || [] })

const qp = reactive({itemName: '', groupName: '', numPage: 1, pageSize: 15})
const list = ref<Page<ImageTest>>({records: [], total: 0})
const changePuB = (val: PageQuery): void => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  reqImageTestPage(qp).then(res => {
    list.value = res
  })
}
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}
const debQuery = debounce(nowQuery, 400)

const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: ImageTest[]): void => {
  roleIdList.value = val.map(obj => obj.itemId)
}
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm('确定删除选中的影像项吗？已开申请单保留历史快照不受影响。', '删除多个确认', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(() => Promise.all(roleIdList.value.map(id => reqDelImageTest(id))).then(() => sendQuery()))
      .catch(() => ElMessage.info('删除取消'))
}
const delOne = (row: ImageTest): void => {
  ElMessageBox.confirm('确定删除该影像项吗？已开申请单保留历史快照不受影响。', '删除确认', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(() => reqDelImageTest(row.itemId).then(res => sureFlush(res))).catch(() => ElMessage.info('删除取消'))
}
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}

const modalView = ref(false)
const modalTit = ref<"新增影像项" | "修改影像项">("修改影像项")
const addRoleB = (): void => {
  form.value = exampleImageTest()
  myFormRef.value?.resetFields()
  modalTit.value = "新增影像项"
  modalView.value = true
}
const showDialog = (row: ImageTest): void => {
  myFormRef.value?.clearValidate()
  form.value = {...row}
  modalView.value = true
  modalTit.value = "修改影像项"
}

const form = ref<ImageTest>(exampleImageTest())
const myFormRef = ref<FormInstance>()
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      modalView.value = false
      if (modalTit.value == "新增影像项") reqAddImageTest(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改影像项") reqUpdateImageTest(form.value).then(res => sureFlush(res))
    }
  })
}
const rules = reactive<FormRules>({
  "itemName": [{required: true, message: '请输入项目名', trigger: 'blur'}]
})

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