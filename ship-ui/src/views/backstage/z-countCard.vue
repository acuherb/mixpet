<template>
  <div>
    <BackOpCol role="次卡模板" @addRole="addRoleB"/>
    <el-table :data="list" max-height="calc(100vh - 160px)" row-key="templateId" stripe>
      <el-table-column label="次卡名" min-width="150" prop="name" show-overflow-tooltip/>
      <el-table-column label="适用服务" min-width="120" prop="serviceType" show-overflow-tooltip/>
      <el-table-column label="次数" prop="times" width="70"/>
      <el-table-column label="价格(元)" prop="price" width="100"/>
      <el-table-column label="有效期(天)" prop="validityDays" width="100"/>
      <el-table-column label="状态" width="80">
        <template #default="s">
          <el-tag :type="s.row.active ? 'success' : 'info'" size="small">{{ s.row.active ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="140">
        <template #default="s">
          <el-button :icon="Edit" circle title="编辑" type="warning" @click="showDialog(s.row)"/>
          <el-button :icon="Delete" circle title="删除" type="danger" @click="delOne(s.row.templateId)"/>
        </template>
      </el-table-column>
    </el-table>
    <p class="tip">次卡购买后从余额扣款，持卡可到店核销；有效期自购买日起算。</p>

    <el-dialog v-model="modalView" :title="modalTit" draggable width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="次卡名" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="如：猫咪洗澡次卡10次"/>
        </el-form-item>
        <el-form-item label="适用服务">
          <el-input v-model="form.serviceType" maxlength="64" placeholder="如：洗护/洗澡"/>
        </el-form-item>
        <el-form-item label="次数" prop="times">
          <el-input-number v-model="form.times" :max="999" :min="1"/>
        </el-form-item>
        <el-form-item label="价格(元)">
          <el-input-number v-model="form.price" :max="99999" :min="0" :precision="2" :step="10"/>
        </el-form-item>
        <el-form-item label="有效期(天)">
          <el-input-number v-model="form.validityDays" :max="3650" :min="0" :step="30"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.active" active-text="上架" inactive-text="下架"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="submit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {Delete, Edit} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {CountCardTemplate} from "@/model/entiy/Member";
import {reqCardTemplates, reqDelCardTemplate, reqSaveCardTemplate} from "@/request/MemberApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

const list = ref<CountCardTemplate[]>([])
const load = (): void => { reqCardTemplates().then(r => { list.value = r || [] }) }
const modalView = ref(false)
const modalTit = ref('新增次卡')
const formRef = ref<FormInstance>()
const empty = (): CountCardTemplate => ({name: '', serviceType: '', times: 10, price: 0, validityDays: 365, active: true})
const form = ref<CountCardTemplate>(empty())
const rules = reactive<FormRules>({
  name: [{required: true, message: '请输入次卡名', trigger: 'blur'}],
  times: [{required: true, message: '请输入次数', trigger: 'blur'}]
})
const addRoleB = (): void => { form.value = empty(); formRef.value?.resetFields(); modalTit.value = '新增次卡'; modalView.value = true }
const showDialog = (row: CountCardTemplate): void => { form.value = {...row}; formRef.value?.resetFields(); modalTit.value = '编辑次卡'; modalView.value = true }
const submit = (): void => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    reqSaveCardTemplate(form.value).then((res: Res) => {
      if (res.code === 1) { ElMessage.success('保存成功'); modalView.value = false; load() }
      else ElMessage.error(res.msg || '保存失败')
    })
  })
}
const delOne = (id: number): void => {
  ElMessageBox.confirm('确定删除该次卡模板吗？', '删除确认', {type: 'warning'})
      .then(() => reqDelCardTemplate(id).then((res: Res) => { if (res.code === 1) { ElMessage.success('已删除'); load() } }))
      .catch(() => ElMessage.info('已取消'))
}
onMounted(() => { load() })
</script>

<style scoped>
.tip { margin: 12px 0 0; font-size: 12px; color: #909399; }
</style>
