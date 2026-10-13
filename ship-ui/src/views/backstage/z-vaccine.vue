<template>
  <!--操作框-->
  <BackOpCol role="疫苗" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.vaccineName" clearable placeholder="疫苗名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">
        已选 <b>{{ roleIdList.length }}</b> 项
      </span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="list.records"
            max-height="calc(100vh - 130px)"
            row-key="vaccineId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="疫苗名称" min-width="150" prop="vaccineName" show-overflow-tooltip/>
    <el-table-column label="品牌" min-width="120" prop="brand" show-overflow-tooltip/>
    <el-table-column label="预防疾病" min-width="160" prop="prevent" show-overflow-tooltip/>
    <el-table-column label="间隔(天)" prop="intervalDays" width="90"/>
    <el-table-column label="参考价(元)" prop="price" width="100"/>
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
      <el-form-item label="疫苗名称" prop="vaccineName">
        <el-input v-model="form.vaccineName" clearable maxlength="64" placeholder="如：狂犬疫苗"/>
      </el-form-item>
      <el-form-item label="品牌">
        <el-input v-model="form.brand" clearable maxlength="64" placeholder="品牌/厂家"/>
      </el-form-item>
      <el-form-item label="预防疾病">
        <el-input v-model="form.prevent" clearable maxlength="128" placeholder="如：狂犬病"/>
      </el-form-item>
      <el-form-item label="接种间隔(天)" prop="intervalDays">
        <el-input-number v-model="form.intervalDays" :max="3650" :min="1" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="参考价(元)">
        <el-input-number v-model="form.price" :max="99999" :min="0" :precision="2" style="width: 100%"/>
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
import {exampleVaccine, Vaccine} from "@/model/entiy/Vaccine";
import {Page} from "@/model/DO/Page";
import {reqAddVaccine, reqDelVaccine, reqUpdateVaccine, reqVaccinePage} from "@/request/ImmuneApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

const qp = reactive({vaccineName: '', numPage: 1, pageSize: 15})
const list = ref<Page<Vaccine>>({records: [], total: 0})
const changePuB = (val: PageQuery): void => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  reqVaccinePage(qp).then(res => {
    list.value = res
  })
}
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}
const debQuery = debounce(nowQuery, 400)

const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: Vaccine[]): void => {
  roleIdList.value = val.map(obj => obj.vaccineId)
}
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm('确定删除选中的疫苗吗？已登记档案保留历史快照不受影响。', '删除多个确认', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(() => Promise.all(roleIdList.value.map(id => reqDelVaccine(id))).then(() => sendQuery()))
      .catch(() => ElMessage.info('删除取消'))
}
const delOne = (row: Vaccine): void => {
  ElMessageBox.confirm('确定删除该疫苗吗？已登记档案保留历史快照不受影响。', '删除确认', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(() => reqDelVaccine(row.vaccineId).then(res => sureFlush(res))).catch(() => ElMessage.info('删除取消'))
}
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}

const modalView = ref(false)
const modalTit = ref<"新增疫苗" | "修改疫苗">("修改疫苗")
const addRoleB = (): void => {
  form.value = exampleVaccine()
  myFormRef.value?.resetFields()
  modalTit.value = "新增疫苗"
  modalView.value = true
}
const showDialog = (row: Vaccine): void => {
  myFormRef.value?.clearValidate()
  form.value = {...row}
  modalView.value = true
  modalTit.value = "修改疫苗"
}

const form = ref<Vaccine>(exampleVaccine())
const myFormRef = ref<FormInstance>()
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      modalView.value = false
      if (modalTit.value == "新增疫苗") reqAddVaccine(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改疫苗") reqUpdateVaccine(form.value).then(res => sureFlush(res))
    }
  })
}
const rules = reactive<FormRules>({
  "vaccineName": [{required: true, message: '请输入疫苗名称', trigger: 'blur'}],
  "intervalDays": [{required: true, message: '请输入间隔天数', trigger: 'blur'}]
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