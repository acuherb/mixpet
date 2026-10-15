<template>
  <div>
    <PageHead>查看挂号</PageHead>
    <ShakeBtn style="margin-left: 80px" @click="addRoleB">添加挂号</ShakeBtn>
    <MyRow>
      <AppointCard v-for="a in appoint" :key="a.appointmentId" :a="a" @toSuc="openTreat(a)"/>
    </MyRow>
    <!--添加的模态框-->
    <el-dialog v-model="modalView" title="新增挂号" draggable width="60%">
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
        <el-button type="primary" @click="formSubmit">确认</el-button>
      </span>
      </template>
    </el-dialog>

    <!--接诊弹窗：阶段一「病历建档」→ 阶段二「开单调（处方/手术/套餐，检验影像走专用开单页）」-->
    <el-dialog v-model="treatView" :close-on-click-modal="false" top="4vh" width="94%" style="max-width: 800px">
      <template #header>
        <span style="font-weight: bold">{{ treatPhase === 'record' ? '接诊建档' : '开单调' }}</span>
        <el-tag v-if="treatAppoint" style="margin-left: 8px" type="warning">
          {{ treatAppoint.petName }} · {{ treatAppoint.clientName }}
        </el-tag>
      </template>

      <!-- 阶段一：病历建档 -->
      <el-form v-if="treatPhase === 'record'" ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="90px">
        <el-divider content-position="left">病历</el-divider>
        <el-form-item label="病历模板">
          <el-select v-model="tplId" clearable filterable placeholder="选择模板一键填充（可再修改）" style="width: 100%" @change="applyTpl">
            <el-option v-for="t in templates" :key="t.templateId" :label="`${t.name}${t.category ? '（' + t.category + '）' : ''}`" :value="t.templateId"/>
          </el-select>
        </el-form-item>
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

        <el-divider content-position="left">分诊与体征</el-divider>
        <el-form-item label="分诊级别">
          <el-select v-model="recordForm.triageLevel" placeholder="选择分诊级别" style="width: 160px">
            <el-option :value="1" label="急诊"/>
            <el-option :value="2" label="普通"/>
            <el-option :value="3" label="复诊"/>
          </el-select>
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="recordForm.weight" :max="200" :min="0" :precision="2" :step="0.1" style="width: 150px"/>
        </el-form-item>
        <el-form-item label="体温(℃)">
          <el-input-number v-model="recordForm.temperature" :max="45" :min="30" :precision="1" :step="0.1" style="width: 150px"/>
        </el-form-item>
        <el-form-item label="心率(次/分)">
          <el-input-number v-model="recordForm.heartRate" :max="400" :min="0" :step="5" style="width: 150px"/>
        </el-form-item>
        <el-form-item label="呼吸(次/分)">
          <el-input-number v-model="recordForm.respiratoryRate" :max="200" :min="0" :step="2" style="width: 150px"/>
        </el-form-item>

        <el-divider content-position="left">费用</el-divider>
        <el-form-item label="挂号费(元)">
          <el-input-number v-model="recordForm.feeRegister" :max="9999" :min="0" :precision="2" :step="5"/>
          <span class="fee-sep">诊疗费(元)</span>
          <el-input-number v-model="recordForm.feeTreat" :max="99999" :min="0" :precision="2" :step="10"/>
        </el-form-item>
        <p class="phase-tip">保存病历后进入开单调：可开处方/手术/套餐；检验、影像请点下方"去开检验单/影像单"在专用页面选择项目开单。</p>
      </el-form>

      <!-- 阶段二：开单调（处方 / 手术 / 套餐） -->
      <div v-else>
        <div class="order-summary">
          已建病历 #{{ treatRecordId }} · 挂号+诊疗 ￥{{ ((recordForm.feeRegister||0)+(recordForm.feeTreat||0)).toFixed(2) }} · 当前合计
          <b style="color:#f56c6c">￥{{ treatTotal() }}</b>
        </div>
        <el-tabs v-model="activeTab">
          <!-- 处方 -->
          <el-tab-pane label="处方" name="rx">
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
            <div style="margin-top: 10px; text-align: right">
              <el-button size="small" type="primary" :loading="rxSaving" @click="saveRx">保存处方（￥{{ feeDrug() }}）</el-button>
            </div>
            <el-divider content-position="left">已开处方</el-divider>
            <el-table :data="savedRx" border size="small">
              <el-table-column label="药品" min-width="140" prop="drugName" show-overflow-tooltip/>
              <el-table-column label="数量" width="80">
                <template #default="s">{{ s.row.quantity }}{{ s.row.drugUnit }}</template>
              </el-table-column>
              <el-table-column label="单价" width="80">
                <template #default="s">￥{{ s.row.drugPrice }}</template>
              </el-table-column>
              <el-table-column label="小计" width="90">
                <template #default="s">￥{{ (s.row.drugPrice * s.row.quantity).toFixed(2) }}</template>
              </el-table-column>
            </el-table>
            <p v-if="savedRx.length === 0" style="color:#999">尚未开具处方</p>
          </el-tab-pane>

          <!-- 手术 -->
          <el-tab-pane label="手术" name="surgery">
            <el-form-item label="需要手术">
              <el-switch v-model="needSurgery"/>
            </el-form-item>
            <template v-if="needSurgery">
              <el-form-item label="手术名称">
                <el-input v-model="surgeryForm.surgeryName" clearable maxlength="128" placeholder="如：绝育手术（母）"/>
              </el-form-item>
              <el-form-item label="类型/麻醉">
                <el-select v-model="surgeryForm.surgeryType" clearable filterable placeholder="类型" size="small" style="width: 48%">
                  <el-option v-for="t in surgeryTypeOpts" :key="t.dictId" :label="t.dictKey" :value="t.dictKey"/>
                </el-select>
                <el-select v-model="surgeryForm.anesthesiaType" clearable placeholder="麻醉" size="small" style="width: 48%; margin-left: 4%">
                  <el-option v-for="a in anesthesiaTypeOpts" :key="a.dictId" :label="a.dictKey" :value="a.dictKey"/>
                </el-select>
              </el-form-item>
              <el-form-item label="手术费(元)">
                <el-input-number v-model="surgeryForm.fee" :max="99999" :min="0" :precision="2" :step="100" style="width: 100%"/>
              </el-form-item>
              <div style="text-align: right">
                <el-button size="small" type="primary" :loading="surgerySaving" @click="createSurgery">安排手术（￥{{ feeSurgery() }}）</el-button>
              </div>
            </template>
            <el-divider content-position="left">已安排手术</el-divider>
            <el-table :data="savedSurgery" border size="small">
              <el-table-column label="手术" min-width="140" prop="surgeryName" show-overflow-tooltip/>
              <el-table-column label="麻醉" width="100" prop="anesthesiaType"/>
              <el-table-column label="费用" width="80">
                <template #default="s">￥{{ s.row.fee }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="s">
                  <el-tag size="small" :type="surgeryStatusType(s.row.status)">{{ surgeryStatusText(s.row.status) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <p v-if="savedSurgery.length === 0" style="color:#999">尚未安排手术</p>
          </el-tab-pane>

          <!-- 套餐 -->
          <el-tab-pane label="套餐" name="pkg">
            <el-table :data="packages" border size="small">
              <el-table-column label="套餐" min-width="200">
                <template #default="scope">
                  <el-select v-model="scope.row.packageId" filterable placeholder="选择套餐" size="small" style="width: 100%">
                    <el-option v-for="pk in packageList" :key="pk.packageId"
                               :label="`${pk.packageName}（￥${pk.price}）`" :value="pk.packageId"/>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="分类" width="80">
                <template #default="scope">{{ pkgOf(scope.row.packageId)?.category || '—' }}</template>
              </el-table-column>
              <el-table-column label="价格(元)" width="90">
                <template #default="scope">￥{{ pkgOf(scope.row.packageId)?.price ?? 0 }}</template>
              </el-table-column>
              <el-table-column width="50">
                <template #default="scope">
                  <el-button :icon="Delete" circle size="small" type="danger" @click="packages.splice(scope.$index, 1)"/>
                </template>
              </el-table-column>
            </el-table>
            <el-button :icon="Plus" plain size="small" style="margin-top: 6px; width: 100%" @click="addPackage">添加套餐</el-button>
            <div style="margin-top: 10px; text-align: right">
              <el-button size="small" type="primary" :loading="pkgSaving" @click="savePkg">保存套餐（￥{{ feePackage() }}）</el-button>
            </div>
            <el-divider content-position="left">已加套餐</el-divider>
            <el-table :data="savedPkg" border size="small">
              <el-table-column label="套餐" min-width="160" prop="packageName" show-overflow-tooltip/>
              <el-table-column label="价格(元)" width="100">
                <template #default="s">￥{{ s.row.price }}</template>
              </el-table-column>
            </el-table>
            <p v-if="savedPkg.length === 0" style="color:#999">尚未添加套餐</p>
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <span class="treat-total">合计：<b style="color:#f56c6c">￥{{ treatTotal() }}</b></span>
        <el-button @click="treatView = false">取消</el-button>
        <el-button v-if="treatPhase === 'record'" :loading="treatSubmitting" type="primary" @click="submitRecord">保存病历并开单</el-button>
        <template v-else>
          <el-button @click="treatPhase = 'record'">返回病历</el-button>
          <el-button type="primary" @click="finishTreat">完成接诊</el-button>
        </template>
      </template>
    </el-dialog>

    <!--就诊汇总（流程连通）-->
    <el-dialog v-model="summaryView" title="接诊完成" width="560px" append-to-body>
      <p style="margin: 0 0 12px">本次接诊已完成，收费账单已生成。处方/手术/套餐可在"接诊"继续追加；检验、影像请在专用页面开单。</p>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="挂号+诊疗">￥{{ (summaryFee.reg + summaryFee.treat).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="药费">￥{{ summaryFee.drug.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="套餐费">￥{{ summaryFee.pkg.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item v-if="summaryFee.surgery > 0" label="手术费">￥{{ summaryFee.surgery.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="合计">
          <b style="color: #f56c6c">￥{{ summaryFee.total.toFixed(2) }}</b>
        </el-descriptions-item>
      </el-descriptions>
      <div class="summary-btns">
        <el-button type="primary" @click="printRx">打印处方笺</el-button>
        <el-button @click="goOrder('d-lab')">去开检验单</el-button>
        <el-button @click="goOrder('d-image')">去开影像单</el-button>
        <el-button @click="goBackstage('z-surgery')">手术记录</el-button>
        <el-button @click="goBackstage('z-hospital')">入院登记</el-button>
        <el-button @click="goBackstage('z-immune')">免疫接种</el-button>
        <el-button @click="summaryView = false">完成</el-button>
      </div>
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
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {reqPetNameByClientId} from "@/request/PetApi";
import {reqClientNameHavePet} from "@/request/ClientApi";
import {Delete, Plus} from "@element-plus/icons-vue";
import {Drug} from "@/model/entiy/Drug";
import {reqDrugList} from "@/request/DrugApi";
import {exampleMedicalRecord, examplePrescription, MedicalRecord, Prescription} from "@/model/entiy/MedicalRecord";
import {reqAddRecordPackage, reqAddRecordPrescription, reqRecordByAppointment, reqRecordOne, reqSaveRecord} from "@/request/RecordApi";
import {reqRecordTemplateList} from "@/request/RecordTemplateApi";
import {RecordTemplate} from "@/model/entiy/RecordTemplate";
import {reqPackageList} from "@/request/PackageApi";
import {examplePackage, RecordPackage, ServicePackage} from "@/model/entiy/ServicePackage";
import {surgeryStatusText, surgeryStatusType, Surgery, SurgeryVo} from "@/model/entiy/Surgery";
import {reqDictByType} from "@/request/DictApi";
import {SysDict} from "@/model/entiy/SysDict";
import {reqCreateSurgery, reqSurgeryByRecord} from "@/request/SurgeryApi";
import {useRouter} from 'vue-router';

const router = useRouter()
// 手术类型/麻醉方式字典（来自系统设置）
const surgeryTypeOpts = ref<SysDict[]>([])
const anesthesiaTypeOpts = ref<SysDict[]>([])
reqDictByType('surgery_type').then(r => { surgeryTypeOpts.value = r || [] })
reqDictByType('anesthesia_type').then(r => { anesthesiaTypeOpts.value = r || [] })

const appoint = ref<AppointDto2[]>([])
onMounted(() => {
  queryAppoint()
  reqClientNameHavePet(null).then(res => {
    clientNameL.value = res;
  })
})
const queryAppoint = () => {
  reqDoctorAppoint().then(res => {
    appoint.value = res
  })
}

/**
 ┌───────────────────────────────────┐
 │=============接诊（两阶段）=========│
 └───────────────────────────────────┘
 */
const treatView = ref(false)
const treatAppoint = ref<AppointDto2 | null>(null)
const treatPhase = ref<'record' | 'orders'>('record')
const treatRecordId = ref(0)
const activeTab = ref('rx')
const treatSubmitting = ref(false)

const recordForm = ref<MedicalRecord>(exampleMedicalRecord(0))
const recordFormRef = ref<FormInstance>()

// 开单：待保存行（处方/手术/套餐）
const prescriptions = ref<Prescription[]>([])
const packages = ref<ServicePackage[]>([])
const needSurgery = ref(false)
const surgeryForm = ref<SurgeryVo>({recordId: null, surgeryName: '', surgeryType: null, anesthesiaType: null, scheduledTime: null, surgeon: null, assistant: null, fee: 0})

// 已保存回显
const savedRx = ref<Prescription[]>([])
const savedPkg = ref<RecordPackage[]>([])
const savedSurgery = ref<Surgery[]>([])

// 保存中
const rxSaving = ref(false)
const pkgSaving = ref(false)
const surgerySaving = ref(false)

// 字典
const drugList = ref<Drug[]>([])
const packageList = ref<ServicePackage[]>([])
const templates = ref<RecordTemplate[]>([])
const tplId = ref<number | null>(null)
const loadDicts = (): void => {
  if (drugList.value.length === 0) reqDrugList().then(res => { drugList.value = res })
  if (packageList.value.length === 0) reqPackageList().then(res => { packageList.value = res })
  if (templates.value.length === 0) reqRecordTemplateList().then(res => { templates.value = res || [] })
}
// 套用病历模板
const applyTpl = (id: number): void => {
  const t = templates.value.find(x => x.templateId === id)
  if (!t) return
  if (t.chiefComplaint != null) recordForm.value.chiefComplaint = t.chiefComplaint
  if (t.physicalExam != null) recordForm.value.physicalExam = t.physicalExam
  if (t.diagnosis != null) recordForm.value.diagnosis = t.diagnosis
  if (t.treatment != null) recordForm.value.treatment = t.treatment
  if (t.advice != null) recordForm.value.advice = t.advice
  if (t.internalNote != null) recordForm.value.internalNote = t.internalNote
}
const resetOrderForms = (): void => {
  prescriptions.value = []
  packages.value = []
  needSurgery.value = false
  surgeryForm.value = {recordId: null, surgeryName: '', surgeryType: null, anesthesiaType: null, scheduledTime: null, surgeon: null, assistant: null, fee: 0}
  savedRx.value = []
  savedPkg.value = []
  savedSurgery.value = []
  activeTab.value = 'rx'
}

const openTreat = (a: AppointDto2): void => {
  treatAppoint.value = a
  recordForm.value = exampleMedicalRecord(a.appointmentId)
  recordForm.value.chiefComplaint = a.appointmentInfo || ''
  tplId.value = null
  resetOrderForms()
  recordFormRef.value?.clearValidate()
  loadDicts()
  treatView.value = true
  treatPhase.value = 'record'
  // 已建过病历的直接进入开单阶段（支持继续追加）
  reqRecordByAppointment(a.appointmentId).then(rec => {
    if (rec && rec.recordId) {
      treatRecordId.value = rec.recordId
      if (rec.feeRegister != null) recordForm.value.feeRegister = rec.feeRegister
      if (rec.feeTreat != null) recordForm.value.feeTreat = rec.feeTreat
      enterOrders()
    }
  })
}
const enterOrders = (): void => {
  treatPhase.value = 'orders'
  activeTab.value = 'rx'
  loadSaved()
}
const loadSaved = (): void => {
  reqRecordOne(treatRecordId.value).then(res => {
    if (res) {
      savedRx.value = res.prescriptions || []
      savedPkg.value = res.packages || []
      if (res.record.feeRegister != null) recordForm.value.feeRegister = res.record.feeRegister
      if (res.record.feeTreat != null) recordForm.value.feeTreat = res.record.feeTreat
    }
  })
  reqSurgeryByRecord(treatRecordId.value).then(r => { savedSurgery.value = r || [] })
}

// 阶段一：保存病历并开单（只建病历+初始账单）
const submitRecord = (): void => {
  recordFormRef.value?.validate((valid: boolean): void => {
    if (!valid) return
    treatSubmitting.value = true
    reqSaveRecord({
      record: recordForm.value,
      prescriptions: [],
      packages: [],
      labTestIds: [],
      imageItemId: null,
      imageExamPart: '',
      surgery: null
    }).then(res => {
      if (res.code === 1) {
        reqRecordByAppointment(treatAppoint.value!.appointmentId).then(rec => {
          treatSubmitting.value = false
          if (rec) {
            treatRecordId.value = rec.recordId
            enterOrders()
          }
        })
      } else {
        treatSubmitting.value = false
        ElMessage.error(res.msg || '保存失败')
      }
    })
  })
}

// 字典查找
const drugOf = (drugId: number): Drug | undefined => drugList.value.find(d => d.drugId === drugId)
const pkgOf = (packageId: number): ServicePackage | undefined => packageList.value.find(p => p.packageId === packageId)
const addPrescription = (): void => { prescriptions.value.push(examplePrescription()) }
const addPackage = (): void => { packages.value.push(examplePackage()) }

// 分区保存
const saveRx = (): void => {
  const ps = prescriptions.value.filter(p => p.drugId && p.drugId > 0)
  if (!ps.length) { ElMessage.warning('请先添加药品'); return }
  rxSaving.value = true
  reqAddRecordPrescription({recordId: treatRecordId.value, prescriptions: ps}).then(res => {
    rxSaving.value = false
    if (res.code === 1) { ElMessage.success(res.msg || '已保存处方'); prescriptions.value = []; loadSaved() }
    else ElMessage.error(res.msg || '保存失败')
  }).catch(() => { rxSaving.value = false })
}
const savePkg = (): void => {
  const ids = packages.value.filter(p => p.packageId && p.packageId > 0).map(p => p.packageId)
  if (!ids.length) { ElMessage.warning('请先添加套餐'); return }
  pkgSaving.value = true
  reqAddRecordPackage({recordId: treatRecordId.value, packageIds: ids}).then(res => {
    pkgSaving.value = false
    if (res.code === 1) { ElMessage.success(res.msg || '已保存套餐'); packages.value = []; loadSaved() }
    else ElMessage.error(res.msg || '保存失败')
  }).catch(() => { pkgSaving.value = false })
}
const createSurgery = (): void => {
  if (!surgeryForm.value.surgeryName.trim()) { ElMessage.warning('请填写手术名称'); return }
  surgerySaving.value = true
  reqCreateSurgery({...surgeryForm.value, recordId: treatRecordId.value}).then(res => {
    surgerySaving.value = false
    if (res.code === 1) {
      ElMessage.success(res.msg || '手术已安排')
      needSurgery.value = false
      surgeryForm.value = {recordId: null, surgeryName: '', surgeryType: null, anesthesiaType: null, scheduledTime: null, surgeon: null, assistant: null, fee: 0}
      loadSaved()
    } else ElMessage.error(res.msg || '安排失败')
  }).catch(() => { surgerySaving.value = false })
}

// 费用合计
const feeSurgery = (): string => (needSurgery.value ? (surgeryForm.value.fee || 0).toFixed(2) : '0.00')
const feePackage = (): string => {
  let sum = 0
  for (const p of packages.value) {
    const pk = pkgOf(p.packageId)
    if (pk && pk.price != null) sum += pk.price
  }
  return sum.toFixed(2)
}
const feeDrug = (): string => {
  let sum = 0
  for (const p of prescriptions.value) {
    const d = drugOf(p.drugId)
    if (d && d.drugPrice != null) sum += d.drugPrice * p.quantity
  }
  return sum.toFixed(2)
}
const savedDrugSum = (): number => savedRx.value.reduce((s, p) => s + (p.drugPrice ?? 0) * (p.quantity ?? 0), 0)
const savedPkgSum = (): number => savedPkg.value.reduce((s, p) => s + (p.price ?? 0), 0)
const savedSurgerySum = (): number => savedSurgery.value.reduce((s, o) => s + (o.fee ?? 0), 0)
const treatTotal = (): string => {
  const sum = (recordForm.value.feeRegister || 0) + (recordForm.value.feeTreat || 0)
      + savedDrugSum() + savedPkgSum() + savedSurgerySum()
      + Number(feeDrug()) + Number(feePackage()) + Number(feeSurgery())
  return sum.toFixed(2)
}

// 完成接诊 → 就诊汇总
const summaryView = ref(false)
const summaryRecordId = ref(0)
const summaryFee = ref({reg: 0, treat: 0, drug: 0, pkg: 0, surgery: 0, total: 0})
const finishTreat = (): void => {
  summaryRecordId.value = treatRecordId.value
  const drug = savedDrugSum() + Number(feeDrug())
  const pkg = savedPkgSum() + Number(feePackage())
  const surgery = savedSurgerySum() + Number(feeSurgery())
  const reg = recordForm.value.feeRegister || 0
  const treat = recordForm.value.feeTreat || 0
  summaryFee.value = {reg, treat, drug, pkg, surgery, total: reg + treat + drug + pkg + surgery}
  summaryView.value = true
  queryAppoint()
}
const printRx = (): void => {
  router.push('/doctorPage/d-rx?recordId=' + summaryRecordId.value)
}
const goOrder = (path: string): void => {
  const id = summaryRecordId.value
  summaryView.value = false
  router.push('/doctorPage/' + path + '?recordId=' + id)
}
const goBackstage = (path: string): void => {
  const id = summaryRecordId.value
  summaryView.value = false
  router.push('/backstage/' + path + '?recordId=' + id)
}

const recordRules = reactive<FormRules>({
  "chiefComplaint": [{required: true, message: '请填写主诉', trigger: 'blur'}],
  "diagnosis": [{required: true, message: '请填写诊断', trigger: 'blur'}],
})

/**
 ┌───────────────────────────────────┐
 │=============新增挂号==============│
 └───────────────────────────────────┘
 */
const modalView = ref(false)
const clientNameL = ref<NameDo[]>([])
const petNameL = ref<NameDo[]>([])
const form = ref<Appoint>(exampleAppoint())
const myFormRef = ref<FormInstance>()
const addRoleB = (): void => {
  form.value = exampleAppoint()
  myFormRef.value?.resetFields()
  modalView.value = true
}
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      modalView.value = false
      reqDoctorAddAppoint(form.value).then(res => {
        if (res.code === 1) queryAppoint()
      })
    }
  })
}
const rules = reactive<FormRules>({
  "appointmentInfo": [{required: true, message: '请输入描述', trigger: 'blur'}],
  "appointmentDate": [{required: true, message: '请输入日期', trigger: 'blur'}],
  "clientId": [{required: true, message: '请选择用户', trigger: 'change'}],
  "petId": [{required: true, message: '请选择宠物', trigger: 'change'}],
})
watch(() => form.value.clientId, (newValue): void => {
  if (newValue == 0 || newValue == null) return;
  reqPetNameByClientId(newValue, <number>form.value.petId).then(res => {
    petNameL.value = res
  })
})
</script>

<style lang="scss" scoped>
.fee-sep { margin: 0 12px 0 24px; color: #909399; }
.phase-tip { margin: 4px 0 0 90px; font-size: 12px; color: #909399; }
.order-summary { padding: 8px 12px; background: #f5f7fa; border-radius: 4px; margin-bottom: 10px; font-size: 14px; }
.treat-total { float: left; font-size: 15px; }
.summary-btns { margin-top: 16px; text-align: right; }
</style>
