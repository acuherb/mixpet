<template>
  <div>
    <!--顶部操作-->
    <div class="appoint-head">
      <el-button :icon="Plus" round type="primary" @click="openAddDialog">预约挂号</el-button>
    </div>
    <div v-for="a in appointList" :key="a.appointmentId" class="pa-card">
      <div class="card-tit">
        <span>单号：{{ a.appointmentId }}</span>
      </div>
      <el-descriptions :column="3" :title="'就诊时间：'+ moveT(a.appointmentDate)" border>
        <el-descriptions-item
            align="center"
            label="宠物"
            label-align="right">
          {{ a.petName }}
        </el-descriptions-item>
        <el-descriptions-item align="center" label="科室" label-align="right">
          <el-tag>{{ a.departmentName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item align="center" label="医生" label-align="right">
          {{ a.doctorName }}
        </el-descriptions-item>
        <el-descriptions-item align="center" label="描述" label-align="right">
          {{ a.appointmentInfo }}
        </el-descriptions-item>
        <el-descriptions-item align="center" label="挂号时间" label-align="right">
          {{ moveT(a.createTime || "") }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="!a.isSuc" class="card-ops">
        <el-button size="small" plain round type="primary" @click="openReschedule(a)">改期</el-button>
        <el-button size="small" plain round type="danger" @click="cancelAppoint(a)">取消预约</el-button>
      </div>
    </div>
    <el-empty v-if="appointList.length===0" description="暂无挂号，点上方「预约挂号」预约就诊"/>

    <!--改期弹窗-->
    <el-dialog v-model="rsVisible" title="改期" width="90%" style="max-width: 420px">
      <el-form label-width="90px">
        <el-form-item label="新的时间">
          <el-date-picker
              v-model="rsDate"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择新的就诊时间"
              style="width: 100%"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rsVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReschedule">确定改期</el-button>
      </template>
    </el-dialog>

    <!--预约挂号弹窗-->
    <el-dialog v-model="addVisible" title="预约挂号" width="90%" style="max-width: 480px">
      <el-alert v-if="originRecordId" :title="`复诊挂号（关联病历 #${originRecordId}）`" :closable="false" show-icon type="info" style="margin-bottom:10px"/>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-form-item label="宠物" prop="petId">
          <el-select v-model="addForm.petId" filterable placeholder="选择就诊宠物" style="width: 100%">
            <el-option v-for="p in appointablePets" :key="p.petId" :label="p.petName" :value="p.petId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="科室" prop="departmentId">
          <el-select v-model="addForm.departmentId" placeholder="选择科室" style="width: 100%"
                     @change="onDepartChange">
            <el-option v-for="d in departList" :key="d.roleId" :label="d.roleName" :value="d.roleId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="addForm.doctorId" filterable placeholder="先选科室，再选医生" style="width: 100%">
            <el-option v-for="d in doctorList" :key="d.roleId" :label="d.roleName" :value="d.roleId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="就诊时间" prop="appointmentDate">
          <el-date-picker
              v-model="addForm.appointmentDate"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择就诊时间"
              style="width: 100%"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="描述" prop="appointmentInfo">
          <el-input
              v-model="addForm.appointmentInfo"
              :rows="3"
              maxlength="64"
              placeholder="简单描述症状，如：食欲不振、呕吐"
              type="textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {AppointDto} from "@/model/DO/AppointDto";
import {moveT} from "@/utils/TimeUtil";
import {computed, onMounted, reactive, ref} from "vue";
import {useRoute} from "vue-router";
import {reqAddSelfAppoint, reqCancelSelfAppoint, reqClientAppoint, reqRescheduleSelfAppoint} from "@/request/AppointApi";
import {Appoint, exampleAppoint} from "@/model/entiy/Appoint";
import {Pet} from "@/model/entiy/Pet";
import {NameDo} from "@/model/DO/NameDo";
import {reqClientPet} from "@/request/PetApi";
import {reqDepartNameHaveDoctor} from "@/request/DepartApi";
import {reqDoctorNameByDepartId} from "@/request/DoctorApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Plus} from "@element-plus/icons-vue";

const appointList = ref<AppointDto[]>([])

/*=====预约挂号=====*/
const addVisible = ref(false)
const addFormRef = ref<FormInstance>()
const petList = ref<Pet[]>([])
// 只有审核通过（未停用）的宠物才能挂号
const appointablePets = computed(() => petList.value.filter(p => p.active !== false))
const departList = ref<NameDo[]>([])
const doctorList = ref<NameDo[]>([])
const addForm = ref<Appoint>(exampleAppoint())
const addRules = reactive<FormRules>({
  petId: [{required: true, message: '请选择宠物', trigger: 'change'}],
  departmentId: [{required: true, message: '请选择科室', trigger: 'change'}],
  doctorId: [{required: true, message: '请选择医生', trigger: 'change'}],
  appointmentDate: [{required: true, message: '请选择就诊时间', trigger: 'change'}],
  appointmentInfo: [{required: true, message: '请简单描述症状', trigger: 'blur'}],
})
const openAddDialog = (): void => {
  addForm.value = exampleAppoint()
  addForm.value.appointmentDate = null
  originRecordId.value = null
  addFormRef.value?.clearValidate()
  addVisible.value = true
}
// 切换科室后联动刷新医生列表
const onDepartChange = (departmentId: number): void => {
  addForm.value.doctorId = null
  doctorList.value = []
  reqDoctorNameByDepartId(departmentId).then(res => {
    doctorList.value = res
  })
}
const submitAdd = (): void => {
  addFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    reqAddSelfAppoint(addForm.value).then(res => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '预约成功')
        addVisible.value = false
        reqClientAppoint().then(r => {
          appointList.value = r
        })
      } else {
        ElMessage.error(res.msg || '预约失败')
      }
    })
  })
}

