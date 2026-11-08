<template>
  <!--操作框-->
  <BackOpCol role="职务" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.jobName" clearable placeholder="职务/职称名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">
        已选 <b>{{ roleIdList.length }}</b> 项
      </span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="jobList.records"
            max-height="calc(100vh - 130px)"
            row-key="jobId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="职务/职称" prop="jobName"/>
    <el-table-column label="排序" prop="sort" width="80" sortable/>
    <el-table-column :formatter="doctorNumStr" label="人数" prop="doctorNum" width="80" sortable/>
    <el-table-column fixed="right" label="操作" width="120">
      <template #default="scope">
        <el-button-group>
          <el-button :icon="Edit" circle type="warning" @click="showDialog(scope.row)"/>
          <el-button :icon="Delete" circle type="danger" @click="delOne(scope.row)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="jobList.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="480px">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="职务/职称" prop="jobName">
        <el-input v-model="form.jobName" clearable maxlength="32" placeholder="如：主治医师 / 科室主任 / 护士长"/>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="form.sort" :min="0" :max="999" style="width: 100%"/>
      </el-form-item>
      <div class="tip">职务仅作展示，与「权限」无关；权限在医生编辑里单独设置。</div>
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
import {Job, exampleJob} from "@/model/entiy/Job";
import {reqAddJob, reqDelJob, reqDelJobBatch, reqJobPage, reqUpdateJob} from "@/request/JobApi";
import {Page} from "@/model/DO/Page";
import {JobDto} from "@/model/DO/JobDto";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

onMounted(() => {
  sendQuery()
})

const qp = reactive({jobName: '', numPage: 1, pageSize: 100})
const jobList = ref<Page<JobDto>>({records: [], total: 0})

const doctorNumStr = (row: JobDto): string => row.doctorNum > 0 ? row.doctorNum + "人" : "无"

const changePuB = (val: PageQuery): void => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  reqJobPage(qp).then(res => {
    jobList.value = res
  })
}
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}
const debQuery = debounce(nowQuery, 400)

const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: Job[]): void => {
  roleIdList.value = val.map(obj => obj.jobId)
}
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm('确定删除选中的职务/职称吗？名下仍有医生的不可删除。', '删除多个确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDelJobBatch(roleIdList.value).then(res => sureFlush(res))
  }).catch(() => ElMessage.info('删除取消'))
}
const delOne = (row: Job): void => {
  ElMessageBox.confirm(`确定删除【${row.jobName}】吗？名下仍有医生的不可删除。`, '删除确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDelJob(row.jobId).then(res => sureFlush(res))
  }).catch(() => ElMessage.info('删除取消'))
}
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}

const modalView = ref(false)
const modalTit = ref<"新增职务" | "修改职务">("修改职务")
const addRoleB = (): void => {
  form.value = exampleJob()
  myFormRef.value?.resetFields()
  modalTit.value = "新增职务"
  modalView.value = true
}
const showDialog = (row: JobDto): void => {
  myFormRef.value?.clearValidate()
  form.value.jobId = row.jobId
  form.value.jobName = row.jobName
  form.value.sort = row.sort ?? 99
  modalView.value = true
  modalTit.value = "修改职务"
}

const form = ref<Job>(exampleJob())
const myFormRef = ref<FormInstance>()
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      modalView.value = false
      if (modalTit.value == "新增职务") reqAddJob(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改职务") reqUpdateJob(form.value).then(res => sureFlush(res))
      else ElMessageBox.alert('操作未完成，请检查填写内容后重试')
    }
  })
}
const rules = reactive<FormRules>({
  "jobName": [
    {required: true, message: '请输入职务/职称名', trigger: 'blur'},
  ],
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
.tip {
  font-size: 12px;
  color: #909399;
  padding: 0 4px 4px;
}
</style>
