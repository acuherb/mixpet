<template>
  <div>
    <!--顶部操作-->
    <div class="pet-head">
      <el-button :icon="Plus" round type="primary" @click="openAddDialog">添加宠物</el-button>
    </div>
    <div v-for="pet in petList" :key="pet.petId" class="pa-card">
      <div class="my-row-2">
        <div class="left">
          <img :src="'/api/common/download?name='+pet.petPhoto" alt="宠物" class="my-avatar"/>
        </div>
        <div class="right">
          <el-descriptions :column="descColumn" border>
            <template #title>
              <span>{{ pet.petName }}</span>
              <el-tag v-if="pet.active === false" size="small" style="margin-left: 8px" type="warning">审核中</el-tag>
            </template>
            <el-descriptions-item
                align="center"
                label="品种"
                label-align="right">
              <el-tag>{{ pet.petVariety }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item align="center" label="健康状况" label-align="right">
              <el-tag>{{ pet.petStatus }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item align="center" label="年龄" label-align="right">
              {{ getAge(pet.petBirthday)}}
            </el-descriptions-item>
            <el-descriptions-item align="center" label="性别" label-align="right">
              <TagSex :sex="pet.petSex"/>
            </el-descriptions-item>
            <el-descriptions-item align="center" label="操作" label-align="right">
              <el-button type="success" @click="$router.push('/reception/petOne/'+pet.petId)">详情
              </el-button>
              <el-button type="primary" @click="openRecords(pet)">就诊记录</el-button>
              <el-button type="warning" @click="openHealth(pet)">健康档案</el-button>
              <el-button type="danger" @click="returnPet(pet)">退养</el-button>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </div>
    <el-empty v-if="petList.length===0" description="暂无宠物，点上方「添加宠物」登记您的宠物吧"/>

    <!--就诊记录弹窗-->
    <el-dialog v-model="recordsVisible" top="6vh" width="94%" style="max-width: 560px">
      <template #header>
        <span style="font-weight: bold">{{ recordsPet?.petName }} 的就诊记录</span>
      </template>
      <el-empty v-if="records.length === 0" description="暂无就诊记录"/>
      <div v-else class="record-list">
        <div v-for="r in records" :key="r.recordId" class="record-item" @click="openRecordDetail(r.recordId)">
          <div class="record-head">
            <span class="record-date">{{ (r.createTime || '').replace('T', ' ').slice(0, 16) }}</span>
            <el-tag v-if="r.payStatus" size="small" type="success">已缴费</el-tag>
            <el-tag v-else size="small" type="warning">待缴费</el-tag>
          </div>
          <div class="record-body">
            <p><b>诊断：</b>{{ r.diagnosis || '—' }}</p>
            <p class="muted">主诉：{{ r.chiefComplaint || '—' }}</p>
            <p class="muted">医生：{{ r.doctorName }}（{{ r.departmentName }}）</p>
          </div>
          <div class="record-foot">
            <span v-if="r.revisitDate" class="revisit">复诊：{{ r.revisitDate }}</span>
            <span class="fee">￥{{ r.totalMoney ?? 0 }}</span>
            <span class="link">查看详情 ›</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!--一页式健康档案-->
    <PetHealthDialog v-model="healthView" :pet-id="healthPetId"/>

    <!--病历详情弹窗（顾客视角，脱敏）-->
    <el-dialog v-model="detailVisible" title="病历详情" top="6vh" width="94%" style="max-width: 560px">
      <template #footer>
        <el-button round @click="saveRecordPdf">保存病历（PDF）</el-button>
        <el-button round @click="viewRx">查看处方笺</el-button>
        <el-button round type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="宠物">{{ detail.record.petName }}</el-descriptions-item>
          <el-descriptions-item label="就诊时间">{{ (detail.record.createTime || '').replace('T', ' ').slice(0, 16) }}</el-descriptions-item>
          <el-descriptions-item label="体重">{{ detail.record.weight != null ? detail.record.weight + 'kg' : '—' }}</el-descriptions-item>
          <el-descriptions-item label="血压">{{ detail.record.bloodPressure || '—' }}</el-descriptions-item>
          <el-descriptions-item label="体温(℃)">{{ detail.record.temperature ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="心率(次/分)">{{ detail.record.heartRate ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="呼吸(次/分)">{{ detail.record.respiratoryRate ?? '—' }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="主诉">{{ detail.record.chiefComplaint || '—' }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="现病史">{{ detail.record.presentIllness || '—' }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="既往病史">{{ detail.record.pastHistory || '—' }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="体格检查">{{ detail.record.physicalExam || '—' }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="诊断">{{ detail.record.diagnosis || '—' }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="治疗方案">{{ detail.record.treatment || '—' }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="医嘱">{{ detail.record.advice || '—' }}</el-descriptions-item>
          <el-descriptions-item label="接诊医生">{{ detail.record.doctorName }}</el-descriptions-item>
          <el-descriptions-item label="科室">{{ detail.record.departmentName }}</el-descriptions-item>
          <el-descriptions-item label="复诊日期">{{ detail.record.revisitDate || '无需复诊' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.record.originRecordId" :span="2" label="关联就诊">
            复诊（初诊/上次病历 #{{ detail.record.originRecordId }}{{ detail.record.originDiagnosis ? '，诊断：' + detail.record.originDiagnosis : '' }}）
          </el-descriptions-item>
          <el-descriptions-item label="费用合计">￥{{ detail.record.totalMoney ?? 0 }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="revisits.length" style="margin-top:10px">
          <h4 style="margin: 6px 0">复诊记录</h4>
          <el-table :data="revisits" border size="small">
            <el-table-column label="就诊时间" min-width="150">
              <template #default="s">{{ (s.row.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
            </el-table-column>
            <el-table-column label="诊断" min-width="160" prop="diagnosis" show-overflow-tooltip/>
          </el-table>
        </div>
        <div class="no-print" style="text-align: right; margin-bottom: 6px">
          <el-button size="small" plain round type="warning" @click="bookRevisit">预约复诊</el-button>
          <el-button size="small" type="primary" @click="printRx">打印处方笺</el-button>
        </div>
        <h4 style="margin: 12px 0 6px">处方用药</h4>
        <el-table :data="detail.prescriptions" border size="small">
          <el-table-column label="药品" min-width="110" prop="drugName" show-overflow-tooltip/>
          <el-table-column label="数量" width="60">
            <template #default="scope">{{ scope.row.quantity }}{{ scope.row.drugUnit }}</template>
          </el-table-column>
          <el-table-column label="用法" min-width="120" prop="usageInfo" show-overflow-tooltip/>
        </el-table>
        <p v-if="detail.prescriptions.length === 0" style="color:#999">本次未开处方</p>
        <h4 style="margin: 14px 0 6px">套餐服务</h4>
        <el-table :data="detail.packages" border size="small">
          <el-table-column label="套餐" min-width="140" prop="packageName" show-overflow-tooltip/>
          <el-table-column label="价格(元)" width="90" prop="price"/>
        </el-table>
        <p v-if="!detail.packages || detail.packages.length === 0" style="color:#999">本次无套餐</p>
        <h4 style="margin: 14px 0 6px">化验结果</h4>
        <template v-for="lab in labs" :key="lab.orderId">
          <div class="lab-hd">
            化验单 #{{ lab.orderId }} · 费用 ￥{{ lab.fee }}
            <el-tag v-if="lab.status" size="small" type="success">已出结果</el-tag>
            <el-tag v-else size="small" type="warning">待检验</el-tag>
          </div>
          <el-table :data="lab.items || []" border size="small" style="margin-bottom: 8px">
            <el-table-column label="检验项" min-width="120" prop="itemName" show-overflow-tooltip/>
            <el-table-column label="结果" width="110">
              <template #default="s">{{ s.row.resultValue || '—' }}</template>
            </el-table-column>
            <el-table-column label="参考范围" width="120" prop="refRange" show-overflow-tooltip/>
            <el-table-column label="单位" width="60" prop="itemUnit"/>
            <el-table-column label="异常" width="70">
              <template #default="s">
                <el-tag v-if="s.row.isAbnormal" size="small" type="danger">异常</el-tag>
                <el-tag v-else-if="s.row.resultValue" size="small" type="success">正常</el-tag>
                <span v-else>—</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="lab.reportFile" style="margin-bottom:8px">
            <el-link type="primary" :href="'/api/common/downReport?name=' + lab.reportFile" target="_blank">查看检查报告附件</el-link>
          </div>
        </template>
        <p v-if="labs.length === 0" style="color:#999">本次无化验项目</p>
        <h4 style="margin: 14px 0 6px">影像检查</h4>
        <template v-for="img in images" :key="img.orderId">
          <div class="lab-hd">
            {{ img.itemName }}（{{ img.groupName || '—' }}）· 部位：{{ img.examPart || '—' }} · 费用 ￥{{ img.fee }}
            <el-tag v-if="img.status" size="small" type="success">已出报告</el-tag>
            <el-tag v-else size="small" type="warning">待检查</el-tag>
          </div>
          <div class="lab-note" style="margin-bottom:8px">报告：{{ img.result || '尚未出报告' }}</div>
          <div v-if="img.reportFile" style="margin-bottom:8px">
            <el-link type="primary" :href="'/api/common/downReport?name=' + img.reportFile" target="_blank">查看影像报告附件</el-link>
          </div>
        </template>
        <p v-if="images.length === 0" style="color:#999">本次无影像检查</p>
        <h4 style="margin: 14px 0 6px">住院信息</h4>
        <template v-for="h in hospitals" :key="h.hospitalId">
          <div class="lab-hd">
            病房 {{ h.roomNo || '—' }} · 日均 ￥{{ h.dailyFee }} · {{ h.days ?? 0 }} 天
            <el-tag v-if="h.status" size="small" type="info">已出院</el-tag>
            <el-tag v-else size="small" type="warning">住院中</el-tag>
          </div>
        </template>
        <p v-if="hospitals.length === 0" style="color:#999">本次无住院记录</p>
        <h4 style="margin: 14px 0 6px">手术/麻醉</h4>
        <template v-for="su in surgeries" :key="su.surgeryId">
          <div class="lab-hd">
            {{ su.surgeryName }}（{{ su.surgeryType || '—' }}）· 麻醉：{{ su.anesthesiaType || '—' }} · 费用 ￥{{ su.fee }}
            <el-tag :type="surgeryStatusType(su.status)" size="small">{{ surgeryStatusText(su.status) }}</el-tag>
          </div>
          <div class="lab-note" style="margin-bottom:8px" v-if="su.postopNote">术后：{{ su.postopNote }}</div>
        </template>
        <p v-if="surgeries.length === 0" style="color:#999">本次无手术</p>
      </template>
    </el-dialog>

    <!--添加宠物弹窗-->
    <el-dialog v-model="addVisible" title="添加宠物" top="6vh" width="90%" style="max-width: 480px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <!--头像（居中显示）-->
        <div class="photo-wrap">
          <UpImg :photoNm="addForm.petPhoto" :name="addForm.petName" show-random @upPhoto="changePhoto"/>
          <div class="photo-tip">点击虚线框上传头像（JPG/PNG，≤3MB，可选）</div>
        </div>

        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="姓名" prop="petName">
          <el-input v-model="addForm.petName" clearable maxlength="16" placeholder="宠物姓名"/>
        </el-form-item>
        <el-form-item label="品种" prop="petVariety">
          <el-input v-model="addForm.petVariety" clearable maxlength="16" placeholder="如：英国短毛猫"/>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="addForm.petSex">
            <el-radio :label="true" border>公</el-radio>
            <el-radio :label="false" border>母</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker
              v-model="addForm.petBirthday"
              format="YYYY-MM-DD"
              placeholder="选择出生日期（可选）"
              style="width: 100%"
              type="date"
              value-format="YYYY-MM-DD"
              @change="addForm.petBirthdayEstimated = false"
          />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input
              v-model="ageInput"
              clearable
              placeholder="不知道生日可填年龄，如：2岁3个月 / 约2岁"
              @change="onAgeInput"
          />
        </el-form-item>

        <el-divider content-position="left">详细信息（可选）</el-divider>
        <el-form-item label="毛色">
          <el-input v-model="addForm.petColor" clearable maxlength="16" placeholder="如：三花"/>
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number
              v-model="addForm.petWeight"
              :max="200"
              :min="0"
              :precision="1"
              :step="0.5"
              placeholder="如：4.5"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="绝育">
          <el-radio-group v-model="addForm.petSterilization">
            <el-radio :label="true" border>已绝育</el-radio>
            <el-radio :label="false" border>未绝育</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="健康状况">
          <el-input v-model="addForm.petStatus" clearable maxlength="16" placeholder="默认为「健康」"/>
        </el-form-item>
        <el-form-item label="芯片号">
          <el-input v-model="addForm.petChip" clearable maxlength="32" placeholder="宠物芯片编号"/>
        </el-form-item>

        <el-divider content-position="left">健康档案（可选）</el-divider>
        <el-form-item label="过敏史">
          <el-input v-model="addForm.petAllergy" :rows="2" maxlength="128" placeholder="如：对青霉素过敏" type="textarea"/>
        </el-form-item>
        <el-form-item label="既往病史">
          <el-input v-model="addForm.petHistory" :rows="2" maxlength="128" placeholder="曾患疾病" type="textarea"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.petNote" :rows="2" maxlength="128" placeholder="其他想告诉医生的信息" type="textarea"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {Pet, examplePet} from "@/model/entiy/Pet";
import {getAge, parseAgeToBirthday} from "@/utils/TimeUtil";
import TagSex from "@/components/show/TagSex.vue";
import {onMounted, reactive, ref} from "vue";
import {reqAddSelfPet, reqClientPet} from "@/request/PetApi";
import {reqReturnPet} from "@/request/AdoptApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import UpImg from "@/components/UpImg.vue";
import {Plus} from "@element-plus/icons-vue";
import {RecordDto, RecordDetailVo} from "@/model/DO/RecordDto";
import {reqPetRecords, reqRecordOne, reqRecordRevisits} from "@/request/RecordApi";
import {reqLabByRecord} from "@/request/LabApi";
import {LabOrderDto} from "@/model/DO/LabOrderDto";
import {reqImageByRecord} from "@/request/ImageApi";
import {reqHospitalByRecord} from "@/request/HospitalApi";
import {ImageOrder} from "@/model/entiy/ImageOrder";
import {HospitalDto} from "@/model/DO/HospitalDto";
import {reqSurgeryByRecord} from "@/request/SurgeryApi";
import {Surgery, surgeryStatusText, surgeryStatusType} from "@/model/entiy/Surgery";
import {useRoute, useRouter} from 'vue-router';
import PetHealthDialog from "@/components/PetHealthDialog.vue";

const petList = ref<Pet[]>([]);

/**
 ┌───────────────────────────────────┐
 │=============就诊记录============│
 └───────────────────────────────────┘
 */
const recordsVisible = ref(false)
const recordsPet = ref<Pet | null>(null)
const records = ref<RecordDto[]>([])
const openRecords = (pet: Pet): void => {
  recordsPet.value = pet
  records.value = []
  recordsVisible.value = true
  reqPetRecords(pet.petId).then(res => {
    records.value = res?.records || []
  })
}
// 一页式健康档案
const healthView = ref(false)
const healthPetId = ref<number | null>(null)
const openHealth = (pet: Pet): void => {
  healthPetId.value = pet.petId
  healthView.value = true
}
const detailVisible = ref(false)
const detail = ref<RecordDetailVo | null>(null)
// 保存病历：跳打印页彩色详情版导出 PDF（顾客仅能导自己的病历，后端 /record/detail 有归属校验）
const saveRecordPdf = (): void => {
  if (detail.value) window.open('/#/doctorPage/d-recordPrint?recordId=' + detail.value.record.recordId + '&style=color', '_blank')
}
// 查看处方笺：只读查看（编号由医院打印时生成；GET /record/rx 有顾客归属校验）
const viewRx = (): void => {
  if (detail.value) window.open('/#/doctorPage/d-rx?recordId=' + detail.value.record.recordId, '_blank')
}
const labs = ref<LabOrderDto[]>([])
const images = ref<ImageOrder[]>([])
const hospitals = ref<HospitalDto[]>([])
const surgeries = ref<Surgery[]>([])
const revisits = ref<RecordDto[]>([])
const router = useRouter()
const route = useRoute()
// 顾客自助打印兽医处方笺（后端 Rx 接口已按归属校验，仅本人宠物）
const printRx = (): void => {
  if (detail.value) router.push('/doctorPage/d-rx?recordId=' + detail.value.record.recordId)
}
// 预约复诊：带初诊病历id跳到挂号页
const bookRevisit = (): void => {
  if (detail.value) router.push('/reception/clientCenter/clientAppoint?originRecordId=' + detail.value.record.recordId)
}
const openRecordDetail = (recordId: number): void => {
  labs.value = []
  images.value = []
  hospitals.value = []
  surgeries.value = []
  revisits.value = []
  reqRecordOne(recordId).then(res => {
    if (res) {
      detail.value = res
      detailVisible.value = true
      // 顾客只能查自己宠物的就诊结果（后端已做归属校验）
      reqLabByRecord(recordId).then(list => {
        labs.value = list || []
      })
      reqRecordRevisits(recordId).then(list => {
        revisits.value = list || []
      })
      reqImageByRecord(recordId).then(list => {
        images.value = list || []
      })
      reqHospitalByRecord(recordId).then(list => {
        hospitals.value = list || []
      })
      reqSurgeryByRecord(recordId).then(list => {
        surgeries.value = list || []
      })
    }
  })
}

// 退养：把自己名下的宠物退回，宠物变回无主待领养
const returnPet = (pet: Pet): void => {
  ElMessageBox.confirm(
      `确定退养【${pet.petName}】吗？退养后它将回到待领养列表，可以被其他人领养。`,
      '退养确认',
      {confirmButtonText: '确定退养', cancelButtonText: '再想想', type: 'warning'}
  ).then(() => {
    reqReturnPet(pet.petId).then(res => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '退养成功')
        reqClientPet().then(r => {
          petList.value = r
        })
      } else {
        ElMessage.error(res.msg || '退养失败')
      }
    })
  }).catch(() => ElMessage.info('已取消'))
}

/*=====添加宠物=====*/
const addVisible = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = ref<Pet>(examplePet())
const ageInput = ref("")
const addRules = reactive<FormRules>({
  petName: [{required: true, message: '请给宠物起一个名字', trigger: 'blur'}],
  petVariety: [{required: true, message: '请填写宠物品种', trigger: 'blur'}],
})
const openAddDialog = (): void => {
  addForm.value = examplePet()
  ageInput.value = ""
  addFormRef.value?.clearValidate()
  addVisible.value = true
}
// 头像上传回调
const changePhoto = (val: string): void => {
  addForm.value.petPhoto = val
}
// 输入年龄推算生日（带「约」为估算）
const onAgeInput = (): void => {
  const r = parseAgeToBirthday(ageInput.value)
  if (r) {
    addForm.value.petBirthday = r.birthday
    addForm.value.petBirthdayEstimated = r.estimated
  }
}
const submitAdd = (): void => {
  addFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    reqAddSelfPet(addForm.value).then(res => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '添加成功')
        addVisible.value = false
        reqClientPet().then(r => {
          petList.value = r
        })
      } else {
        ElMessage.error(res.msg || '添加失败')
      }
    })
  })
}

// 响应式列数：手机 1 列，平板 2 列，桌面 3 列
const descColumn = ref(3)
const updateColumn = () => {
  const w = window.innerWidth
  if (w < 576) descColumn.value = 1
  else if (w < 992) descColumn.value = 2
  else descColumn.value = 3
}

onMounted(() => {
  updateColumn()
  window.addEventListener('resize', updateColumn)
  reqClientPet().then(res => {
    petList.value = res
  })
  // 从宠物详情页(petOne)带病历id进入：自动打开该病历详情，并清掉URL参数防刷新重弹
  const rid = Number(route.query.recordId)
  if (rid) {
    openRecordDetail(rid)
    router.replace('/reception/clientCenter/clientPet')
  }
})
</script>

<style lang="scss" scoped>
@import url("/css/pa-card.css");

.pet-head {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

/* 添加宠物弹窗：头像区 */
.photo-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 4px 0 8px;

  .photo-tip {
    font-size: 12px;
    color: #909399;
  }
}

/* 就诊记录列表 */
.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all .2s;

  &:hover {
    border-color: #1E6FA8;
    box-shadow: 0 2px 10px rgba(30, 111, 168, .12);

    .link {
      color: #1E6FA8;
    }
  }

  .record-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    color: #606266;
    border-bottom: 1px dashed #e4e7ed;
    padding-bottom: 6px;
    margin-bottom: 6px;
  }

  .record-body {
    p {
      margin: 3px 0;
      font-size: 14px;
    }

    .muted {
      color: #909399;
      font-size: 13px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .record-foot {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 6px;
    font-size: 13px;

    .revisit {
      color: #e6a23c;
    }

    .fee {
      color: #f56c6c;
      font-weight: bold;
      margin-left: auto;
    }

    .link {
      color: #909399;
    }
  }
}

/* 覆盖 UpImg 组件默认的绝对定位（那是给老后台弹窗右上角设计的，会把头像浮到表单上面） */
:deep(.avatar-uploader) {
  position: static;
  left: auto;
  margin: 0;
  border-radius: 50%;

  .avatar {
    border-radius: 50%;
    object-fit: cover;
  }

  .el-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
  }
}

.my-row-2 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-areas: "left right right right";
  /* 在屏幕宽度小于992px时改变布局 */
  @media screen and (max-width: 992px) {
    grid-template-columns: 1fr;
    grid-template-areas: "left" "right";
  }

  .left {
    grid-area: left;
    text-align: center;

    img.my-avatar {
      border-radius: 50%;
      width: 128px;
      height: 128px;
    }
  }

  .right {
    grid-area: right;
    min-width: 0;
    overflow: hidden;
  }

  @media (max-width: 576px) {
    .left img.my-avatar {
      width: 96px;
      height: 96px;
    }
  }

  .lab-hd {
    font-size: 13px;
    color: #606266;
    margin: 4px 0 6px;

    .el-tag {
      margin-left: 6px;
    }
  }
}
</style>