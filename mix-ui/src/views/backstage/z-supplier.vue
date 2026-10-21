<template>
  <div>
    <BackOpCol role="供应商" @addRole="addRoleB" @query="nowQuery">
      <el-input v-model="qp.seaName" clearable placeholder="供应商 / 联系人" size="large" style="width: 200px"
                @input="debQuery" @keyup.enter="nowQuery"/>
    </BackOpCol>
    <el-table :data="list.records" max-height="calc(100vh - 200px)" row-key="supplierId" stripe>
      <el-table-column label="供应商" min-width="160" prop="supplierName" show-overflow-tooltip/>
      <el-table-column label="联系人" min-width="100" prop="contact"/>
      <el-table-column label="电话" min-width="120" prop="phone"/>
      <el-table-column label="地址" min-width="180" prop="address" show-overflow-tooltip/>
      <el-table-column label="备注" min-width="140" prop="note" show-overflow-tooltip/>
      <el-table-column fixed="right" label="操作" width="140">
        <template #default="scope">
          <el-button-group>
            <el-button :icon="Edit" circle title="编辑" type="warning" @click="showDialog(scope.row)"/>
            <el-button :icon="Delete" circle title="删除" type="danger" @click="delOne(scope.row.supplierId)"/>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>
    <BackPage :total="list.total" @changePu="changePuB"/>

    <el-dialog v-model="modalView" :title="modalTit" draggable width="500px">
      <el-form ref="myFormRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="supplierName">
          <el-input v-model="form.supplierName" clearable maxlength="128" placeholder="供应商/经销商名称"/>
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" clearable maxlength="64"/>
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" clearable maxlength="32"/>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" clearable maxlength="255"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" :rows="2" maxlength="255" type="textarea"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {Delete, Edit} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {Page} from "@/model/DO/Page";
import {exampleSupplier, Supplier} from "@/model/entiy/Supplier";
import {reqAddSupplier, reqDelSupplier, reqSupplierPage, reqUpdateSupplier} from "@/request/SupplierApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

const qp = reactive({seaName: '', numPage: 1, pageSize: 15})
const list = ref<Page<Supplier>>({records: [], total: 0})
const changePuB = (val: PageQuery): void => { qp.numPage = val.numPage; qp.pageSize = val.pageSize; sendQuery() }
const sendQuery = (): void => { reqSupplierPage(qp).then(res => { list.value = res }) }
const nowQuery = (): void => { qp.numPage = 1; sendQuery() }
const debQuery = debounce(nowQuery, 400)

const modalView = ref(false)
const modalTit = ref('新增供应商')
const myFormRef = ref<FormInstance>()
const form = ref<Supplier>(exampleSupplier())
const rules = reactive<FormRules>({
  supplierName: [{required: true, message: '请输入供应商名称', trigger: 'blur'}]
})
const addRoleB = (): void => { form.value = exampleSupplier(); myFormRef.value?.resetFields(); modalTit.value = '新增供应商'; modalView.value = true }
const showDialog = (row: Supplier): void => { form.value = {...row}; myFormRef.value?.resetFields(); modalTit.value = '编辑供应商'; modalView.value = true }
const formSubmit = (): void => {
  myFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    const isEdit = form.value.supplierId != null
    const api = isEdit ? reqUpdateSupplier : reqAddSupplier
    api(form.value).then((res: Res) => {
      if (res.code === 1) { ElMessage.success(res.msg || '保存成功'); modalView.value = false; sendQuery() }
      else ElMessage.error(res.msg || '保存失败')
    })
  })
}
const delOne = (id: number): void => {
  ElMessageBox.confirm('确定删除该供应商吗？', '删除确认', {type: 'warning'})
      .then(() => reqDelSupplier(id).then((res: Res) => { if (res.code === 1) { ElMessage.success('已删除'); sendQuery() } }))
      .catch(() => ElMessage.info('已取消'))
}

onMounted(() => { sendQuery() })
</script>