/*=====取消/改期=====*/
const refresh = (): void => {
  reqClientAppoint().then(r => {
    appointList.value = r
  })
}
// 取消待就诊预约
const cancelAppoint = (a: AppointDto): void => {
  ElMessageBox.confirm(`确定取消单号 #${a.appointmentId} 的预约吗？`, '取消预约', {
    confirmButtonText: '确认取消', cancelButtonText: '再想想', type: 'warning'
  }).then(() => {
    reqCancelSelfAppoint(a.appointmentId).then(res => {
      if (res.code === 1) {
        ElMessage.success('已取消')
        refresh()
      } else ElMessage.error(res.msg || '取消失败')
    })
  }).catch(() => ElMessage.info('已保留该预约'))
}
// 改期待就诊预约
const rsVisible = ref(false)
const rsDate = ref<string | null>(null)
const rsTarget = ref<AppointDto | null>(null)
const openReschedule = (a: AppointDto): void => {
  rsTarget.value = a
  rsDate.value = a.appointmentDate || null
  rsVisible.value = true
}
const submitReschedule = (): void => {
  if (!rsTarget.value || !rsDate.value) {
    ElMessage.warning('请选择新的就诊时间')
    return
  }
  reqRescheduleSelfAppoint(rsTarget.value.appointmentId, rsDate.value).then(res => {
    if (res.code === 1) {
      ElMessage.success('改期成功')
      rsVisible.value = false
      refresh()
    } else ElMessage.error(res.msg || '改期失败')
  })
}

const route = useRoute()
const originRecordId = ref<number | null>(null)

onMounted(() => {
  reqClientAppoint().then(res => {
    appointList.value = res
  })
  // 名下宠物（挂号用）与有医生的科室
  reqClientPet().then(res => {
    petList.value = res
  })
  reqDepartNameHaveDoctor(null).then(res => {
    departList.value = res
  })
  // 从病历详情"预约复诊"跳转过来：带初诊病历id，自动打开预约弹窗
  const orid = Number(route.query.originRecordId)
  if (orid) {
    originRecordId.value = orid
    addForm.value = exampleAppoint()
    addForm.value.appointmentDate = null
    addForm.value.originRecordId = orid
    addVisible.value = true
  }
})
</script>

<style lang="scss" scoped>
@import url("/css/pa-card.css");

.appoint-head {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.card-ops {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
