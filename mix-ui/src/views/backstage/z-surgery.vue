<template>
  <div>
    <!--操作框：新增 + 状态筛选-->
    <BackOpCol role="手术" @addRole="addRoleB" @query="nowQuery">
      <el-select v-model="statusFilter" clearable placeholder="全部状态" size="large" style="width: 140px" @change="nowQuery">
        <el-option label="待手术" :value="0"/>
        <el-option label="手术中" :value="1"/>
        <el-option label="已完成" :value="2"/>
        <el-option label="已取消" :value="3"/>
      </el-select>
    </BackOpCol>
    <!--列表展示-->
    <el-table :data="list.records" max-height="calc(100vh - 200px)" row-key="surgeryId" stripe>
      <el-table-column label="宠物" min-width="100" prop="petName" show-overflow-tooltip/>
      <el-table-column label="客户" min-width="100" prop="clientName" show-overflow-tooltip/>
      <el-table-column label="诊断" min-width="120" prop="diagnosis" show-overflow-tooltip/>
      <el-table-column label="手术名称" min-width="140" prop="surgeryName" show-overflow-tooltip/>
      <el-table-column label="类型" width="80" prop="surgeryType"/>
      <el-table-column label="麻醉" width="100" prop="anesthesiaType"/>
      <el-table-column label="主刀" width="90" prop="surgeon" show-overflow-tooltip/>
      <el-table-column label="预约时间" width="160" prop="scheduledTime">
        <template #default="scope">{{ fmt(scope.row.scheduledTime) }}</template>
      </el-table-column>
      <el-table-column label="费用(元)" width="90" prop="fee"/>
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="surgeryStatusType(scope.row.status)" size="small">{{ surgeryStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="245">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openRecord(scope.row)">记录</el-button>
          <el-button size="small" plain type="warning" @click="openConsent(scope.row)">同意书</el-button>
          <el-button size="small" plain type="success" @click="printSheet(scope.row)">记录单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!--分页条-->
    <BackPage :total="list.total" @changePu="changePuB"/>

    <!--新建手术单弹窗-->
    <el-dialog v-model="createView" title="新建手术单" width="560px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="选择病历" prop="recordId">
          <el-select v-model="createForm.recordId" filterable placeholder="选择本次就诊病历（确定宠物）" style="width: 100%">
            <el-option v-for="r in records" :key="r.recordId" :label="recordLabel(r)" :value="r.recordId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="手术名称" prop="surgeryName">
          <el-input v-model="createForm.surgeryName" clearable maxlength="128" placeholder="如：绝育手术（母）"/>
        </el-form-item>
        <el-form-item label="手术类型" prop="surgeryType">
          <el-select v-model="createForm.surgeryType" filterable placeholder="选择类型（在系统设置维护）">
            <el-option v-for="t in surgeryTypeOpts" :key="t.dictId" :label="t.dictKey" :value="t.dictKey"/>
          </el-select>
        </el-form-item>
        <el-form-item label="麻醉方式" prop="anesthesiaType">
          <el-select v-model="createForm.anesthesiaType" placeholder="选择麻醉方式（在系统设置维护）">
            <el-option v-for="a in anesthesiaTypeOpts" :key="a.dictId" :label="a.dictKey" :value="a.dictKey"/>
          </el-select>
        </el-form-item>
        <el-form-item label="预约时间">
          <el-date-picker v-model="createForm.scheduledTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          format="YYYY-MM-DD HH:mm" placeholder="选择预约手术时间" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="主刀医生">
          <el-input v-model="createForm.surgeon" clearable maxlength="64" placeholder="主刀（可空）"/>
        </el-form-item>
        <el-form-item label="助手">
          <el-input v-model="createForm.assistant" clearable maxlength="64" placeholder="助手（可空）"/>
        </el-form-item>
        <el-form-item label="手术费(元)" prop="fee">
          <el-input-number v-model="createForm.fee" :max="99999" :min="0" :precision="2" :step="50" style="width: 100%"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createView = false">取消</el-button>
        <el-button type="primary" @click="createSubmit">确认安排</el-button>
      </template>
    </el-dialog>

    <!--手术/麻醉记录弹窗-->
    <el-dialog v-model="recordView" title="手术 / 麻醉记录" width="680px" top="4vh">
      <el-scrollbar max-height="70vh">
        <div v-if="current" class="rec-head">
          {{ current.surgeryName }} · {{ current.petName }}（{{ current.clientName }}）
        </div>
        <el-form label-width="110px">
          <el-divider content-position="left">手术记录</el-divider>
          <el-form-item label="状态">
            <el-select v-model="rec.status" style="width: 160px">
              <el-option label="待手术" :value="0"/>
              <el-option label="手术中" :value="1"/>
              <el-option label="已完成" :value="2"/>
              <el-option label="已取消" :value="3"/>
            </el-select>
          </el-form-item>
          <el-form-item label="实际时间">
            <el-date-picker v-model="rec.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" placeholder="开始" style="width: 46%"/>
            <span style="margin: 0 8px">至</span>
            <el-date-picker v-model="rec.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" placeholder="结束" style="width: 46%"/>
          </el-form-item>
          <el-form-item label="主刀 / 助手">
            <el-input v-model="rec.surgeon" clearable placeholder="主刀" style="width: 46%"/>
            <el-input v-model="rec.assistant" clearable placeholder="助手" style="width: 46%; margin-left: 8%"/>
          </el-form-item>
          <el-form-item label="术前评估/准备">
            <el-input v-model="rec.preopNote" :rows="2" maxlength="500" type="textarea" placeholder="术前检查、禁食、风险评估等"/>
          </el-form-item>
          <el-form-item label="术中记录">
            <el-input v-model="rec.intraopNote" :rows="3" maxlength="1000" type="textarea" placeholder="术中所见、操作过程"/>
          </el-form-item>
          <el-form-item label="术后记录/医嘱">
            <el-input v-model="rec.postopNote" :rows="3" maxlength="1000" type="textarea" placeholder="术后情况、恢复建议、复诊"/>
          </el-form-item>

          <el-divider content-position="left">麻醉记录</el-divider>
          <el-form-item label="麻醉方式">
            <el-select v-model="anesthesia.anesthesiaType" filterable placeholder="选择或输入" style="width: 200px">
              <el-option v-for="a in anesthesiaTypeOpts" :key="a.dictId" :label="a.dictKey" :value="a.dictKey"/>
            </el-select>
          </el-form-item>
          <el-form-item label="麻醉师">
            <el-input v-model="anesthesia.anesthetist" clearable maxlength="64" placeholder="麻醉师" style="width: 200px"/>
          </el-form-item>
          <el-form-item label="麻醉时间">
            <el-date-picker v-model="anesthesia.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" placeholder="开始" style="width: 46%"/>
            <span style="margin: 0 8px">至</span>
            <el-date-picker v-model="anesthesia.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" placeholder="结束" style="width: 46%"/>
          </el-form-item>
          <el-form-item label="麻醉用药">
            <el-input v-model="anesthesia.drugUsed" clearable maxlength="255" placeholder="如：丙泊酚 + 异氟烷"/>
          </el-form-item>
          <el-form-item label="生命体征监护">
            <el-input v-model="anesthesia.vitals" :rows="2" maxlength="500" type="textarea" placeholder="心率/呼吸/体温/血氧等监护记录"/>
          </el-form-item>
          <el-form-item label="备注/异常">
            <el-input v-model="anesthesia.note" :rows="2" maxlength="500" type="textarea" placeholder="麻醉异常及处理"/>
          </el-form-item>
        </el-form>
      </el-scrollbar>
      <template #footer>
        <el-button @click="recordView = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="recordSubmit">保存记录</el-button>
      </template>
    </el-dialog>

    <!--手术/麻醉知情同意书弹窗-->
    <el-dialog v-model="consentView" title="手术 / 麻醉知情同意书" width="580px">
      <div v-if="consentRow" class="rec-head">{{ consentRow.surgeryName }} · {{ consentRow.petName }}（{{ consentRow.clientName }}）</div>
      <el-form label-width="90px">
        <el-form-item label="风险告知">
          <el-input v-model="consentForm.risks" :rows="5" maxlength="1000" placeholder="麻醉与手术风险、可能并发症、术后注意事项等" type="textarea"/>
        </el-form-item>
        <el-form-item label="畜主同意">
          <el-switch v-model="consentForm.agree" active-text="同意手术及麻醉" inactive-text="不同意"/>
        </el-form-item>
        <el-form-item label="签字人">
          <el-input v-model="consentForm.signer" clearable maxlength="64" placeholder="畜主本人签字（姓名）"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="consentForm.note" :rows="2" maxlength="255" type="textarea"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="consentView = false">取消</el-button>
        <el-button type="warning" @click="printConsent">打印同意书</el-button>
        <el-button :loading="consentSaving" type="primary" @click="consentSubmit">确认签署</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import BackOpCol from "@/components/row/BackOpCol.vue";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {Page} from "@/model/DO/Page";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Surgery, surgeryStatusText, surgeryStatusType, SurgeryVo} from "@/model/entiy/Surgery";
import {AnesthesiaRecord} from "@/model/entiy/AnesthesiaRecord";
import {reqDictByType} from "@/request/DictApi";
import {SysDict} from "@/model/entiy/SysDict";

// 手术类型/麻醉方式字典（来自系统设置）
const surgeryTypeOpts = ref<SysDict[]>([])
const anesthesiaTypeOpts = ref<SysDict[]>([])
reqDictByType('surgery_type').then(r => { surgeryTypeOpts.value = r || [] })
reqDictByType('anesthesia_type').then(r => { anesthesiaTypeOpts.value = r || [] })
import {RecordDto} from "@/model/DO/RecordDto";
import {reqDoctorRecords, reqLabByRecord} from "@/request/LabApi";
import {reqConsentOne, reqCreateSurgery, reqSaveAnesthesia, reqSignConsent, reqSurgeryOne, reqSurgeryPage, reqUpdateSurgery} from "@/request/SurgeryApi";

// 术前必查项目（如需调整为可在系统设置维护）
const PRE_OP_CHECKS = ['血常规', '生化', '凝血']

const route = useRoute()
const qp = reactive({numPage: 1, pageSize: 20, status: null as number | null})
const statusFilter = ref<number | null>(null)
const list = ref<Page<Surgery>>({records: [], total: 0})

const changePuB = (val: PageQuery): void => { qp.numPage = val.numPage; qp.pageSize = val.pageSize; sendQuery() }
const sendQuery = (): void => {
  qp.status = statusFilter.value
  reqSurgeryPage(qp).then(res => { list.value = res })
}
const nowQuery = (): void => { qp.numPage = 1; sendQuery() }
const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)

