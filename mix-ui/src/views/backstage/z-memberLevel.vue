<template>
  <div>
    <BackOpCol role="会员等级" @addRole="addRoleB"/>
    <el-table :data="list" max-height="calc(100vh - 160px)" row-key="levelId" stripe>
      <el-table-column label="等级" min-width="120" prop="levelName"/>
      <el-table-column label="等级键" prop="levelKey" width="80"/>
      <el-table-column label="折扣" width="90">
        <template #default="s">{{ s.row.discount ?? 1 }}</template>
      </el-table-column>
      <el-table-column label="充值返赠" width="110">
        <template #default="s">{{ ((s.row.rechargeBonus ?? 0) * 100).toFixed(0) }}%</template>
      </el-table-column>
      <el-table-column label="积分率" width="110">
        <template #default="s">1元={{ s.row.pointsRate ?? 1 }}分</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="s">
          <el-tag :type="s.row.active ? 'success' : 'info'" size="small">{{ s.row.active ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="190">
        <template #default="s">
          <el-button :icon="Edit" circle title="编辑" type="warning" @click="showDialog(s.row)"/>
          <el-button :icon="PriceTag" circle title="类别折扣" type="primary" @click="openDiscount(s.row)"/>
          <el-button :icon="Delete" circle title="删除" type="danger" @click="delOne(s.row.levelId)"/>
        </template>
      </el-table-column>
    </el-table>
    <p class="tip">等级键对应客户等级（0普通/1银/2金/3钻石）；折扣率 1=不打折，0.95=95折；充值返赠 0.1=充100送10；积分率 1=消费1元得1分。</p>

    <el-dialog v-model="modalView" :title="modalTit" draggable width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="等级键" prop="levelKey">
          <el-input-number v-model="form.levelKey" :max="9" :min="0"/>
        </el-form-item>
        <el-form-item label="等级名称" prop="levelName">
          <el-input v-model="form.levelName" maxlength="16" placeholder="如：金卡"/>
        </el-form-item>
        <el-form-item label="折扣率">
          <el-input-number v-model="form.discount" :max="1" :min="0.1" :precision="2" :step="0.05"/>
        </el-form-item>
        <el-form-item label="充值返赠比例">
          <el-input-number v-model="form.rechargeBonus" :max="1" :min="0" :precision="2" :step="0.05"/>
        </el-form-item>
        <el-form-item label="积分率(元)">
          <el-input-number v-model="form.pointsRate" :max="100" :min="0" :step="1"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.active"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="submit">确认</el-button>
      </template>
    </el-dialog>

    <!--类别折扣弹窗：等级折扣之外，按商品类别(drug_type)再设一档折扣，开方时折进净单价-->
    <el-dialog v-model="discView" :title="discTitle" draggable width="500px" :close-on-click-modal="false">
      <p class="tip" style="margin-top: 0">类别折扣与等级折扣<b>叠加</b>：开方时按药品类别折进净单价。例：类别 0.9 × 等级 0.95 = 85.5 折。未配置的类别不打折。</p>
      <div v-for="(d, i) in discRows" :key="i" class="disc-row">
        <el-select v-model="d.itemType" placeholder="商品类别" style="width: 150px">
          <el-option v-for="t in drugTypes" :key="t.dictKey" :label="t.dictKey" :value="t.dictKey"/>
        </el-select>
        <el-input-number v-model="d.discount" :max="1" :min="0.1" :precision="2" :step="0.05"/>
        <el-button :icon="Delete" circle type="danger" @click="discRows.splice(i, 1)"/>
      </div>
      <el-button size="small" @click="discRows.push({itemType: '', discount: 0.9, active: true})">添加类别</el-button>
      <template #footer>
        <el-button @click="discView = false">取消</el-button>
        <el-button type="primary" @click="saveDiscounts">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {Delete, Edit, PriceTag} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {MemberLevel, MemberLevelDiscount} from "@/model/entiy/Member";
import {reqDelMemberLevel, reqLevelDiscounts, reqMemberLevels, reqSaveLevelDiscounts, reqSaveMemberLevel, reqUpdateMemberLevel} from "@/request/MemberApi";
import {reqDictByType} from "@/request/DictApi";
import {SysDict} from "@/model/entiy/SysDict";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

const list = ref<MemberLevel[]>([])
const load = (): void => { reqMemberLevels().then(r => { list.value = r || [] }) }
const modalView = ref(false)
const modalTit = ref('新增等级')
const formRef = ref<FormInstance>()
const empty = (): MemberLevel => ({levelKey: 0, levelName: '', discount: 1, rechargeBonus: 0, pointsRate: 1, active: true})
const form = ref<MemberLevel>(empty())
const rules = reactive<FormRules>({
  levelName: [{required: true, message: '请输入等级名称', trigger: 'blur'}]
})
const addRoleB = (): void => { form.value = empty(); formRef.value?.resetFields(); modalTit.value = '新增等级'; modalView.value = true }
const showDialog = (row: MemberLevel): void => { form.value = {...row}; formRef.value?.resetFields(); modalTit.value = '编辑等级'; modalView.value = true }
const submit = (): void => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    const isEdit = form.value.levelId != null
    const api = isEdit ? reqUpdateMemberLevel : reqSaveMemberLevel
    api(form.value).then((res: Res) => {
      if (res.code === 1) { ElMessage.success('保存成功'); modalView.value = false; load() }
      else ElMessage.error(res.msg || '保存失败')
    })
  })
}
const delOne = (id: number): void => {
  ElMessageBox.confirm('确定删除该等级吗？', '删除确认', {type: 'warning'})
      .then(() => reqDelMemberLevel(id).then((res: Res) => { if (res.code === 1) { ElMessage.success('已删除'); load() } }))
      .catch(() => ElMessage.info('已取消'))
}
// ============ 类别折扣 ============
const discView = ref(false)
const discTitle = ref('类别折扣')
const discLevel = ref<MemberLevel | null>(null)
const discRows = ref<MemberLevelDiscount[]>([])
const drugTypes = ref<SysDict[]>([])
const openDiscount = (row: MemberLevel): void => {
  discLevel.value = row
  discTitle.value = `类别折扣 - ${row.levelName}`
  discView.value = true
  if (drugTypes.value.length === 0) reqDictByType('drug_type').then(r => { drugTypes.value = r || [] })
  reqLevelDiscounts(row.levelId!).then(r => { discRows.value = r || [] })
}
const saveDiscounts = (): void => {
  if (!discLevel.value?.levelId) return
  const rows = discRows.value.filter(d => d.itemType)
  reqSaveLevelDiscounts(discLevel.value.levelId, rows).then((res: Res) => {
    if (res.code === 1) { ElMessage.success('已保存'); discView.value = false }
    else ElMessage.error(res.msg || '保存失败')
  })
}
onMounted(() => { load() })
</script>

<style lang="scss" scoped>
.tip { margin: 12px 0 0; font-size: 12px; color: #909399; }
.disc-row { display: flex; gap: 8px; align-items: center; margin-bottom: 10px; }
</style>
