<template>
  <div>
    <PageHead>查看挂号</PageHead>
    <div class="appoint-tabbar">
      <button
          class="appoint-tab"
          :class="{active: appointTab === 'pending'}"
          @click="appointTab = 'pending'">待处理 ({{ pendingAppoint.length }})</button>
      <button
          class="appoint-tab"
          :class="{active: appointTab === 'done'}"
          @click="appointTab = 'done'">已处理 ({{ doneAppoint.length }})</button>
      <ShakeBtn style="float: right" @click="addRoleB">添加挂号</ShakeBtn>
    </div>
    <MyRow>
      <AppointCard
          v-for="a in (appointTab === 'pending' ? pendingAppoint : doneAppoint)"
          :key="a.appointmentId"
          :a="a"
          :status="recordStatusMap[a.appointmentId]" @toSuc="openTreat(a)"/>
    </MyRow>
    <!--添加的模态框-->
    <el-dialog v-model="modalView" title="新增挂号" draggable width="60%">
      <el-form ref="myFormRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="简介" prop="appointmentInfo">
          <el-input v-model="form.appointmentInfo" clearable maxlength="64" show-word-limit placeholder="无"/>
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

      <div class="treat-body">
        <div class="treat-main">

      <!-- 阶段一：病历建档 -->
      <el-form v-if="treatPhase === 'record'" ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="90px">
        <el-divider content-position="left">病历模板（一键填充，可再修改）</el-divider>
        <el-form-item label="选择模板">
          <el-select v-model="tplId" clearable filterable placeholder="选择模板一键填充（可再修改）" style="width: 100%" @change="applyTpl">
            <el-option v-for="t in templates" :key="t.templateId" :label="`${t.name}${t.category ? '（' + t.category + '）' : ''}`" :value="t.templateId"/>
          </el-select>
        </el-form-item>

        <el-divider content-position="left">一、问诊 · 宠物为什么来医院？</el-divider>
        <el-form-item label="主诉" prop="chiefComplaint">
          <el-input v-model="recordForm.chiefComplaint" :rows="2" maxlength="255" placeholder="主要症状 + 持续时间，如：呕吐2天，今日次数增加伴食欲下降（已自动带入挂号描述）" type="textarea"/>
        </el-form-item>
        <el-form-item label="现病史">
          <el-input v-model="recordForm.presentIllness" :rows="3" maxlength="1000" placeholder="按时间顺序记录：何时开始 → 初期表现 → 加重/减轻 → 伴随症状 → 是否治疗及用药效果。如：2日前开始间断呕吐，最初每日约1次…今日加重至3次，伴食欲下降" type="textarea"/>
        </el-form-item>
        <el-form-item label="既往病史">
          <el-input v-model="recordForm.pastHistory" :rows="2" maxlength="1000" placeholder="过去重要的医疗信息：既往疾病 / 手术 / 长期用药 / 过敏 / 类似发作史（可参考右侧病史卡与宠物档案）" type="textarea"/>
        </el-form-item>

        <el-divider content-position="left">二、检查 · 医生检查发现了什么？</el-divider>
        <el-form-item label="体格检查">
          <el-input v-model="recordForm.physicalExam" :rows="2" maxlength="255" placeholder="视触叩听等检查发现，如：体温39.2℃，心率120，精神沉郁，黏膜苍白" type="textarea"/>
        </el-form-item>

        <el-divider content-position="left">三、诊断与治疗</el-divider>
        <el-form-item label="诊断" prop="diagnosis">
          <el-input v-model="recordForm.diagnosis" maxlength="255" placeholder="诊断结论"/>
        </el-form-item>
        <el-form-item label="治疗方案">
          <el-input v-model="recordForm.treatment" :rows="2" maxlength="255" placeholder="治疗措施" type="textarea"/>
        </el-form-item>
        <el-form-item label="医嘱">
          <el-input v-model="recordForm.advice" :rows="2" maxlength="255" placeholder="给宠物主人的嘱咐（顾客端可见）" type="textarea"/>
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
        <el-form-item label="内部备注">
          <el-input v-model="recordForm.internalNote" :rows="2" maxlength="255" placeholder="仅医院内部可见，顾客端不显示" type="textarea"/>
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
        <el-alert v-if="orderLocked" :title="treatStatus === 2 ? '该就诊已作废，单据只读' : '该就诊已完结，单据只读'"
                  show-icon :closable="false" type="warning" style="margin-bottom: 10px"/>
        <el-tabs v-model="activeTab">
          <!-- 处方 -->
          <el-tab-pane label="处方" name="rx">
            <el-table :data="prescriptions" border size="small">
              <el-table-column label="药品" min-width="180">
                <template #default="scope">
                  <el-select v-model="scope.row.drugId" filterable placeholder="选择药品" size="small" style="width: 100%" @change="doseCalc(scope.row)">
                    <el-option v-for="d in drugList" :key="d.drugId"
                               :disabled="d.drugStock === 0"
                               :label="`${d.drugName}（￥${d.drugPrice}/${d.drugUnit}，库存${d.drugStock}）`"
                               :value="d.drugId"/>
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="数量" min-width="120">
                <template #default="scope">
                  <el-input-number v-model="scope.row.quantity" :max="99" :min="1" size="small" style="width: 90px"/>
                  <div v-if="scope.row.doseTip" style="margin-top:2px;font-size:11px;line-height:1.35;color:#E6A23C">{{ scope.row.doseTip }}</div>
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
              <el-button size="small" type="primary" :loading="rxSaving" :disabled="orderLocked" @click="saveRx">保存处方（￥{{ feeDrug() }}）</el-button>
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
                <el-button size="small" type="primary" :loading="surgerySaving" :disabled="orderLocked" @click="createSurgery">安排手术（￥{{ feeSurgery() }}）</el-button>
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
              <el-button size="small" type="primary" :loading="pkgSaving" :disabled="orderLocked" @click="savePkg">保存套餐（￥{{ feePackage() }}）</el-button>
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
      </div>

        <!-- 宠物病史侧栏：写病历时参考历史 -->
        <aside v-if="petHistory" class="pet-history">
          <div class="ph-head">宠物病史</div>
          <div class="ph-sec">
            <div class="ph-tit">档案</div>
            <p>{{ petHistory.petName }}（{{ petHistory.petVariety || '未知品种' }} · {{ petHistory.petSex ? '公' : '母' }}）</p>
            <p>芯片：{{ petHistory.petChip || '—' }} · {{ petHistory.petSterilization ? '已绝育' : '未绝育' }}</p>
          </div>
          <div v-if="petHistory.petAllergy" class="ph-sec ph-danger">
            <div class="ph-tit">⚠ 过敏史</div>
            <p>{{ petHistory.petAllergy }}</p>
          </div>
          <div v-if="petHistory.petHistory" class="ph-sec">
            <div class="ph-tit">既往病史</div>
            <p>{{ petHistory.petHistory }}</p>
          </div>
          <div class="ph-sec">
            <div class="ph-tit">最近就诊</div>
            <p v-if="!petHistory.recentRecords.length" class="ph-empty">暂无就诊记录</p>
            <div v-for="r in petHistory.recentRecords" :key="r.recordId" class="ph-rec">
              <span class="ph-diag" :title="r.diagnosis || ''">{{ r.diagnosis || '—' }}</span>
              <span class="ph-date">{{ moveT(r.createTime) }}</span>
            </div>
          </div>
          <div v-if="petHistory.recentRx.length" class="ph-sec">
            <div class="ph-tit">近期用药</div>
            <div v-for="(x, i) in petHistory.recentRx" :key="i" class="ph-rec">
              <span class="ph-diag">{{ x.drugName }}</span>
              <span class="ph-date">{{ x.usageInfo }}</span>
            </div>
          </div>
          <div v-if="petHistory.abnormalLabs.length" class="ph-sec ph-warn">
            <div class="ph-tit">近期检验异常</div>
            <div v-for="(x, i) in petHistory.abnormalLabs" :key="i" class="ph-rec">
              <span class="ph-diag">{{ x.itemName }}</span>
              <span class="ph-date">{{ x.resultValue }}{{ x.itemUnit || '' }}</span>
            </div>
          </div>
        </aside>
      </div>

      <template #footer>
        <span class="treat-total">合计：<b style="color:#f56c6c">￥{{ treatTotal() }}</b></span>
        <el-button @click="treatView = false">取消</el-button>
        <el-button v-if="treatPhase === 'record'" :loading="treatSubmitting" type="primary" @click="submitRecord">
          {{ treatRecordId > 0 ? '保存修改' : '保存病历并开单' }}
        </el-button>
        <template v-else>
          <el-button @click="treatPhase = 'record'">返回病历</el-button>
          <el-button v-if="!orderLocked" plain type="danger" @click="cancelTreat">作废就诊</el-button>
          <el-button :disabled="orderLocked" type="primary" @click="finishTreat">完结就诊</el-button>
        </template>
      </template>
    </el-dialog>

    <!--就诊汇总（流程连通）-->
    <el-dialog v-model="summaryView" title="就诊完结" width="560px" append-to-body>
      <p style="margin: 0 0 12px">本次就诊已完结，收费账单已生成。完结后不能再追加/修改单据；检验、影像请在专用页面开单。</p>
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
        <el-button @click="printRecord('std')">国标病历</el-button>
        <el-button @click="printRecord('color')">彩色详情版</el-button>
        <el-button @click="goOrder('d-lab')">去开检验单</el-button>
        <el-button @click="goOrder('d-image')">去开影像单</el-button>
        <el-button @click="goBackstage('z-surgery')">手术记录</el-button>
        <el-button @click="goBackstage('z-hospital')">入院登记</el-button>
        <el-button @click="goBackstage('z-immune')">免疫接种</el-button>
        <el-button @click="goBackstage('z-follow')">写随访</el-button>
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
import {computed, onMounted, reactive, ref, watch} from "vue";
import {NameDo} from "@/model/DO/NameDo";
import {Appoint, exampleAppoint} from "@/model/entiy/Appoint";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {reqPetNameByClientId} from "@/request/PetApi";
import {reqClientNameHavePet} from "@/request/ClientApi";
import {Delete, Plus} from "@element-plus/icons-vue";
import {Drug} from "@/model/entiy/Drug";
import {reqDrugList} from "@/request/DrugApi";
import {exampleMedicalRecord, examplePrescription, MedicalRecord, Prescription} from "@/model/entiy/MedicalRecord";
import {reqAddRecordPackage, reqAddRecordPrescription, reqCancelRecord, reqFinishRecord, reqRecordByAppointment, reqRecordOne, reqSaveRecord, reqUpdateRecord} from "@/request/RecordApi";
import {reqRecordTemplateList} from "@/request/RecordTemplateApi";
import {RecordTemplate} from "@/model/entiy/RecordTemplate";
import {reqPackageList} from "@/request/PackageApi";
import {examplePackage, RecordPackage, ServicePackage} from "@/model/entiy/ServicePackage";
import {surgeryStatusText, surgeryStatusType, Surgery, SurgeryVo} from "@/model/entiy/Surgery";
import {reqDictByType} from "@/request/DictApi";
import {SysDict} from "@/model/entiy/SysDict";
import {reqCreateSurgery, reqSurgeryByRecord} from "@/request/SurgeryApi";
import {useRouter} from 'vue-router';
import {moveT} from "@/utils/TimeUtil";
import {reqPetHistory} from "@/request/RecordApi";
import {PetHistory} from "@/model/VO/PetHistory";