// ---- 新建手术单 ----
const records = ref<RecordDto[]>([])
const createView = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref<SurgeryVo>({recordId: 0, surgeryName: '', surgeryType: '绝育', anesthesiaType: '全身麻醉', scheduledTime: null, surgeon: '', assistant: '', fee: 0})
const createRules = reactive<FormRules>({
  recordId: [{required: true, type: 'number', message: '请选择病历', trigger: 'change'}],
  surgeryName: [{required: true, message: '请填写手术名称', trigger: 'blur'}],
  fee: [{required: true, message: '请输入手术费', trigger: 'blur'}]
})
const recordLabel = (r: RecordDto): string => `#${r.recordId} ${r.petName || ''}（${r.clientName || ''}）`
const addRoleB = (): void => {
  createForm.value = {recordId: 0, surgeryName: '', surgeryType: '绝育', anesthesiaType: '全身麻醉', scheduledTime: null, surgeon: '', assistant: '', fee: 0}
  createFormRef.value?.resetFields()
  createView.value = true
}
const createSubmit = (): void => {
  createFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    reqCreateSurgery(createForm.value).then(res => {
      if (res.code === 1) { createView.value = false; ElMessage.success(res.msg || '手术安排成功'); sendQuery() }
    })
  })
}

// ---- 手术/麻醉记录 ----
const recordView = ref(false)
const saving = ref(false)
const current = ref<Surgery | null>(null)
const rec = ref<{status: number; startTime: string | null; endTime: string | null; surgeon: string | null; assistant: string | null; preopNote: string | null; intraopNote: string | null; postopNote: string | null}>({
  status: 0, startTime: null, endTime: null, surgeon: '', assistant: '', preopNote: '', intraopNote: '', postopNote: ''
})
const emptyAnesthesia = (surgeryId: number): AnesthesiaRecord => ({
  surgeryId, anesthesiaType: '', anesthetist: '', startTime: null, endTime: null, drugUsed: '', vitals: '', note: ''
})
const anesthesia = ref<AnesthesiaRecord>(emptyAnesthesia(0))

