<template>
  <!--顶部：状态筛选 + 入院登记-->
  <el-card class="filter-card" shadow="never">
    <el-select v-model="statusFilter" placeholder="全部状态" size="large" style="width: 160px" clearable @change="nowQuery">
      <el-option label="住院中" :value="false"/>
      <el-option label="已出院" :value="true"/>
    </el-select>
    <span class="tip">出院时按住院天数×日均费计入账单，随收费列表一起结算</span>
    <el-button type="primary" size="large" style="float: right" @click="addRoleB">入院登记</el-button>
  </el-card>

  <!--列表展示-->
  <el-table :data="list.records"
            max-height="calc(100vh - 200px)"
            row-key="hospitalId"
            stripe>
    <el-table-column label="床位" prop="roomNo" width="100"/>
    <el-table-column label="宠物" min-width="100" prop="petName" show-overflow-tooltip/>
    <el-table-column label="客户" min-width="110" prop="clientName" show-overflow-tooltip/>
    <el-table-column label="主治医生" min-width="90" prop="doctorName" show-overflow-tooltip/>
    <el-table-column label="诊断" min-width="140" prop="diagnosis" show-overflow-tooltip/>
    <el-table-column label="入院时间" width="160" prop="inTime"/>
    <el-table-column label="日均费(元)" prop="dailyFee" width="100"/>
    <el-table-column label="天数" prop="days" width="70"/>
    <el-table-column label="状态" width="90">
      <template #default="scope">
        <el-tag v-if="scope.row.status" size="small" type="success">已出院</el-tag>
        <el-tag v-else size="small" type="warning">住院中</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="180" fixed="right">
      <template #default="scope">
        <el-button v-if="!scope.row.status" size="small" type="primary" plain @click="dischargeOne(scope.row)">出院结算</el-button>
        <el-button v-else size="small" type="info" plain @click="showDetail(scope.row)">查看</el-button>
        <el-button size="small" plain round @click="openMonitor(scope.row)">监护</el-button>
      </template>
    </el-table-column>
  </el-table>

  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>

  <!--入院登记弹窗-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="500px">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="病历号" prop="recordId">
        <el-input-number v-model="form.recordId" :min="1" style="width: 100%" placeholder="输入本次就诊病历id"/>
      </el-form-item>
      <el-form-item label="床位号">
        <el-input v-model="form.roomNo" clearable maxlength="32" placeholder="如：住院部A-01"/>
      </el-form-item>
      <el-form-item label="日均费(元)" prop="dailyFee">
        <el-input-number v-model="form.dailyFee" :max="99999" :min="0" :precision="2" :step="10" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.note" :rows="2" maxlength="255" placeholder="入院原因/注意事项" type="textarea"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="modalView = false">取消</el-button>
      <el-button type="primary" @click="formSubmit">确认入院</el-button>
    </template>
  </el-dialog>

  <!--住院监护抽屉：新增(住院中)+历史-->
  <el-drawer v-model="monitorView" :title="monitorTitle" size="560px">
    <el-form v-if="monitorRow && !monitorRow.status" :model="monitorForm" label-width="84px" style="margin-bottom:12px">
      <el-form-item label="记录时间">
        <el-date-picker v-model="monitorForm.monitorTime" format="YYYY-MM-DD HH:mm" placeholder="默认现在"
                        style="width:100%" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"/>
      </el-form-item>
      <el-form-item label="体温(℃)">
        <el-input-number v-model="monitorForm.temperature" :max="45" :min="30" :precision="1" :step="0.1" style="width:100%"/>
      </el-form-item>
      <el-form-item label="心率">
        <el-input-number v-model="monitorForm.heartRate" :max="400" :min="0" :step="5" style="width:100%"/>
      </el-form-item>
      <el-form-item label="呼吸">
        <el-input-number v-model="monitorForm.respiratoryRate" :max="200" :min="0" :step="2" style="width:100%"/>
      </el-form-item>
      <el-form-item label="体重(kg)">
        <el-input-number v-model="monitorForm.weight" :max="200" :min="0" :precision="2" :step="0.1" style="width:100%"/>
      </el-form-item>
      <el-form-item label="精神/状态">
        <el-input v-model="monitorForm.mentalState" maxlength="64" placeholder="如：活泼 / 沉郁"/>
      </el-form-item>
      <el-form-item label="食欲/饮食">
        <el-input v-model="monitorForm.appetite" maxlength="128" placeholder="如：主动进食 / 拒食"/>
      </el-form-item>
      <el-form-item label="排便排尿">
        <el-input v-model="monitorForm.excretion" maxlength="128" placeholder="如：正常 / 腹泻"/>
      </el-form-item>
      <el-form-item label="当日用药">
        <el-input v-model="monitorForm.medication" :rows="2" maxlength="255" type="textarea"/>
      </el-form-item>
      <el-form-item label="处置/治疗">
        <el-input v-model="monitorForm.treatment" :rows="2" maxlength="255" type="textarea"/>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="monitorForm.note" :rows="2" maxlength="255" type="textarea"/>
      </el-form-item>
      <el-form-item>
        <el-button round type="primary" @click="submitMonitor">添加监护记录</el-button>
      </el-form-item>
    </el-form>
    <el-divider content-position="left">监护历史</el-divider>
    <el-timeline>
      <el-timeline-item v-for="m in monitors" :key="m.monitorId" :timestamp="moveT(m.monitorTime || '')" placement="top">
        <div><b>体温</b> {{ m.temperature ?? '—' }}℃ · <b>心率</b> {{ m.heartRate ?? '—' }} · <b>呼吸</b> {{ m.respiratoryRate ?? '—' }} · <b>体重</b> {{ m.weight ?? '—' }}kg</div>
        <div v-if="m.mentalState">精神：{{ m.mentalState }}</div>
        <div v-if="m.appetite">食欲：{{ m.appetite }}</div>
        <div v-if="m.excretion">排便排尿：{{ m.excretion }}</div>
        <div v-if="m.medication">用药：{{ m.medication }}</div>
        <div v-if="m.treatment">处置：{{ m.treatment }}</div>
        <div v-if="m.note">备注：{{ m.note }}</div>
        <div style="color:#909399;font-size:12px">记录人：{{ m.recorder || '—' }}</div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="monitors.length===0" description="暂无监护记录"/>
  </el-drawer>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {useRoute} from 'vue-router'
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {HospitalDto} from "@/model/DO/HospitalDto";
import {HospitalVo} from "@/model/VO/HospitalVo";
import {Page} from "@/model/DO/Page";
import {reqAddHospitalMonitor, reqDischargeHospital, reqHospitalMonitorList, reqHospitalPage, reqSaveHospital} from "@/request/HospitalApi";
import {HospitalMonitor} from "@/model/entiy/HospitalMonitor";
import {moveT} from "@/utils/TimeUtil";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