const router = useRouter()
// 手术类型/麻醉方式字典（来自系统设置）
const surgeryTypeOpts = ref<SysDict[]>([])
const anesthesiaTypeOpts = ref<SysDict[]>([])
reqDictByType('surgery_type').then(r => { surgeryTypeOpts.value = r || [] })
reqDictByType('anesthesia_type').then(r => { anesthesiaTypeOpts.value = r || [] })

const appoint = ref<AppointDto2[]>([])
// 待处理/已处理 分类选项卡
const appointTab = ref<'pending' | 'done'>('pending')
const pendingAppoint = computed(() => appoint.value.filter(a => !a.isSuc))
const doneAppoint = computed(() => appoint.value.filter(a => !!a.isSuc))
onMounted(() => {
  queryAppoint()
  reqClientNameHavePet(null).then(res => {
    clientNameL.value = res;
  })
})
const queryAppoint = () => {
  reqDoctorAppoint().then(res => {
    appoint.value = res
    loadRecordStatus()
  })
}
// 已处理挂号对应的病历状态（0接诊中 1已完结 2已作废），用于卡片标签
const recordStatusMap = ref<Record<number, number>>({})
const loadRecordStatus = (): void => {
  doneAppoint.value.forEach(a => {
    if (recordStatusMap.value[a.appointmentId] != null) return
    reqRecordByAppointment(a.appointmentId).then(rec => {
      if (rec && rec.recordId) recordStatusMap.value[a.appointmentId] = rec.status ?? 1
    })
  })
}