const openRecord = (row: Surgery): void => {
  reqSurgeryOne(row.surgeryId).then(res => {
    current.value = res
    rec.value = {
      status: res.status ?? 0, startTime: res.startTime, endTime: res.endTime,
      surgeon: res.surgeon, assistant: res.assistant,
      preopNote: res.preopNote, intraopNote: res.intraopNote, postopNote: res.postopNote
    }
    anesthesia.value = res.anesthesia ? {...res.anesthesia, surgeryId: res.surgeryId} : emptyAnesthesia(res.surgeryId)
    recordView.value = true
  })
}
const recordSubmit = (): void => {
  const cur = current.value
  if (!cur) return
  const doSave = (): void => {
    saving.value = true
    const surgery: Surgery = {...cur, ...rec.value, surgeryId: cur.surgeryId}
    reqUpdateSurgery(surgery).then(r1 => {
      if (r1.code !== 1) { ElMessage.error(r1.msg || '保存失败'); return }
      reqSaveAnesthesia(anesthesia.value).then(r2 => {
        saving.value = false
        if (r2.code === 1) { ElMessage.success('记录已保存'); recordView.value = false; sendQuery() }
        else ElMessage.error(r2.msg || '麻醉记录保存失败')
      })
    }).catch(() => { saving.value = false })
  }
  if ((cur.status ?? 0) === 0 && (rec.value.status === 1 || rec.value.status === 2)) {
    reqLabByRecord(cur.recordId).then(orders => {
      if (!orders) { ElMessage.error('请确认已开具术前化验单'); return }
      const finished = orders.filter(o => o.status).flatMap(o => o.items || []).map(i => i.itemName || '')
      const missing = PRE_OP_CHECKS.filter(c => !finished.some(n => n.includes(c)))
      if (missing.length) {
        ElMessageBox.confirm(
            `本台手术尚缺【${missing.join('、')}】未出结果，直接开始手术存在麻醉/手术风险。是否仍要继续？`,
            '术前检查缺失预警',
            {confirmButtonText: '仍要开始', cancelButtonText: '返回补齐', type: 'warning'}
        ).then(() => doSave()).catch(() => ElMessage.info('已取消，请先补齐术前检查'))
      } else {
        doSave()
      }
    }).catch(() => { ElMessage.info('无法获取术前化验信息，已跳过校验'); doSave() })
    return
  }
  doSave()
}

