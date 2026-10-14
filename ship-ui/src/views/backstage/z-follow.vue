<template>
  <div>
    <!--操作框：新建 + 状态筛选 + 搜索-->
    <BackOpCol role="随访" @addRole="addRoleB" @query="nowQuery">
      <el-input v-model="qp.seaName" clearable placeholder="宠物名 / 客户名" size="large" style="width: 200px"
                @input="debQuery" @keyup.enter="nowQuery"/>
      <el-select v-model="statusFilter" clearable placeholder="全部状态" size="large" style="width: 130px; margin-left: 8px" @change="nowQuery">
        <el-option label="待随访" :value="false"/>
        <el-option label="已随访" :value="true"/>
      </el-select>
    </BackOpCol>

    <!--列表展示-->
    <el-table :data="list.records" max-height="calc(100vh - 210px)" row-key="followId" stripe>
      <el-table-column label="宠物" min-width="100" prop="petName" show-overflow-tooltip/>
      <el-table-column label="客户" min-width="100" prop="clientName" show-overflow-tooltip/>
      <el-table-column label="诊断" min-width="120" prop="diagnosis" show-overflow-tooltip/>
      <el-table-column label="方式" width="80" prop="followType"/>
      <el-table-column label="随访内容" min-width="160" prop="content" show-overflow-tooltip/>
      <el-table-column label="计划时间" width="160" prop="planTime">
        <template #default="scope">{{ fmt(scope.row.planTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.status" size="small" type="success">已随访</el-tag>
          <el-tag v-else size="small" type="warning">待随访</el-tag>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="150">
        <template #default="scope">
          <el-button v-if="!scope.row.status" size="small" plain round type="primary" @click="openDone(scope.row)">完成随访</el-button>
          <el-button size="small" type="info" plain @click="showDetail(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <BackPage :total="list.total" @changePu="changePuB"/>

    <!--新建随访弹窗-->
    <el-dialog v-model="modalView" title="新建随访计划" width="520px" draggable>
      <el-form ref="myFormRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="病历号" prop="recordId">
          <el-input-number v-model="form.recordId" :min="1" placeholder="输入本次就诊病历id" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="随访方式" prop="followType">
          <el-select v-model="form.followType" placeholder="选择方式" style="width: 100%">
            <el-option v-for="t in followTypes" :key="t" :label="t" :value="t"/>
          </el-select>
        </el-form-item>
        <el-form-item label="随访内容">
          <el-input v-model="form.content" :rows="3" maxlength="500" placeholder="随访内容/计划，如：术后恢复情况、用药依从性" type="textarea"/>
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker v-model="form.planTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          format="YYYY-MM-DD HH:mm" placeholder="选择计划随访时间" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" :rows="2" maxlength="255" type="textarea"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit">确认创建</el-button>
      </template>
    </el-dialog>

    <!--完成随访弹窗-->
    <el-dialog v-model="doneView" title="完成随访" width="480px" draggable>
      <div class="done-head">{{ doneRow?.petName }}（{{ doneRow?.clientName }}）· {{ doneRow?.followType }}</div>
      <el-form label-width="80px">
        <el-form-item label="随访结果">
          <el-input v-model="doneResult" :rows="4" maxlength="500" placeholder="客户反馈、恢复情况、是否需要复诊等" type="textarea"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="doneView = false">取消</el-button>
        <el-button type="primary" :loading="doneSaving" @click="submitDone">确认完成</el-button>
      </template>
    </el-dialog>

    <!--查看详情-->
    <el-dialog v-model="detailView" title="随访详情" width="480px" draggable>
      <el-descriptions v-if="detailRow" :column="1" border>
        <el-descriptions-item label="宠物">{{ detailRow.petName }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detailRow.clientName }}</el-descriptions-item>
        <el-descriptions-item label="诊断">{{ detailRow.diagnosis || '—' }}</el-descriptions-item>
        <el-descriptions-item label="方式">{{ detailRow.followType }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ detailRow.content || '—' }}</el-descriptions-item>
        <el-descriptions-item label="计划时间">{{ fmt(detailRow.planTime) || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="detailRow.status" size="small" type="success">已随访</el-tag>
          <el-tag v-else size="small" type="warning">待随访</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="随访结果">{{ detailRow.result || '—' }}</el-descriptions-item>
        <el-descriptions-item label="随访时间">{{ fmt(detailRow.followTime) || '—' }}</el-descriptions-item>
        <el-descriptions-item label="随访人">{{ detailRow.followBy || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import BackPage from "@/components/row/BackPage.vue";
import {debounce} from "@/utils/DebounceUtil";
import {PageQuery} from "@/model/VO/BackQuery";
import {Page} from "@/model/DO/Page";
import {followTypes, FollowUp, FollowUpDto} from "@/model/entiy/FollowUp";
import {reqAddFollow, reqFollowDone, reqFollowPage} from "@/request/FollowApi";
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

const qp = reactive({numPage: 1, pageSize: 15, status: null as boolean | null, seaName: ''})
const statusFilter = ref<boolean | null>(null)
const list = ref<Page<FollowUpDto>>({records: [], total: 0})
const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)

const changePuB = (val: PageQuery): void => { qp.numPage = val.numPage; qp.pageSize = val.pageSize; sendQuery() }
const sendQuery = (): void => {
  qp.status = statusFilter.value
  reqFollowPage(qp).then(res => { list.value = res })
}
const nowQuery = (): void => { qp.numPage = 1; sendQuery() }
const debQuery = debounce(nowQuery, 400)

// ---- 新建随访 ----
const modalView = ref(false)
const myFormRef = ref<FormInstance>()
const emptyForm = (): FollowUp => ({followId: 0, recordId: 0, petId: 0, clientId: 0, followType: '电话', content: '', result: null, planTime: null, followTime: null, status: false, followBy: null, note: ''})
const form = ref<FollowUp>(emptyForm())
const rules = reactive<FormRules>({
  recordId: [{required: true, type: 'number', message: '请输入病历号', trigger: 'blur'}],
  followType: [{required: true, message: '请选择随访方式', trigger: 'change'}]
})
const addRoleB = (): void => { form.value = emptyForm(); myFormRef.value?.resetFields(); modalView.value = true }
const formSubmit = (): void => {
  myFormRef.value?.validate((valid: boolean) => {
    if (!valid) return
    reqAddFollow(form.value).then((res: Res) => {
      if (res.code === 1) { ElMessage.success(res.msg || '随访计划已创建'); modalView.value = false; sendQuery() }
      else ElMessage.error(res.msg || '创建失败')
    })
  })
}

// ---- 完成随访 ----
const doneView = ref(false)
const doneSaving = ref(false)
const doneRow = ref<FollowUpDto | null>(null)
const doneResult = ref('')
const openDone = (row: FollowUpDto): void => { doneRow.value = row; doneResult.value = ''; doneView.value = true }
const submitDone = (): void => {
  if (!doneRow.value) return
  doneSaving.value = true
  reqFollowDone(doneRow.value.followId, doneResult.value).then((res: Res) => {
    doneSaving.value = false
    if (res.code === 1) { ElMessage.success('随访已完成'); doneView.value = false; sendQuery() }
    else ElMessage.error(res.msg || '操作失败')
  }).catch(() => { doneSaving.value = false })
}

// ---- 查看详情 ----
const detailView = ref(false)
const detailRow = ref<FollowUpDto | null>(null)
const showDetail = (row: FollowUpDto): void => { detailRow.value = row; detailView.value = true }

onMounted(() => { sendQuery() })
</script>

<style scoped>
.done-head { font-size: 14px; font-weight: 600; margin-bottom: 10px; color: #1f2d5a; }
</style>