/**
 ┌───────────────────────────────────┐
 │=============列表查询相关===========│
 └───────────────────────────────────┘
 */
const qp = ref<{ numPage: number; pageSize: number; status: boolean | null }>({numPage: 1, pageSize: 15, status: null})
const statusFilter = ref<boolean | null>(null)
const list = ref<Page<HospitalDto>>({records: [], total: 0})

const changePuB = (val: PageQuery): void => {
  qp.value.numPage = val.numPage
  qp.value.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  qp.value.status = statusFilter.value
  reqHospitalPage(qp.value).then(res => {
    list.value = res
  })
}
const nowQuery = (): void => {
  qp.value.numPage = 1
  sendQuery()
}

/**
 ┌───────────────────────────────────┐
 │=============入院登记相关===========│
 └───────────────────────────────────┘
 */
const modalView = ref(false)
const modalTit = ref('入院登记')
const form = ref<HospitalVo>({recordId: 0, roomNo: '', dailyFee: 50, note: ''})
const myFormRef = ref<FormInstance>()

const addRoleB = (): void => {
  form.value = {recordId: 0, roomNo: '', dailyFee: 50, note: ''}
  myFormRef.value?.resetFields()
  modalView.value = true
}
const formSubmit = (): void => {
  myFormRef.value?.validate((valid: boolean) => {
    if (valid) {
      reqSaveHospital(form.value).then((res: Res) => {
        modalView.value = false
        if (res.code === 1) {
          ElMessage.success(res.msg || '入院登记成功')
          sendQuery()
        }
      })
    }
  })
}
const rules = reactive<FormRules>({
  "recordId": [{required: true, type: 'number', message: '请输入病历号', trigger: 'blur'}],
  "dailyFee": [{required: true, message: '请输入日均费', trigger: 'blur'}]
})

