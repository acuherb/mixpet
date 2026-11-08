<template>
  <div>
    <BackOpCol role="病历模板" @addRole="addRoleB" @query="nowQuery">
      <el-input v-model="qp.seaName" clearable placeholder="模板名 / 分类" size="large" style="width: 200px"
                @input="debQuery" @keyup.enter="nowQuery"/>
    </BackOpCol>
    <el-table :data="list.records" max-height="calc(100vh - 200px)" row-key="templateId" stripe>
      <el-table-column label="模板名" min-width="140" prop="name" show-overflow-tooltip/>
      <el-table-column label="分类" width="100" prop="category"/>
      <el-table-column label="诊断" min-width="180" prop="diagnosis" show-overflow-tooltip/>
      <el-table-column label="状态" width="80">
        <template #default="s">
          <el-tag :type="s.row.active ? 'success' : 'info'" size="small">{{ s.row.active ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="140">
        <template #default="s">
          <el-button-group>
            <el-button :icon="Edit" circle title="编辑" type="warning" @click="showDialog(s.row)"/>
            <el-button :icon="Delete" circle title="删除" type="danger" @click="delOne(s.row.templateId)"/>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>
    <BackPage :total="list.total" @changePu="changePuB"/>

    <el-dialog v-model="modalView" :title="modalTit" draggable width="600px">
      <el-form ref="myFormRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="模板名" prop="name">
          <el-input v-model="form.name" clearable maxlength="64" placeholder="如：犬瘟热初诊 / 年度体检"/>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" clearable maxlength="32" placeholder="如：常见病/体检/术后/复诊"/>
        </el-form-item>
        <el-form-item label="主诉">
          <el-input v-model="form.chiefComplaint" :rows="2" maxlength="500" type="textarea" placeholder="主要症状+持续时间，如：呕吐2天，今日次数增加伴食欲下降"/>
        </el-form-item>
        <el-form-item label="现病史">
          <el-input v-model="form.presentIllness" :rows="2" maxlength="500" type="textarea" placeholder="症状发展：开始→变化→当前状态，如：2日前开始间断呕吐，今日加重至3次…"/>
        </el-form-item>
        <el-form-item label="既往病史">
          <el-input v-model="form.pastHistory" :rows="2" maxlength="500" type="textarea" placeholder="过去重要的医疗信息：既往疾病/手术/长期用药/过敏等"/>
        </el-form-item>
        <el-form-item label="体格检查">
          <el-input v-model="form.physicalExam" :rows="2" maxlength="500" type="textarea" placeholder="体温/心率/呼吸/精神/黏膜等检查发现"/>
        </el-form-item>
        <el-form-item label="诊断"><el-input v-model="form.diagnosis" :rows="2" maxlength="500" type="textarea"/></el-form-item>
        <el-form-item label="治疗方案"><el-input v-model="form.treatment" :rows="2" maxlength="500" type="textarea"/></el-form-item>
        <el-form-item label="医嘱"><el-input v-model="form.advice" :rows="2" maxlength="500" type="textarea"/></el-form-item>
        <el-form-item label="内部备注"><el-input v-model="form.internalNote" :rows="2" maxlength="500" type="textarea"/></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.active" active-text="启用" inactive-text="停用"/></el-form-item>
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
import {exampleRecordTemplate, RecordTemplate} from "@/model/entiy/RecordTemplate";
import {reqAddRecordTemplate, reqDelRecordTemplate, reqRecordTemplatePage, reqUpdateRecordTemplate} from "@/request/RecordTemplateApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

const qp = reactive({seaName: '', numPage: 1, pageSize: 20})
const list = ref<Page<RecordTemplate>>({records: [], total: 0})
const changePuB = (val: PageQuery): void => { qp.numPage = val.numPage; qp.pageSize = val.pageSize; sendQuery() }
const sendQuery = (): void => { reqRecordTemplatePage(qp).then(res => { list.value = res }) }
const nowQuery = (): void => { qp.numPage = 1; sendQuery() }
const debQuery = debounce(nowQuery, 400)

const modalView = ref(false)
const modalTit = ref('新增模板')
const myFormRef = ref<FormInstance>()
const form = ref<RecordTemplate>(exampleRecordTemplate())
const rules = reactive<FormRules>({
  name: [{required: true, message: '请输入模板名', trigger: 'blur'}]
})
const addRoleB = (): void => { form.value = exampleRecordTemplate(); myFormRef.value?.resetFields(); modalTit.value = '新增模板'; modalView.value = true }
const showDialog = (row: RecordTemplate): void => { form.value = {...row}; myFormRef.value?.resetFields(); modalTit.value = '编辑模板'; modalView.value = true }
const formSubmit = (): void => {
  myFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    const isEdit = form.value.templateId != null
    const api = isEdit ? reqUpdateRecordTemplate : reqAddRecordTemplate
    api(form.value).then((res: Res) => {
      if (res.code === 1) { ElMessage.success(res.msg || '保存成功'); modalView.value = false; sendQuery() }
      else ElMessage.error(res.msg || '保存失败')
    })
  })
}
const delOne = (id: number): void => {
  ElMessageBox.confirm('确定删除该模板吗？', '删除确认', {type: 'warning'})
      .then(() => reqDelRecordTemplate(id).then((res: Res) => { if (res.code === 1) { ElMessage.success('已删除'); sendQuery() } }))
      .catch(() => ElMessage.info('已取消'))
}

onMounted(() => { sendQuery() })
</script>