/**
 ┌───────────────────────────────────┐
 │=============接诊（两阶段）=========│
 └───────────────────────────────────┘
 */
const treatView = ref(false)
const treatAppoint = ref<AppointDto2 | null>(null)
/** 宠物病史侧栏（接诊时参考：档案/过敏史/既往史/最近就诊/用药/检验异常） */
const petHistory = ref<PetHistory | null>(null)
const loadPetHistory = (petId: number): void => {
  if (!petId) { petHistory.value = null; return }
  reqPetHistory(petId).then(res => { petHistory.value = res })
}
const treatPhase = ref<'record' | 'orders'>('record')
const treatRecordId = ref(0)
/** 当前病历状态：0接诊中 1已完结 2已作废；非0时单据只读 */
const treatStatus = ref<number>(0)
const orderLocked = computed(() => treatStatus.value !== 0)
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
  if (t.presentIllness != null) recordForm.value.presentIllness = t.presentIllness
  if (t.pastHistory != null) recordForm.value.pastHistory = t.pastHistory
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
  loadPetHistory(a.petId)
  recordForm.value = exampleMedicalRecord(a.appointmentId)
  recordForm.value.chiefComplaint = a.appointmentInfo || ''
  tplId.value = null
  resetOrderForms()
  recordFormRef.value?.clearValidate()
  loadDicts()
  treatView.value = true
  treatPhase.value = 'record'
  treatStatus.value = 0
  // 已建过病历：回显内容 + 进入开单阶段（接诊中可继续追加/修改，已完结/已作废只读）
  reqRecordByAppointment(a.appointmentId).then(rec => {
    if (rec && rec.recordId) {
      treatRecordId.value = rec.recordId
      treatStatus.value = rec.status ?? 1
      recordForm.value.chiefComplaint = rec.chiefComplaint ?? recordForm.value.chiefComplaint
      recordForm.value.presentIllness = rec.presentIllness
      recordForm.value.pastHistory = rec.pastHistory
      recordForm.value.physicalExam = rec.physicalExam
      recordForm.value.diagnosis = rec.diagnosis
      recordForm.value.treatment = rec.treatment
      recordForm.value.advice = rec.advice
      recordForm.value.internalNote = rec.internalNote
      recordForm.value.revisitDate = rec.revisitDate
      recordForm.value.triageLevel = rec.triageLevel
      recordForm.value.weight = rec.weight
      recordForm.value.temperature = rec.temperature
      recordForm.value.heartRate = rec.heartRate
      recordForm.value.respiratoryRate = rec.respiratoryRate
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

// 阶段一：保存病历并开单（新建走 save；已建病历未完绪走 update 只改内容）
const submitRecord = (): void => {
  recordFormRef.value?.validate((valid: boolean): void => {
    if (!valid) return
    if (treatRecordId.value > 0) {
      if (orderLocked.value) { ElMessage.warning('该就诊已完结/已作废，不能修改'); return }
      treatSubmitting.value = true
      reqUpdateRecord({...recordForm.value, recordId: treatRecordId.value}).then(res => {
        treatSubmitting.value = false
        if (res.code === 1) { ElMessage.success('病历已更新'); enterOrders() }
        else ElMessage.error(res.msg || '保存失败')
      })
      return
    }
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
// 处方自动算量：药品配置了剂量规则时，按体重/固定量自动填入数量（医生可改）
const doseCalc = (row: Prescription): void => {
  const d = drugOf(row.drugId)
  if (!d) return
  if (d.doseFixed != null && d.doseFixed > 0) {
    row.quantity = Math.max(1, Math.round(d.doseFixed))
    row.doseTip = `固定单次用量 ${d.doseFixed}${d.drugUnit || ''}，已自动填入，请确认`
  } else if (d.dosePerKg != null && d.dosePerKg > 0) {
    const w = recordForm.value.weight
    if (w == null) {
      row.doseTip = '该药按体重算量，请先在上方「体征」填写体重'
      return
    }
    const dose = w * d.dosePerKg
    row.quantity = Math.max(1, Math.round(dose))
    row.doseTip = `按体重 ${w}kg 算得单次约 ${dose.toFixed(2)}${d.drugUnit || ''}，已取整，请确认`
  } else {
    row.doseTip = ''
  }
}
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

// 完结就诊 → 就诊汇总（有未完成事项时确认后强制完结）
const summaryView = ref(false)
const summaryRecordId = ref(0)
const summaryFee = ref({reg: 0, treat: 0, drug: 0, pkg: 0, surgery: 0, total: 0})
const finishTreat = (): void => {
  reqFinishRecord(treatRecordId.value, false).then(res => {
    if (res.code === 1) { showSummary(); return }
    // 有未完成事项（待出结果/待手术/待随访）：确认后强制完结
    ElMessageBox.confirm(res.msg + '。确认仍要完结吗？', '存在未完成事项', {
      confirmButtonText: '仍要完结', cancelButtonText: '返回继续处理', type: 'warning'
    }).then(() => {
      reqFinishRecord(treatRecordId.value, true).then(r2 => {
        if (r2.code === 1) showSummary()
        else ElMessage.error(r2.msg || '完结失败')
      })
    }).catch(() => ElMessage.info('已取消，可继续处理'))
  })
}
const showSummary = (): void => {
  treatStatus.value = 1
  if (treatAppoint.value) recordStatusMap.value[treatAppoint.value.appointmentId] = 1
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
// 作废就诊（仅接诊中，需填原因）
const cancelTreat = (): void => {
  ElMessageBox.prompt('填写作废原因（必填）：', '作废就诊', {
    confirmButtonText: '确认作废', cancelButtonText: '取消', inputType: 'textarea',
    inputPattern: /\S+/, inputErrorMessage: '请填写作废原因'
  }).then(({value}) => {
    reqCancelRecord(treatRecordId.value, value).then(res => {
      if (res.code === 1) {
        ElMessage.success('已作废')
        treatStatus.value = 2
        if (treatAppoint.value) recordStatusMap.value[treatAppoint.value.appointmentId] = 2
        treatView.value = false
        queryAppoint()
      } else ElMessage.error(res.msg || '作废失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}
const printRx = (): void => {
  router.push('/doctorPage/d-rx?recordId=' + summaryRecordId.value)
}
// 打印完整病历：std 国标病历 / color 彩色详情版
const printRecord = (style: string): void => {
  router.push('/doctorPage/d-recordPrint?recordId=' + summaryRecordId.value + '&style=' + style)
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
.appoint-tabbar { margin: 0 80px 12px; }
.appoint-tab {
  margin-right: 10px;
  padding: 8px 22px;
  border: 1px solid #dcdfe6;
  border-radius: 18px;
  background-color: #fff;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all .2s;
  &:hover { border-color: #409eff; color: #409eff; }
  &.active { background-color: #409eff; border-color: #409eff; color: #fff; }
}
.fee-sep { margin: 0 12px 0 24px; color: #909399; }
.phase-tip { margin: 4px 0 0 90px; font-size: 12px; color: #909399; }
.order-summary { padding: 8px 12px; background: #f5f7fa; border-radius: 4px; margin-bottom: 10px; font-size: 14px; }
.treat-total { float: left; font-size: 15px; }

/* 接诊弹窗：左表单 + 右病史侧栏 */
.treat-body { display: flex; gap: 14px; align-items: flex-start; }
.treat-main { flex: 1; min-width: 0; }
.pet-history {
  width: 260px;
  flex-shrink: 0;
  max-height: 66vh;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fafbfc;

  .ph-head { font-weight: 700; color: #1F2D3D; margin-bottom: 8px; font-size: 14px; }
  .ph-sec { margin-bottom: 10px; p { margin: 2px 0; font-size: 12px; color: #606266; line-height: 1.5; } }
  .ph-tit { font-size: 12px; font-weight: 600; color: #909399; margin-bottom: 3px; }
  .ph-rec { display: flex; justify-content: space-between; gap: 8px; font-size: 12px; padding: 2px 0; }
  .ph-date { color: #909399; flex-shrink: 0; }
  .ph-diag { color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .ph-empty { color: #c0c4cc; }
  .ph-danger { background: #fef0f0; border-radius: 6px; padding: 6px; .ph-tit { color: #f56c6c; } p { color: #dd6161; } }
  .ph-warn { background: #fdf6ec; border-radius: 6px; padding: 6px; .ph-tit { color: #e6a23c; } }
}
@media (max-width: 900px) { .pet-history { display: none; } }
.summary-btns { margin-top: 16px; text-align: right; }
</style>