/**
 ┌───────────────────────────────────┐
 │=============出院结算与查看========═│
 └───────────────────────────────────┘
 */
const dischargeOne = (row: HospitalDto): void => {
  ElMessageBox.confirm(
      `确认对 ${row.petName}（${row.clientName}）办理出院？住院费将自动计入账单待收费。`,
      '出院结算',
      {confirmButtonText: '结算', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDischargeHospital(row.hospitalId).then((res: Res) => {
      if (res.code === 1) {
        ElMessage.success(res.msg || '结算成功')
        sendQuery()
      }
    })
  }).catch(() => ElMessage.info('取消出院'))
}
const showDetail = (row: HospitalDto): void => {
  ElMessageBox.alert(
      `住院 ${row.days ?? 0} 天，日均费 ￥${row.dailyFee ?? 0}，共住 ￥${((row.days ?? 0) * (row.dailyFee ?? 0)).toFixed(2)}。<br/>入院：${row.inTime}，出院：${row.outTime}<br/>备注：${row.note || '无'}`,
      `住院详情 #${row.hospitalId}`,
      {confirmButtonText: '知道了', dangerouslyUseHTMLString: true}
  )
}

/**
 ┌───────────────────────────────────┐
 │=============住院监护==============│
 └───────────────────────────────────┘
 */
const monitorView = ref(false)
const monitorTitle = ref('住院监护')
const monitorRow = ref<HospitalDto | null>(null)
const monitors = ref<HospitalMonitor[]>([])
const monitorForm = ref<Partial<HospitalMonitor>>({})
const openMonitor = (row: HospitalDto): void => {
  monitorRow.value = row
  monitorTitle.value = `住院监护 - ${row.petName}（${row.roomNo || '未分配床位'}）`
  monitorForm.value = {hospitalId: row.hospitalId}
  monitors.value = []
  monitorView.value = true
  loadMonitors(row.hospitalId)
}
const loadMonitors = (hospitalId: number): void => {
  reqHospitalMonitorList(hospitalId).then(res => {
    monitors.value = res || []
  })
}
const submitMonitor = (): void => {
  if (!monitorRow.value) return
  const hid = monitorRow.value.hospitalId
  reqAddHospitalMonitor(monitorForm.value as HospitalMonitor).then((res: Res) => {
    if (res.code === 1) {
      ElMessage.success('监护记录已添加')
      monitorForm.value = {hospitalId: hid}
      loadMonitors(hid)
    } else ElMessage.error(res.msg || '添加失败')
  })
}

/** 生命周期 */
const route = useRoute()
onMounted(() => {
  sendQuery()
  // 从就诊汇总跳转过来：预填病历号并打开入院登记
  const rid = Number(route.query.recordId)
  if (rid) {
    form.value = {recordId: rid, roomNo: '', dailyFee: 50, note: ''}
    modalView.value = true
  }
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 8px;
}
.tip {
  margin-left: 16px;
  font-size: 12px;
  color: #909399;
}
</style>