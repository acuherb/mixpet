<template>
  <div>
    <PageHead>查看挂号</PageHead>
    <ShakeBtn style="margin-left: 80px" @click="addRoleB">添加挂号</ShakeBtn>
    <MyRow>
      <AppointCard v-for="a in appoint" :key="a.appointmentId" :a="a" @toSuc="openTreat(a)"/>
    </MyRow>
    <!--添加的模态框-->
    <el-dialog v-model="modalView" title="新增挂号" draggable width="60%">
      <!--表单-->
      <el-form ref="myFormRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="简介" prop="appointmentInfo">
          <el-input v-model="form.appointmentInfo" clearable placeholder="无"/>
        </el-form-item>
        <el-form-item label="就诊时间" prop="appointmentDate">
          <el-date-picker
              v-model="form.appointmentDate"
              :value-format="'YYYY-MM-DD'+'T'+'HH:mm:ss'"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择就诊时间"
              type="datetime"
          />
        </el-form-item>
        <el-form-item label="主人" prop="clientId">
          <el-select v-model="form.clientId" filterable placeholder="选择用户" @change="form.petId=null">
            <el-option v-for="d in clientNameL" :key="d.roleId" :label="d.roleName" :value="d.roleId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="宠物" prop="petId">
          <el-select v-model="form.petId" filterable placeholder="选择宠物">
            <el-option v-for="d in petNameL" :key="d.roleId" :label="d.roleName" :value="d.roleId"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit">
          确认
        </el-button>
      </span>
      </template>
    </el-dialog>

    <!--接诊弹窗：病历 + 处方-->
    <el-dialog v-model="treatView" :close-on-click-modal="false" top="4vh" width="94%" style="max-width: 760px">
      <template #header>
        <span style="font-weight: bold">接诊建档</span>
        <el-tag v-if="treatAppoint" style="margin-left: 8px" type="warning">
          {{ treatAppoint.petName }} · {{ treatAppoint.clientName }}
        </el-tag>
      </template>
      <el-scrollbar max-height="72vh">
        <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="90px">
          <el-divider content-position="left">病历</el-divider>
          <el-form-item label="主诉" prop="chiefComplaint">
            <el-input v-model="recordForm.chiefComplaint" :rows="2" maxlength="255" placeholder="宠物主人描述的症状，已自动带入挂号描述" type="textarea"/>
          </el-form-item>
          <el-form-item label="体格检查">
            <el-input v-model="recordForm.physicalExam" :rows="2" maxlength="255" placeholder="如：体温39.2℃，心率120，精神沉郁" type="textarea"/>
          </el-form-item>
          <el-form-item label="诊断" prop="diagnosis">
            <el-input v-model="recordForm.diagnosis" maxlength="255" placeholder="诊断结论"/>
          </el-form-item>
          <el-form-item label="治疗方案">
            <el-input v-model="recordForm.treatment" :rows="2" maxlength="255" placeholder="治疗措施" type="textarea"/>
          </el-form-item>
          <el-form-item label="医嘱">
            <el-input v-model="recordForm.advice" :rows="2" maxlength="255" placeholder="给宠物主人的嘱咐（顾客端可见）" type="textarea"/>
          </el-form-item>
          <el-form-item label="内部备注">
            <el-input v-model="recordForm.internalNote" :rows="2" maxlength="255" placeholder="仅医院内部可见，顾客端不显示" type="textarea"/>
          </el-form-item>
          <el-form-item label="复诊日期">
            <el-date-picker
                v-model="recordForm.revisitDate"
                format="YYYY-MM-DD"
                placeholder="需要复诊时选择（可选）"
                style="width: 100%"
                type="date"
                value-format="YYYY-MM-DD"
            />
          </el-form-item>

          <el-divider content-position="left">费用</el-divider>
          <el-form-item label="挂号费(元)">
            <el-input-number v-model="recordForm.feeRegister" :max="9999" :min="0" :precision="2" :step="5"/>
            <span class="fee-sep">诊疗费(元)</span>
            <el-input-number v-model="recordForm.feeTreat" :max="99999" :min="0" :precision="2" :step="10"/>
          </el-form-item>

          <el-divider content-position="left">处方（发药在收费时进行）</el-divider>
          <el-table :data="prescriptions" border size="small">
            <el-table-column label="药品" min-width="180">
              <template #default="scope">
                <el-select v-model="scope.row.drugId" filterable placeholder="选择药品" size="small" style="width: 100%">
                  <el-option v-for="d in drugList" :key="d.drugId"
                             :disabled="d.drugStock === 0"
                             :label="`${d.drugName}（￥${d.drugPrice}/${d.drugUnit}，库存${d.drugStock}）`"
                             :value="d.drugId"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="100">
              <template #default="scope">
                <el-input-number v-model="scope.row.quantity" :max="99" :min="1" size="small" style="width: 90px"/>
              </template>
            </el-table-column>
            <el-table-column label="用法" min-width="140">
              <template #default="scope">
                <el-input v-model="scope.row.usageInfo" clearable maxlength="64" placeholder="如：每日2次，每次1片" size="small"/>
              </template>
            </el-table-column>
            <el-table-column label="小计" width="80">
              <template #default="scope">
                <span v-if="drugOf(scope.row.drugId)">￥{{ (drugOf(scope.row.drugId)!.drugPrice! * scope.row.quantity).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column width="50">
              <template #default="scope">
                <el-button :icon="Delete" circle size="small" type="danger" @click="prescriptions.splice(scope.$index, 1)"/>
              </template>
            </el-table-column>
          </el-table>
          <el-button :icon="Plus" plain size="small" style="margin-top: 6px; width: 100%" @click="addPrescription">添加药品</el-button>
        </el-form>
      </el-scrollbar>
      <template #footer>
        <div style="text-align: left">
          <span style="font-size: 15px">
            合计：<b style="color: #f56c6c; font-size: 18px">￥{{ totalMoney() }}</b>
            （挂号 ￥{{ recordForm.feeRegister }} + 诊疗 ￥{{ recordForm.feeTreat }} + 药费 ￥{{ feeDrug() }}）
          </span>
        </div>
        <el-button @click="treatView = false">取消</el-button>
        <el-button :loading="treatSubmitting" type="primary" @click="submitTreat">完成接诊</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import PageHead from "@/components/row/PageHead.vue";
import MyRow from "@/components/row/MyRow.vue";
import {AppointDto2} from "@/model/DO/AppointDto2";
import AppointCard from "@/components/card/AppointCard.vue";
import ShakeBtn from "@/components/button/ShakeBtn.vue";
import {reqDoctorAddAppoint, reqDoctorAppoint} from "@/request/DoctorApi";
import {onMounted, reactive, ref, watch} from "vue";
import {NameDo} from "@/model/DO/NameDo";
import {Appoint, exampleAppoint} from "@/model/entiy/Appoint";
import {FormInstance, FormRules} from "element-plus";
import {reqPetNameByClientId} from "@/request/PetApi";
import {reqClientNameHavePet} from "@/request/ClientApi";
import {Delete, Plus} from "@element-plus/icons-vue";
import {Drug} from "@/model/entiy/Drug";
import {reqDrugList} from "@/request/DrugApi";
import {exampleMedicalRecord, examplePrescription, MedicalRecord, Prescription} from "@/model/entiy/MedicalRecord";
import {reqSaveRecord} from "@/request/RecordApi";

const appoint = ref<AppointDto2[]>([])
onMounted(() => {
  queryAppoint()
  reqClientNameHavePet(null).then(res => {
    clientNameL.value = res;
  })
})
// 查询订单
const queryAppoint = () => {
  reqDoctorAppoint().then(res => {
    appoint.value = res
  })
}

/**
 ┌───────────────────────────────────┐
 │=============接诊建档============│
 └───────────────────────────────────┘
 */
// 接诊弹窗
const treatView = ref(false)
const treatAppoint = ref<AppointDto2 | null>(null)
const treatSubmitting = ref(false)
const recordForm = ref<MedicalRecord>(exampleMedicalRecord(0))
const prescriptions = ref<Prescription[]>([])
const recordFormRef = ref<FormInstance>()
const drugList = ref<Drug[]>([])
// 打开接诊弹窗
const openTreat = (a: AppointDto2): void => {
  treatAppoint.value = a
  recordForm.value = exampleMedicalRecord(a.appointmentId)
  recordForm.value.chiefComplaint = a.appointmentInfo || ''
  prescriptions.value = []
  recordFormRef.value?.clearValidate()
  if (drugList.value.length === 0) {
    reqDrugList().then(res => {
      drugList.value = res
    })
  }
  treatView.value = true
}
// 药品字典查找
const drugOf = (drugId: number): Drug | undefined => drugList.value.find(d => d.drugId === drugId)
// 添加处方行
const addPrescription = (): void => {
  prescriptions.value.push(examplePrescription())
}
// 药费合计
const feeDrug = (): string => {
  let sum = 0
  for (const p of prescriptions.value) {
    const d = drugOf(p.drugId)
    if (d && d.drugPrice != null) sum += d.drugPrice * p.quantity
  }
  return sum.toFixed(2)
}
// 总价
const totalMoney = (): string => {
  const sum = (recordForm.value.feeRegister || 0) + (recordForm.value.feeTreat || 0) + Number(feeDrug())
  return sum.toFixed(2)
}
// 提交接诊
const submitTreat = async (): Promise<void> => {
  await recordFormRef.value?.validate((valid: boolean): void => {
    if (!valid) return
    // 过滤没选药品的行
    const ps = prescriptions.value.filter(p => p.drugId && p.drugId > 0)
    treatSubmitting.value = true
    reqSaveRecord({record: recordForm.value, prescriptions: ps}).then(res => {
      if (res.code === 1) {
        treatView.value = false
        queryAppoint()
      }
    }).finally(() => {
      treatSubmitting.value = false
    })
  })
}
const recordRules = reactive<FormRules>({
  "chiefComplaint": [
    {required: true, message: '请填写主诉', trigger: 'blur'},
  ],
  "diagnosis": [
    {required: true, message: '请填写诊断', trigger: 'blur'},
  ],
})

/**
 ┌───────────────────────────────────┐
 │=============新增挂号============│
 └───────────────────────────────────┘
 */
// 表单的数据
const modalView = ref(false)
const clientNameL = ref<NameDo[]>([]) // 下拉框用户名
const petNameL = ref<NameDo[]>([])
const form = ref<Appoint>(exampleAppoint()) // 空的默认值
const myFormRef = ref<FormInstance>()
// 新增
const addRoleB = (): void => {
  form.value = exampleAppoint()
  myFormRef.value?.resetFields()
  modalView.value = true
}
// 校验表单并提交
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      // 校验通过，提交
      modalView.value = false
      reqDoctorAddAppoint(form.value).then(res => {
        if (res.code === 1) queryAppoint()
      })
    }
  })
}
// 表单校验规则
const rules = reactive<FormRules>({
  "appointmentInfo": [
    {required: true, message: '请输入描述', trigger: 'blur'},
  ],
  "appointmentDate": [
    {required: true, message: '请输入日期', trigger: 'blur'},
  ],
  "clientId": [
    {required: true, message: '请选择用户', trigger: 'blur'},
  ],
  "petId": [
    {required: true, message: '请选择宠物', trigger: 'blur'},
  ],
})

// 在用户改变时，查询宠物名
watch(() => form.value.clientId, (newValue): void => {
  if (newValue == 0 || newValue == null) return;
  reqPetNameByClientId(newValue, <number>form.value.petId).then(res => {
    petNameL.value = res
  })
})
</script>

<style lang="scss" scoped>
.fee-sep {
  margin: 0 12px 0 24px;
  color: #909399;
}
</style>
