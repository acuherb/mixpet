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
    <el-table-column label="操作" width="240" fixed="right">
      <template #default="scope">
        <el-button v-if="!scope.row.status" size="small" type="primary" plain @click="dischargeOne(scope.row)">出院结算</el-button>
        <el-button v-else size="small" type="info" plain @click="showDetail(scope.row)">查看</el-button>
        <el-button size="small" plain round @click="openMonitor(scope.row)">监护</el-button>
        <el-button v-if="!scope.row.status" size="small" plain round type="warning" @click="openAdvice(scope.row)">医嘱</el-button>
      </template>
    </el-table-column>
  </el-table>

  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>

  <!--入院登记弹窗-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="500px">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="病历号" prop="recordId">
        <el-select v-model="form.recordId" filterable clearable placeholder="选择本次就诊病历" style="width: 100%">
          <el-option v-for="r in recordOptions" :key="r.recordId"
                     :label="`#${r.recordId} ${r.petName}（${r.clientName}）`" :value="r.recordId"/>
        </el-select>
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

  <!-- 住院医嘱抽屉：下达/从处方导入/停嘱/完成 -->
  <el-drawer v-model="adviceView" :title="adviceTitle" size="640px">
    <template #default>
      <!-- 从本病历处方导入（与处方联动） -->
      <div class="adv-import">
        <el-button size="small" plain round type="success" :loading="importLoading" @click="loadRxImport">
          <el-icon style="margin-right:4px"><Plus/></el-icon>从本病历处方导入
        </el-button>
        <span class="tip">把本次就诊已开处方快速转为住院医嘱（药品/剂量自动带入）</span>
      </div>
      <!-- 处方导入勾选区 -->
      <div v-if="importList.length" class="adv-import-list">
        <el-checkbox-group v-model="importChecks">
          <div v-for="(p, i) in importList" :key="i" class="imp-row">
            <el-checkbox :value="i">
              {{ p.drugName }}（{{ p.drugSpec || '—' }}） 数量{{ p.quantity }}{{ p.drugUnit }} · {{ p.usageInfo || '—' }}
            </el-checkbox>
          </div>
        </el-checkbox-group>
        <div style="text-align:right;margin-top:8px">
          <el-button size="small" @click="importList = []">取消</el-button>
          <el-button size="small" type="primary" :loading="importSaving" @click="submitImport">导入所选（{{ importChecks.length }}）</el-button>
        </div>
      </div>

      <!-- 下达医嘱表单 -->
      <el-form v-if="adviceRow && !adviceRow.status" :model="adviceForm" label-width="76px" style="margin:12px 0">
        <el-form-item label="类型">
          <el-radio-group v-model="adviceForm.itemType">
            <el-radio :value="1">药品</el-radio>
            <el-radio :value="2">治疗</el-radio>
            <el-radio :value="3">护理</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="项目">
          <el-select v-if="adviceForm.itemType === 1" v-model="adviceForm.drugId" filterable clearable
                     placeholder="选择药品（自动带名称/单位）" style="width:100%" @change="onAdviceDrugChange">
            <el-option v-for="d in drugList" :key="d.drugId" :label="`${d.drugName}（￥${d.drugPrice}/${d.drugUnit}）`" :value="d.drugId"/>
          </el-select>
          <el-input v-else v-model="adviceForm.itemName" clearable maxlength="64" placeholder="如：皮下补液/每日创口换药"/>
        </el-form-item>
        <div style="display:flex;gap:10px">
          <el-form-item label="单次剂量" style="flex:1">
            <el-input v-model="adviceForm.dose" clearable maxlength="16" placeholder="如：0.5"/>
          </el-form-item>
          <el-form-item label="单位" style="width:120px">
            <el-input v-model="adviceForm.doseUnit" clearable maxlength="8" placeholder="片/ml"/>
          </el-form-item>
          <el-form-item label="频次" style="width:150px">
            <el-select v-model="adviceForm.frequency" clearable placeholder="选择" style="width:100%">
              <el-option v-for="f in FREQS" :key="f" :label="f" :value="f"/>
            </el-select>
          </el-form-item>
        </div>
        <div style="display:flex;gap:10px">
          <el-form-item label="途径" style="width:150px">
            <el-select v-model="adviceForm.route" clearable placeholder="选择" style="width:100%">
              <el-option v-for="r in ROUTES" :key="r" :label="r" :value="r"/>
            </el-select>
          </el-form-item>
          <el-form-item label="天数" style="width:130px">
            <el-input-number v-model="adviceForm.days" :min="1" :max="365" controls-position="right" style="width:100%"/>
          </el-form-item>
          <el-form-item label="开始日期" style="flex:1">
            <el-date-picker v-model="adviceForm.startDate" format="YYYY-MM-DD" placeholder="默认今天"
                            style="width:100%" type="date" value-format="YYYY-MM-DD"/>
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="adviceForm.remark" :rows="2" clearable maxlength="255" placeholder="注意事项/观察点" type="textarea"/>
        </el-form-item>
        <el-form-item>
          <el-button round type="primary" :loading="adviceSaving" @click="submitAdvice">下达医嘱</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-else title="该住院已出院，医嘱只读（不可再下达）" type="info" show-icon :closable="false" style="margin:12px 0"/>

      <el-divider content-position="left">医嘱单（{{ advices.length }} 条）</el-divider>
      <el-table :data="advices" border size="small">
        <el-table-column label="项目" min-width="130">
          <template #default="s">
            {{ s.row.itemName }}
            <el-tag size="small" type="info" effect="plain" style="margin-left:4px">
              {{ ({1: '药品', 2: '治疗', 3: '护理'} as Record<number, string>)[s.row.itemType!] || '其他' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剂量" width="100">
          <template #default="s">{{ s.row.dose ? s.row.dose + (s.row.doseUnit || '') : '—' }}</template>
        </el-table-column>
        <el-table-column label="频次/途径" min-width="130" show-overflow-tooltip>
          <template #default="s">{{ s.row.frequency || '—' }}<span v-if="s.row.route"> · {{ s.row.route }}</span></template>
        </el-table-column>
        <el-table-column label="天数" width="60">
          <template #default="s">{{ s.row.days ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="88">
          <template #default="s">
            <el-tag v-if="s.row.status === 4" size="small" type="success">已完成</el-tag>
            <el-tag v-else-if="s.row.status === 3" size="small" type="info">已停嘱</el-tag>
            <el-tag v-else-if="s.row.status === 2" size="small" type="warning">执行中</el-tag>
            <el-tag v-else size="small" type="danger">待执行</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="s">
            <el-button v-if="s.row.status === 1 || s.row.status === 2" size="small" type="primary" link @click="doneAdvice(s.row)">完成</el-button>
            <el-button v-if="s.row.status === 1 || s.row.status === 2" size="small" type="warning" link @click="stopAdvice(s.row)">停嘱</el-button>
            <span v-if="s.row.status === 3" style="color:#909399;font-size:12px" :title="s.row.stopReason || ''">
              停嘱：{{ s.row.stopReason || '—' }}
            </span>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无医嘱，点上方下达或从处方导入"/></template>
      </el-table>
    </template>
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
import {reqAddAdvice, reqAddAdviceBatch, reqAdviceByHospital, reqDoneAdvice, reqStopAdvice} from "@/request/HospitalAdviceApi";
import {HospitalAdvice} from "@/model/entiy/HospitalAdvice";
import {Prescription} from "@/model/entiy/MedicalRecord";
import {Drug} from "@/model/entiy/Drug";
import {reqDrugList} from "@/request/DrugApi";
import {reqRecordPage, reqRecordOne} from "@/request/RecordApi";
import {RecordDto} from "@/model/DO/RecordDto";
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

// 病历下拉（复用病例检索接口，避免手填填错串档）
const recordOptions = ref<RecordDto[]>([])
const loadRecordOptions = (): void => {
  if (recordOptions.value.length) return
  reqRecordPage({numPage: 1, pageSize: 100, petName: '', clientName: '', doctorName: '', diagnosis: '', startTime: '', endTime: ''})
      .then(res => { recordOptions.value = res?.records || [] })
}
const addRoleB = (): void => {
  form.value = {recordId: 0, roomNo: '', dailyFee: 50, note: ''}
  myFormRef.value?.resetFields()
  loadRecordOptions()
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

/**
 ┌───────────────────────────────────┐
 │=============住院医嘱单=============│
 └───────────────────────────────────┘
 */
const FREQS = ['QD 每日一次', 'BID 每日两次', 'TID 每日三次', 'QID 每日四次', '持续', '必要时']
const ROUTES = ['口服', '皮下注射', '肌肉注射', '静脉', '外用']

const adviceView = ref(false)
const adviceTitle = ref('住院医嘱')
const adviceRow = ref<HospitalDto | null>(null)
const advices = ref<HospitalAdvice[]>([])
const adviceForm = ref<Partial<HospitalAdvice>>({itemType: 1, frequency: 'BID 每日两次', route: '口服'})
const adviceSaving = ref(false)
const drugList = ref<Drug[]>([])

const openAdvice = (row: HospitalDto): void => {
  adviceRow.value = row
  adviceTitle.value = `住院医嘱 - ${row.petName}（${row.roomNo || '未分配床位'}）`
  adviceForm.value = {itemType: 1, frequency: 'BID 每日两次', route: '口服'}
  importList.value = []
  importChecks.value = []
  advices.value = []
  adviceView.value = true
  loadAdvices(row.hospitalId)
  if (drugList.value.length === 0) reqDrugList().then(res => { drugList.value = res || [] })
}
const loadAdvices = (hospitalId: number): void => {
  reqAdviceByHospital(hospitalId).then(res => { advices.value = res || [] })
}
// 选药品自动带名称/单位
const onAdviceDrugChange = (drugId: number | null): void => {
  const d = drugList.value.find(x => x.drugId === drugId)
  if (d) {
    adviceForm.value.drugId = d.drugId
    adviceForm.value.itemName = d.drugName
    adviceForm.value.doseUnit = d.drugUnit
  } else {
    adviceForm.value.itemName = ''
  }
}
const submitAdvice = (): void => {
  if (!adviceRow.value) return
  const f = adviceForm.value
  if (!f.itemName) { ElMessage.warning('请填写/选择医嘱项目'); return }
  adviceSaving.value = true
  reqAddAdvice({
    hospitalId: adviceRow.value.hospitalId,
    recordId: adviceRow.value.recordId,
    itemType: f.itemType ?? 1,
    drugId: f.drugId ?? null,
    itemName: f.itemName!,
    dose: f.dose ?? null,
    doseUnit: f.doseUnit ?? null,
    frequency: f.frequency ?? null,
    route: f.route ?? null,
    days: f.days ?? null,
    startDate: f.startDate ?? null,
    remark: f.remark ?? null
  }).then(res => {
    adviceSaving.value = false
    if (res.code === 1) {
      ElMessage.success('医嘱已下达')
      adviceForm.value = {itemType: f.itemType ?? 1, frequency: 'BID 每日两次', route: '口服'}
      loadAdvices(adviceRow.value!.hospitalId)
    } else ElMessage.error(res.msg || '下达失败')
  }).catch(() => { adviceSaving.value = false })
}

/** 从本病历处方导入（与处方联动） */
const importLoading = ref(false)
const importSaving = ref(false)
const importList = ref<Prescription[]>([])
const importChecks = ref<number[]>([])
const loadRxImport = (): void => {
  if (!adviceRow.value) return
  importLoading.value = true
  reqRecordOne(adviceRow.value.recordId).then(res => {
    importLoading.value = false
    const ps = res?.prescriptions || []
    if (!ps.length) { ElMessage.info('本病历暂未开处方'); return }
    importList.value = ps
    importChecks.value = ps.map((_, i) => i)
  }).catch(() => { importLoading.value = false; ElMessage.error('拉取处方失败') })
}
const submitImport = (): void => {
  if (!adviceRow.value || !importChecks.value.length) return
  const row = adviceRow.value
  const today = new Date().toISOString().slice(0, 10)
  const list: HospitalAdvice[] = importChecks.value.map(i => {
    const p = importList.value[i]
    return {
      hospitalId: row.hospitalId,
      recordId: row.recordId,
      itemType: 1,
      drugId: p.drugId ?? null,
      itemName: p.drugName ?? '',
      dose: p.singleDose != null ? String(p.singleDose) : null,
      doseUnit: p.singleDoseUnit ?? null,
      frequency: p.dailyTimes ? `每日${p.dailyTimes}次` : null,
      route: p.usageInfo || null,
      days: p.days ?? null,
      startDate: today,
      remark: `由本病历处方导入（数量${p.quantity}${p.drugUnit || ''}）`
    }
  })
  importSaving.value = true
  reqAddAdviceBatch(list).then(res => {
    importSaving.value = false
    if (res.code === 1) {
      ElMessage.success(`已导入 ${list.length} 条医嘱`)
      importList.value = []
      importChecks.value = []
      loadAdvices(row.hospitalId)
    } else ElMessage.error(res.msg || '导入失败')
  }).catch(() => { importSaving.value = false })
}

/** 停嘱 / 完成 */
const stopAdvice = (row: HospitalAdvice): void => {
  ElMessageBox.prompt('填写停嘱原因（医疗留痕，必填）：', '停嘱', {
    confirmButtonText: '确认停嘱', cancelButtonText: '取消',
    inputType: 'textarea', inputPlaceholder: '如：病情好转/出现不良反应/转治疗方案',
    inputPattern: /\S+/, inputErrorMessage: '请填写停嘱原因'
  }).then(({value}) => {
    reqStopAdvice(row.adviceId!, value as string).then(res => {
      if (res.code === 1) { ElMessage.success('已停嘱'); loadAdvices(row.hospitalId) }
      else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}
const doneAdvice = (row: HospitalAdvice): void => {
  reqDoneAdvice(row.adviceId!).then(res => {
    if (res.code === 1) { ElMessage.success('已完成'); loadAdvices(row.hospitalId) }
    else ElMessage.error(res.msg || '操作失败')
  })
}
</script>

<style lang="scss" scoped>
.filter-card {
  margin-bottom: 8px;
}
.tip {
  margin-left: 16px;
  font-size: 12px;
  color: #909399;
}

/* 医嘱：处方导入区 */
.adv-import {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;

  .tip { margin-left: 8px; }
}
.adv-import-list {
  border: 1px dashed #b3d8a8;
  background: #f6fbf3;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;

  .imp-row { padding: 3px 0; font-size: 13px; }
}
</style>