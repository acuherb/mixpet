<template>
  <el-card class="filter-card" shadow="never">
    <el-input v-model="qp.keyword" clearable placeholder="编码 / 名称" size="large" style="width: 240px" @input="debQuery" @keyup.enter="nowQuery"/>
    <span class="tip">诊断编码字典：接诊向导下拉选码，病历落库 code+体系 快照；停用不物理删除，历史病历编码可溯源</span>
  </el-card>

  <el-table :data="list.records" max-height="calc(100vh - 200px)" stripe>
    <el-table-column label="编码" width="140" prop="code"/>
    <el-table-column label="诊断名称" min-width="160" prop="name" show-overflow-tooltip/>
    <el-table-column label="体系" width="120" prop="codeSystem">
      <template #default="scope"><el-tag size="small">{{ scope.row.codeSystem || 'INTERNAL' }}</el-tag></template>
    </el-table-column>
    <el-table-column label="分类" width="120" prop="category"/>
    <el-table-column label="物种" width="80">
      <template #default="scope"><span v-if="scope.row.species">{{ scope.row.species }}</span><span v-else>通用</span></template>
    </el-table-column>
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

  <el-dialog v-model="modalView" :title="modalTit" draggable width="520px" :close-on-click-modal="false">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="编码" prop="code">
        <el-input v-model="form.code" maxlength="64" placeholder="如 ICD11_VET 或 院内码" :disabled="!!form.dictId"/>
      </el-form-item>
      <el-form-item label="诊断名称" prop="name">
        <el-input v-model="form.name" maxlength="128" placeholder="如：慢性肾病"/>
      </el-form-item>
      <el-form-item label="编码体系">
        <el-select v-model="form.codeSystem" clearable placeholder="INTERNAL" style="width: 100%">
          <el-option label="INTERNAL（院内码）" value="INTERNAL"/>
          <el-option label="ICD11_VET" value="ICD11_VET"/>
          <el-option label="SNOMED_CT_VET" value="SNOMED_CT_VET"/>
        </el-select>
      </el-form-item>
      <el-form-item label="分类">
        <el-input v-model="form.category" maxlength="64" placeholder="如：泌尿内科"/>
      </el-form-item>
      <el-form-item label="适用物种">
        <el-select v-model="form.species" clearable placeholder="通用" style="width: 100%">
          <el-option label="通用" value=""/>
          <el-option label="犬" value="犬"/>
          <el-option label="猫" value="猫"/>
        </el-select>
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
import {reqAddDiagnosis, reqDelDiagnosis, reqDiagnosisPage, reqUpdateDiagnosis, type DiagnosisDict} from "@/request/DiagnosisApi";
import {Res} from "@/request/Res";

const qp = reactive<{ numPage: number; pageSize: number; keyword: string }>({numPage: 1, pageSize: 20, keyword: ''})
const list = ref<Page<DiagnosisDict>>({records: [], total: 0})

let timer: number | undefined
const debQuery = (): void => { window.clearTimeout(timer); timer = window.setTimeout(nowQuery, 350) }

const sendQuery = (): void => { reqDiagnosisPage({...qp}).then(res => { list.value = res }) }
const changePuB = (val: { numPage: number; pageSize: number }): void => {
  qp.numPage = val.numPage; qp.pageSize = val.pageSize; sendQuery()
}
const nowQuery = (): void => { qp.numPage = 1; sendQuery() }

const modalView = ref(false)
const modalTit = ref('新增诊断编码')
const myFormRef = ref<FormInstance>()
const form = ref<DiagnosisDict>(blank())
function blank(): DiagnosisDict { return {code: '', name: '', codeSystem: 'INTERNAL', category: '', species: null, active: true} }
const rules = reactive<FormRules>({
  code: [{required: true, message: '编码必填', trigger: 'blur'}],
  name: [{required: true, message: '名称必填', trigger: 'blur'}]
})
const showDialog = (row?: DiagnosisDict): void => {
  modalTit.value = row?.dictId ? '编辑诊断编码' : '新增诊断编码'
  form.value = row?.dictId ? {...row} : blank()
  modalView.value = true
}
const formSubmit = (): void => {
  myFormRef.value?.validate((valid: boolean): void => {
    if (!valid) return
    const api = form.value.dictId ? reqUpdateDiagnosis(form.value) : reqAddDiagnosis(form.value)
    api.then((res: Res) => {
      if (res.code === 1) { ElMessage.success(res.msg || '已保存'); modalView.value = false; sendQuery() }
      else ElMessage.error(res.msg || '保存失败')
    })
  })
}
const delOne = (row: DiagnosisDict): void => {
  ElMessageBox.confirm(`确认停用「${row.name}」？历史病历编码仍可溯源。`, '停用确认', {
    confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqDelDiagnosis(row.dictId!).then((res: Res) => {
      if (res.code === 1) { ElMessage.success('已停用'); sendQuery() } else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => {})
}

onMounted(sendQuery)
</script>

<style scoped>
.filter-card { margin-bottom: 8px; }
.tip { margin-left: 16px; font-size: 12px; color: #909399; }
</style>
