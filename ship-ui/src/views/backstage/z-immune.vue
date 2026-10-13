<template>
  <!--顶部：筛选 + 接种登记-->
  <el-card class="filter-card" shadow="never">
    <el-select v-model="dueSoonFilter" placeholder="全部档案" size="large" style="width: 170px" clearable @change="nowQuery">
      <el-option label="近30天该接种" :value="true"/>
    </el-select>
    <span class="tip">按疫苗建议间隔自动推算下一针时间，临期自动标注</span>
    <el-button type="primary" size="large" style="float: right" @click="addRoleB">接种登记</el-button>
  </el-card>

  <!--列表展示-->
  <el-table :data="list.records"
            max-height="calc(100vh - 200px)"
            row-key="immuneId"
            stripe>
    <el-table-column label="宠物" min-width="110" prop="petName" show-overflow-tooltip/>
    <el-table-column label="客户" min-width="110" prop="clientName" show-overflow-tooltip/>
    <el-table-column label="疫苗" min-width="140" prop="drugName" show-overflow-tooltip/>
    <el-table-column label="针次" prop="doseCnt" width="60"/>
    <el-table-column label="接种时间" width="160" prop="vaccinateTime"/>
    <el-table-column label="下一针建议" width="160">
      <template #default="scope">
        <span :class="{due: isDue(scope.row)}">{{ scope.row.nextTime || '无' }}</span>
      </template>
    </el-table-column>
    <el-table-column label="接种医生" min-width="90" prop="doctorName" show-overflow-tooltip/>
    <el-table-column label="状态" width="100">
      <template #default="scope">
        <el-tag v-if="isDue(scope.row)" size="small" type="danger">待接种</el-tag>
        <el-tag v-else size="small" type="success">正常</el-tag>
      </template>
    </el-table-column>
  </el-table>

  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>

  <!--接种登记弹窗-->
  <el-dialog v-model="modalView" title="接种登记" draggable width="520px">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="选择病历" prop="recordId">
        <el-select v-model="form.recordId" filterable placeholder="选择本次就诊病历（确定宠物）" style="width: 100%">
          <el-option v-for="r in records" :key="r.recordId" :label="recordLabel(r)" :value="r.recordId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="疫苗" prop="drugId">
        <el-select v-model="form.drugId" filterable placeholder="选择疫苗（在药品列表按疫苗分类维护）" style="width: 100%">
          <el-option v-for="v in vaccineDrugs" :key="v.drugId" :label="`${v.drugName}${v.intervalDays ? '（间隔'+v.intervalDays+'天）' : ''}`" :value="v.drugId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="接种时间">
        <el-date-picker v-model="form.vaccinateTime" type="datetime" placeholder="选择时间" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.note" :rows="2" maxlength="255" placeholder="如：首次接种/加强针" type="textarea"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="modalView = false">取消</el-button>
      <el-button type="primary" @click="formSubmit">确认登记</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {RecordDto} from "@/model/DO/RecordDto";
import {Drug} from "@/model/entiy/Drug";
import {ImmuneRecord} from "@/model/entiy/ImmuneRecord";
import {Page} from "@/model/DO/Page";
import {reqDoctorRecords} from "@/request/LabApi";
import {reqDrugVaccineList} from "@/request/DrugApi";
import {reqImmunePage, reqSaveImmune} from "@/request/ImmuneApi";
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

const qp = reactive({numPage: 1, pageSize: 15, dueSoon: false})
const dueSoonFilter = ref<boolean | null>(null)
const list = ref<Page<ImmuneRecord>>({records: [], total: 0})
const vaccineDrugs = ref<Drug[]>([])
const records = ref<RecordDto[]>([])

const isDue = (row: ImmuneRecord): boolean =>
    !!row.nextTime && new Date(row.nextTime) <= new Date(new Date().getTime() + 30 * 86400000)

const changePuB = (val: PageQuery): void => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  const dueSoon = dueSoonFilter.value === true
  reqImmunePage({petName: '', dueSoon, numPage: qp.numPage, pageSize: qp.pageSize}).then(res => {
    list.value = res
  })
}
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}

// 登记弹窗
const modalView = ref(false)
const form = reactive<{ recordId: number | null; drugId: number | null; vaccinateTime: string | Date; note: string }>({
  recordId: null,
  drugId: null,
  vaccinateTime: new Date(),
  note: ''
})
const myFormRef = ref<FormInstance>()

const recordLabel = (r: RecordDto): string =>
    `#${r.recordId} ${r.petName || ''}（${r.clientName || ''}）`

const addRoleB = (): void => {
  form.recordId = null
  form.drugId = null
  form.vaccinateTime = new Date()
  form.note = ''
  myFormRef.value?.resetFields()
  modalView.value = true
}
const formSubmit = (): void => {
  myFormRef.value?.validate((valid: boolean) => {
    if (valid) {
      reqSaveImmune({
        recordId: form.recordId,
        drugId: form.drugId as number,
        vaccinateTime: new Date(form.vaccinateTime).toISOString(),
        note: form.note
      }).then((res: Res) => {
        modalView.value = false
        if (res.code === 1) {
          ElMessage.success(res.msg || '接种登记成功')
          sendQuery()
        }
      })
    }
  })
}
const rules = reactive<FormRules>({
  "recordId": [{required: true, type: 'number', message: '请选择病历', trigger: 'change'}],
  "drugId": [{required: true, type: 'number', message: '请选择疫苗', trigger: 'change'}]
})

onMounted(() => {
  sendQuery()
  reqDrugVaccineList().then(res => { vaccineDrugs.value = res || [] })
  reqDoctorRecords().then(res => { records.value = res || [] })
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 8px;
}
.tip {
  margin-left: 16px;
  font-size: 12px;
  color: #909399;
}
.due {
  color: #f56c6c;
  font-weight: 600;
}
</style>