onMounted(() => {
  sendQuery()
  reqDoctorRecords().then(res => { records.value = res || [] })
  // 从就诊汇总跳转过来：预填病历并打开新建弹窗
  const rid = Number(route.query.recordId)
  if (rid) {
    createForm.value.recordId = rid
    createView.value = true
  }
})

// ---- 手术/麻醉知情同意书 ----
const router = useRouter()
const consentView = ref(false)
const consentSaving = ref(false)
const consentRow = ref<Surgery | null>(null)
const defaultRisks = '本人已被告知：本次手术及麻醉可能存在麻醉过敏、呼吸抑制、心跳异常、术中及术后出血、感染、切口愈合不良等风险，医院将尽最大努力防范；本人知悉上述风险，同意对上述宠物实施手术及麻醉，并承担相应责任。'
const consentForm = ref<{ surgeryId: number; risks: string; agree: boolean; signer: string; note: string }>({
  surgeryId: 0, risks: defaultRisks, agree: true, signer: '', note: ''
})
// 手术记录单打印（版式移植自老系统 ReportOper，A5 横向）
const printSheet = (row: Surgery): void => {
  window.open('/#/doctorPage/z-surgeryPrint?surgeryId=' + row.surgeryId, '_blank')
}

const openConsent = (row: Surgery): void => {
  consentRow.value = row
  consentForm.value = {surgeryId: row.surgeryId, risks: defaultRisks, agree: true, signer: '', note: ''}
  consentView.value = true
  // 已签过则回显
  reqConsentOne(row.surgeryId).then(res => {
    if (res && res.consentId) {
      consentForm.value = {
        surgeryId: row.surgeryId,
        risks: res.risks || defaultRisks,
        agree: res.agree ?? true,
        signer: res.signer || '',
        note: res.note || ''
      }
    }
  })
}
const consentSubmit = (): void => {
  if (!consentForm.value.signer.trim()) {
    ElMessage.warning('请填写签字人（畜主）姓名')
    return
  }
  consentSaving.value = true
  reqSignConsent(consentForm.value).then(res => {
    consentSaving.value = false
    if (res.code === 1) {
      ElMessage.success('知情同意书已签署')
      consentView.value = false
    } else ElMessage.error(res.msg || '签署失败')
  }).catch(() => { consentSaving.value = false })
}
const printConsent = (): void => {
  router.push('/backstage/z-consent?surgeryId=' + consentForm.value.surgeryId)
}
</script>

<style scoped>
.rec-head {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #1f2d5a;
}
</style>