<template>
  <!--操作框-->
  <BackOpCol :writable="isManager" role="寄养" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.petName" clearable placeholder="宠物名 / 品种" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-input v-model="qp.clientName" clearable placeholder="寄养人姓名 / 手机号" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">已选 <b>{{ roleIdList.length }}</b> 项</span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="fosterList.records" max-height="calc(100vh - 130px)" row-key="fosterId" stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="订单号" prop="fosterCode"/>
    <el-table-column label="入托/到期/接回" min-width="190">
      <template #default="scope">
        <span style="font-size:12px">{{ scope.row.inDate || '—' }} → {{ scope.row.fosterTerm || '—' }}</span>
        <div v-if="scope.row.backDate" style="font-size:11px;color:#67c23a">实接 {{ scope.row.backDate }} · {{ scope.row.dayCount }} 天</div>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="90">
      <template #default="scope">
        <el-tag v-if="scope.row.status === 2" size="small" type="success">已接回</el-tag>
        <el-tag v-else-if="scope.row.status === 1" size="small" type="warning">寄养中</el-tag>
        <el-tag v-else size="small" type="info">待入托</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="费用/天" min-width="140">
      <template #default="scope">
        <span style="font-size:12px">笼位{{ n2(scope.row.dailyFee) }}｜清洁{{ n2(scope.row.cleanFee) }}｜餐{{ n2(scope.row.mealFee) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="押金 / 结算" min-width="130">
      <template #default="scope">
        <div style="font-size:12px">押金 ￥{{ n2(scope.row.fosterMoney) }}</div>
        <div v-if="scope.row.status === 2" style="font-size:12px;color:#67c23a;font-weight:600">
          结算 ￥{{ n2(scope.row.settleMoney) }}
        </div>
        <div v-if="scope.row.status === 2" style="font-size:11px;color:#909399">
          冲抵 ￥{{ n2(scope.row.depositUsed) }} · 补收 ￥{{ n2(scope.row.chargeMoney) }} · 应退 ￥{{ n2(scope.row.refundMoney) }}
        </div>
      </template>
    </el-table-column>
    <el-table-column label="宠物名" prop="petName" min-width="90" show-overflow-tooltip/>
    <el-table-column label="寄养人" prop="clientName" min-width="90" show-overflow-tooltip/>
    <el-table-column label="订单备注" prop="fosterInfo" min-width="110" show-overflow-tooltip/>
    <el-table-column fixed="right" label="操作" width="230">
      <template #default="scope">
        <el-button-group>
          <el-button v-if="(scope.row.status ?? 0) === 0" size="small" type="primary" @click="startFoster(scope.row)">开始寄养</el-button>
          <el-button v-if="(scope.row.status ?? 0) === 1" size="small" type="success" @click="openBack(scope.row)">接回结算</el-button>
          <el-button size="small" plain @click="openLog(scope.row)">每日记录</el-button>
          <el-button v-if="scope.row.status === 2" size="small" plain @click="printFoster(scope.row)">结算单</el-button>
          <template v-if="isManager">
            <el-button :icon="Edit" circle type="warning" @click="showDialog(scope.row)"/>
            <el-button :icon="Delete" circle type="danger" @click="delOne(scope.row.fosterId)"/>
          </template>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="fosterList.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="640px" :close-on-click-modal="false">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="寄养人" prop="clientId">
        <el-select-v2 v-model="form.clientId" filterable :options="clientOpts"
                      placeholder="输入姓名检索用户" style="width:100%" @change="form.petId=null"/>
      </el-form-item>
      <el-form-item label="宠物" prop="petId">
        <el-select v-model="form.petId" filterable placeholder="选择宠物" style="width:100%">
          <el-option v-for="d in petNameL" :key="d.roleId" :label="d.roleName" :value="d.roleId"/>
        </el-select>
      </el-form-item>
      <div style="display:flex;gap:12px">
        <el-form-item label="入托日期" style="flex:1">
          <el-date-picker v-model="form.inDate" format="YYYY-MM-DD" placeholder="默认今天"
                          style="width:100%" type="date" value-format="YYYY-MM-DD"/>
        </el-form-item>
        <el-form-item label="预计接回" prop="fosterTerm" style="flex:1">
          <el-date-picker v-model="form.fosterTerm" format="YYYY-MM-DD" placeholder="寄养截止日期"
                          style="width:100%" type="date" value-format="YYYY-MM-DD"/>
        </el-form-item>
      </div>
      <div style="display:flex;gap:12px">
        <el-form-item label="笼位费/天" style="flex:1">
          <el-input-number v-model="form.dailyFee" :min="0" :max="9999" :precision="2" :step="10"
                           controls-position="right" style="width:100%"/>
        </el-form-item>
        <el-form-item label="清洁费/天" style="flex:1">
          <el-input-number v-model="form.cleanFee" :min="0" :max="9999" :precision="2" :step="5"
                           controls-position="right" style="width:100%"/>
        </el-form-item>
        <el-form-item label="餐费/天" style="flex:1">
          <el-input-number v-model="form.mealFee" :min="0" :max="9999" :precision="2" :step="5"
                           controls-position="right" style="width:100%"/>
        </el-form-item>
      </div>
      <div class="foster-sum">
        日均 <b>￥{{ dailySum().toFixed(2) }}</b> × 预计 <b>{{ planDays() }}</b> 天 = 预估 <b style="color:#E6A23C">￥{{ planTotal().toFixed(2) }}</b>
        （天数含首尾，与接回结算口径一致；押金多退少补）
      </div>
      <el-form-item label="押金" prop="fosterMoney">
        <el-input-number v-model="form.fosterMoney" :min="1" :max="99999" :precision="0" :step="50"
                         controls-position="right" style="width:180px"/>
      </el-form-item>
      <el-form-item label="订单备注" prop="fosterInfo">
        <el-input v-model="form.fosterInfo" clearable placeholder="无"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button round @click="modalView = false">取消</el-button>
      <el-button round type="primary" @click="formSubmit">确认</el-button>
    </template>
  </el-dialog>

  <!--接回结算弹窗：天数含首尾，金额可改但改价说明必填并留痕-->
  <el-dialog v-model="backView" title="接回结算" draggable width="500px" :close-on-click-modal="false">
    <div v-if="backRow" class="back-box">
      <div class="back-row back-head"><span>{{ backRow.petName }}（{{ backRow.clientName }}）· 订单 {{ backRow.fosterCode }}</span></div>
      <div class="back-row"><span>入托日期</span><b>{{ backRow.inDate || '—' }}</b></div>
      <div class="back-row"><span>实际接回日期（可改）</span>
        <el-date-picker v-model="backDate" format="YYYY-MM-DD" placeholder="默认今天"
                        size="small" style="width:140px" type="date" value-format="YYYY-MM-DD"/>
      </div>
      <div class="back-row"><span>实际寄养天数（含首尾）</span><b>{{ actualDays() }} 天</b></div>
      <div class="back-row"><span>笼位费 ￥{{ n2(backRow.dailyFee) }}/天</span><b>￥{{ (n(backRow.dailyFee) * actualDays()).toFixed(2) }}</b></div>
      <div class="back-row"><span>清洁费 ￥{{ n2(backRow.cleanFee) }}/天</span><b>￥{{ (n(backRow.cleanFee) * actualDays()).toFixed(2) }}</b></div>
      <div class="back-row"><span>餐费 ￥{{ n2(backRow.mealFee) }}/天</span><b>￥{{ (n(backRow.mealFee) * actualDays()).toFixed(2) }}</b></div>
      <div class="back-row back-total"><span>系统计价</span><b>￥{{ autoMoney().toFixed(2) }}</b></div>
      <div class="back-row back-total"><span>结算金额（可改）</span>
        <el-input-number v-model="backMoney" :min="0" :max="99999" :precision="2" :step="10"
                         controls-position="right" size="small" style="width:130px" @change="onBackMoneyChange"/>
      </div>
      <div v-if="adjusted()" class="back-row">
        <span style="color:#e6a23c">改价说明（必填，留痕）</span>
        <el-input v-model="adjustNote" clearable maxlength="200" placeholder="如：老客户减免 50 元" size="small" style="width:230px"/>
      </div>
      <div class="back-row"><span>登记押金</span><b>￥{{ n2(backRow.fosterMoney) }}</b></div>
      <div class="back-row"><span>押金冲抵</span><b>￥{{ depositUsed().toFixed(2) }}</b></div>
      <div class="back-row"><span>{{ backMoney >= backRow.fosterMoney ? '需补收（计入账单）' : '应退还主人' }}</span>
        <b :style="{color: backMoney >= backRow.fosterMoney ? '#f56c6c' : '#67C23A'}">￥{{ Math.abs(backMoney - backRow.fosterMoney).toFixed(2) }}</b>
      </div>
      <div class="back-tip">确认后服务端按天数与单价校验改价并留痕，押金先冲抵，净额计入收银台的「寄养费」分项；请到收费列表收款。</div>
    </div>
    <template #footer>
      <el-button round @click="backView = false">取消</el-button>
      <el-button round type="primary" :loading="backSaving" @click="submitBack">确认接回</el-button>
    </template>
  </el-dialog>

  <!--每日照护记录：文字 + 照片，主人可在「我的寄养」里按天查看-->
  <el-dialog v-model="logView" title="每日照护记录" draggable width="720px" :close-on-click-modal="false">
    <div v-if="logRow" class="log-box">
      <div class="log-head">{{ logRow.petName }} · 订单 {{ logRow.fosterCode }} · {{ fosterStatusText(logRow.status) }}</div>
      <div class="log-add">
        <div class="log-add-tit">新增一天的记录</div>
        <div class="log-form">
          <el-date-picker v-model="logForm.logDate" format="YYYY-MM-DD" type="date" value-format="YYYY-MM-DD" size="small" style="width:140px"/>
          <el-select v-model="logForm.mentalState" placeholder="精神" clearable size="small" style="width:110px">
            <el-option v-for="o in ['正常','活泼','安静','萎靡','紧张']" :key="o" :label="o" :value="o"/>
          </el-select>
          <el-select v-model="logForm.appetite" placeholder="食欲" clearable size="small" style="width:110px">
            <el-option v-for="o in ['正常','良好','一般','不佳','拒食']" :key="o" :label="o" :value="o"/>
          </el-select>
          <el-select v-model="logForm.defecation" placeholder="排便" clearable size="small" style="width:110px">
            <el-option v-for="o in ['正常','偏稀','偏干','未排','腹泻']" :key="o" :label="o" :value="o"/>
          </el-select>
          <el-input-number v-model="logForm.weight" :min="0" :max="200" :precision="2" placeholder="体重kg" size="small" style="width:110px"/>
        </div>
        <el-input v-model="logForm.note" :rows="2" maxlength="500" placeholder="今日照护情况 / 给主人的留言" type="textarea"/>
        <div class="log-files">
          <input ref="fileInput" accept="image/*" multiple type="file" @change="onFiles"/>
          <span v-if="pendingFiles.length" class="log-file-tip">已选 {{ pendingFiles.length }} 张：{{ pendingFiles.map(f => f.name).join('、') }}</span>
          <span v-else class="log-file-tip">可同时选择多张当日照片</span>
          <el-button :loading="logSaving" size="small" type="primary" @click="submitLog">保存记录</el-button>
        </div>
      </div>
      <div class="log-list-tit">历史记录（{{ logs.length }}）</div>
      <div v-if="!logs.length" class="log-none">还没有照护记录</div>
      <div v-for="l in logs" :key="l.logId" class="log-item">
        <div class="log-date">{{ l.logDate }} <span>{{ l.recorder || '' }}</span></div>
        <div class="log-fields">
          精神 {{ l.mentalState || '—' }} · 食欲 {{ l.appetite || '—' }} · 排便 {{ l.defecation || '—' }}
          <template v-if="l.weight != null"> · 体重 {{ l.weight }}kg</template>
        </div>
        <div v-if="l.note" class="log-note">{{ l.note }}</div>
        <div v-if="photosOf(l.logDate).length" class="log-photos">
          <img v-for="p in photosOf(l.logDate)" :key="p.photoId" :src="fosterPhotoUrl(p.photoId)" alt="寄养照片" loading="lazy"/>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {Delete, Edit} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {canManage, useAuthLv} from '@/utils/PermissionUtil'
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery, YFosterList} from "@/model/VO/BackQuery";
import {exampleFoster, Foster, fosterDays, fosterStatusText} from "@/model/entiy/Foster";
import {FosterDto} from "@/model/DO/FosterDto";
import {Page} from "@/model/DO/Page";
import {
  reqAddFoster, reqAddFosterLog, reqBackFoster, reqDelFoster, reqDelFosterBatch, reqFosterList,
  reqFosterLogs, reqFosterPhotos, reqStartFoster, reqUpdateFoster, reqUploadFosterPhoto, fosterPhotoUrl
} from "@/request/FosterApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";
import {reqClientNameHavePet} from "@/request/ClientApi";
import {NameDo} from "@/model/DO/NameDo";
import {reqPetNameByClientId} from "@/request/PetApi";
import {FosterLog, FosterLogVo, FosterPhoto} from "@/model/VO/FosterCare";

const authLv = useAuthLv()
const isManager = computed(() => canManage(authLv.value))

onMounted(() => { sendQuery() })

// ── 表格查询 ───────────────────────────────────────────
const qp: YFosterList = reactive({petName: '', clientName: '', numPage: 1, pageSize: 20})
const fosterList = ref<Page<FosterDto>>({records: [], total: 0})
const changePuB = (val: PageQuery): void => { qp.numPage = val.numPage; qp.pageSize = val.pageSize; sendQuery() }
const sendQuery = (): void => { reqFosterList(qp).then(res => { fosterList.value = res }) }
const nowQuery = (): void => { qp.numPage = 1; sendQuery() }
const debQuery = debounce(nowQuery, 400)

// ── 删除 ───────────────────────────────────────────────
const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: Foster[]): void => { roleIdList.value = val.map(obj => obj.fosterId) }
const delBatchB = (): void => {
  if (!roleIdList.value.length) return
  ElMessageBox.confirm('确定删除选中的记录吗？删除后不可恢复。', '删除多个确认', {type: 'warning'})
      .then(() => reqDelFosterBatch(roleIdList.value).then(res => sureFlush(res)))
      .catch(() => ElMessage.info('删除取消'))
}
const delOne = (id: number): void => {
  ElMessageBox.confirm('删除后不可恢复，请确认。', '删除单个确认', {type: 'warning'})
      .then(() => reqDelFoster(id).then(res => sureFlush(res)))
      .catch(() => ElMessage.info('删除取消'))
}
const sureFlush = (res: Res): void => { if (res.code === 1) sendQuery() }

// ── 新增/修改 ──────────────────────────────────────────
const modalView = ref(false)
const modalTit = ref<"新增寄养" | "修改寄养">("修改寄养")
const todayStr = (): string => {
  const t = new Date()
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`
}
const addRoleB = (): void => {
  const f = exampleFoster()
  f.inDate = todayStr(); f.status = 0
  f.dailyFee = 60; f.cleanFee = 10; f.mealFee = 20; f.fosterMoney = 90
  form.value = f
  myFormRef.value?.resetFields()
  reqClientNameHavePet(null).then(res => { clientNameL.value = res })
  modalTit.value = "新增寄养"
  modalView.value = true
}
const showDialog = (row: Foster): void => {
  myFormRef.value?.clearValidate()
  Object.assign(form.value, {
    fosterId: row.fosterId, fosterInfo: row.fosterInfo, fosterTerm: row.fosterTerm,
    fosterMoney: row.fosterMoney, clientId: row.clientId, petId: row.petId,
    inDate: row.inDate, dailyFee: row.dailyFee ?? 0, cleanFee: row.cleanFee ?? 0,
    mealFee: row.mealFee ?? 0, status: row.status
  })
  reqClientNameHavePet(form.value.clientId).then(res => { clientNameL.value = res })
  modalView.value = true
  modalTit.value = "修改寄养"
}

// ── 按天计费与状态流转 ──────────────────────────────────
const n = (v: number | null | undefined): number => v == null ? 0 : Number(v)
const n2 = (v: number | null | undefined): string => n(v).toFixed(2)
const dailySum = (): number => n(form.value.dailyFee) + n(form.value.cleanFee) + n(form.value.mealFee)
// 天数含首尾，与后端一致
const planDays = (): number => fosterDays(form.value.inDate, form.value.fosterTerm)
const planTotal = (): number => dailySum() * planDays()

const startFoster = (row: FosterDto): void => {
  ElMessageBox.confirm(`确认 ${row.petName} 今日入托？状态将变为「寄养中」。`, '开始寄养', {
    confirmButtonText: '确认入托', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqStartFoster(row.fosterId).then(res => {
      if (res.code === 1) { ElMessage.success(res.msg || '已开始寄养'); sendQuery() }
      else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}

// ── 接回结算 ───────────────────────────────────────────
const backView = ref(false)
const backSaving = ref(false)
const backRow = ref<FosterDto | null>(null)
const backDate = ref(todayStr())
const backMoney = ref(0)
const adjustNote = ref('')
const onBackMoneyChange = (): void => { /* 改价后由 adjusted() 决定是否要求说明 */ }
const actualDays = (): number => fosterDays(backRow.value?.inDate, backDate.value) || 1
const autoMoney = (): number => {
  if (!backRow.value) return 0
  return Math.round((n(backRow.value.dailyFee) + n(backRow.value.cleanFee) + n(backRow.value.mealFee)) * actualDays() * 100) / 100
}
const adjusted = (): boolean => Math.abs(backMoney.value - autoMoney()) > 0.001
const depositUsed = (): number => Math.min(n(backRow.value?.fosterMoney), backMoney.value)
const openBack = (row: FosterDto): void => {
  backRow.value = row
  backDate.value = todayStr()
  backMoney.value = 0
  adjustNote.value = ''
  backMoney.value = Math.round((n(row.dailyFee) + n(row.cleanFee) + n(row.mealFee)) * (fosterDays(row.inDate, backDate.value) || 1) * 100) / 100
  backView.value = true
}
const submitBack = (): void => {
  if (!backRow.value) return
  if (adjusted() && !adjustNote.value.trim()) { ElMessage.warning('人工改价必须填写改价说明'); return }
  backSaving.value = true
  reqBackFoster(backRow.value.fosterId, backDate.value || '', backMoney.value, adjusted() ? adjustNote.value.trim() : undefined)
      .then(res => {
        backSaving.value = false
        if (res.code === 1) {
          ElMessage.success(res.msg || `接回成功，结算 ￥${backMoney.value.toFixed(2)}`)
          backView.value = false
          sendQuery()
        } else ElMessage.error(res.msg || '操作失败')
      }).catch(() => { backSaving.value = false })
}
const printFoster = (row: FosterDto): void => {
  window.open('/#/doctorPage/z-fosterPrint?id=' + row.fosterId, '_blank')
}

// ── 每日照护记录 / 照片 ────────────────────────────────
const logView = ref(false)
const logSaving = ref(false)
const logRow = ref<FosterDto | null>(null)
const logs = ref<FosterLog[]>([])
const photos = ref<FosterPhoto[]>([])
const pendingFiles = ref<File[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const logForm = reactive({logDate: todayStr(), mentalState: '', appetite: '', defecation: '', weight: null as number | null, note: ''})
const photosOf = (date: string): FosterPhoto[] => photos.value.filter(p => p.logDate === date)
const loadLog = (id: number): void => {
  reqFosterLogs(id).then(res => { logs.value = res || [] })
  reqFosterPhotos(id).then(res => { photos.value = res || [] })
}
const openLog = (row: FosterDto): void => {
  logRow.value = row
  pendingFiles.value = []
  logForm.logDate = todayStr()
  logForm.mentalState = ''; logForm.appetite = ''; logForm.defecation = ''
  logForm.weight = null; logForm.note = ''
  loadLog(row.fosterId)
  logView.value = true
}
const onFiles = (e: Event): void => {
  const el = e.target as HTMLInputElement
  pendingFiles.value = Array.from(el.files || [])
}
const submitLog = (): void => {
  if (!logRow.value) return
  logSaving.value = true
  Promise.all(pendingFiles.value.map(f => reqUploadFosterPhoto(f)))
      .then(results => {
        const names = results.filter(r => r?.code === 1).map(r => String(r.data || '')).filter(Boolean)
        const vo: FosterLogVo = {
          fosterId: logRow.value!.fosterId, logDate: logForm.logDate,
          mentalState: logForm.mentalState, appetite: logForm.appetite,
          defecation: logForm.defecation, weight: logForm.weight,
          note: logForm.note, photos: names
        }
        return reqAddFosterLog(vo)
      })
      .then(res => {
        if (res?.code === 1) {
          ElMessage.success(res.msg || '照护记录已保存')
          pendingFiles.value = []
          if (fileInput.value) fileInput.value.value = ''
          logForm.note = ''
          loadLog(logRow.value!.fosterId)
        } else ElMessage.error(res?.msg || '保存失败')
      })
      .catch(() => ElMessage.error('照片上传或记录保存失败'))
      .finally(() => { logSaving.value = false })
}

// ── 表单 ───────────────────────────────────────────────
const clientNameL = ref<NameDo[]>([])
const clientOpts = computed(() => clientNameL.value.map(c => ({label: c.roleName, value: c.roleId})))
const petNameL = ref<NameDo[]>([])
const form = ref<Foster>(exampleFoster())
const myFormRef = ref<FormInstance>()
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      if (dailySum() <= 0) { ElMessage.warning('请至少配置一项日单价'); return }
      modalView.value = false
      if (modalTit.value === "新增寄养") reqAddFoster(form.value).then(res => sureFlush(res))
      else reqUpdateFoster(form.value).then(res => sureFlush(res))
    }
  })
}
const rules = reactive<FormRules>({
  fosterInfo: [{required: true, message: '请输入描述', trigger: 'blur'}],
  fosterTerm: [{required: true, message: '请输入日期', trigger: 'blur'}],
  fosterMoney: [{required: true, message: '请填押金', trigger: 'blur'}],
  clientId: [{required: true, message: '请选择用户', trigger: 'blur'}],
  petId: [{required: true, message: '请选择宠物', trigger: 'blur'}]
})
watch(() => form.value.clientId, (newValue): void => {
  if (newValue == 0 || newValue == null) return
  reqPetNameByClientId(newValue, <number>form.value.petId).then(res => { petNameL.value = res })
})
</script>

<style scoped>
.sel-tip { margin-right: 12px; font-size: 14px; color: #409eff; }
.sel-tip b { font-size: 16px; margin: 0 2px; }
.foster-sum { padding: 8px 12px; margin-bottom: 12px; background: #f0f7fc; border: 1px solid #c6e2ff; border-radius: 8px; font-size: 13px; color: #1F2D3D; }
.foster-sum b { color: var(--brand); }
.back-box .back-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 7px 2px; font-size: 13px; color: #606266; border-bottom: 1px dashed #ebeef5; }
.back-box .back-row b { color: #1F2D3D; font-variant-numeric: tabular-nums; }
.back-box .back-row.back-head { border-bottom: none; padding-bottom: 2px; font-weight: 700; color: #1F2D3D; }
.back-box .back-row.back-total { border-bottom: none; padding-top: 10px; font-size: 14px; font-weight: 700; }
.back-tip { margin-top: 10px; padding: 8px 12px; background: #fdf6ec; border: 1px solid #f5dab1; border-radius: 8px; font-size: 12px; color: #8a6d3b; line-height: 1.6; }
.log-box { max-height: 62vh; overflow: auto; }
.log-head { font-weight: 700; margin-bottom: 10px; }
.log-add { padding: 10px; background: #fafcff; border: 1px solid #e8edf5; border-radius: 8px; margin-bottom: 14px; }
.log-add-tit { font-size: 13px; font-weight: 700; margin-bottom: 8px; }
.log-form { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 8px; }
.log-files { display: flex; align-items: center; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.log-file-tip { font-size: 12px; color: #909399; flex: 1; min-width: 120px; }
.log-list-tit { font-size: 13px; font-weight: 700; margin-bottom: 8px; }
.log-none { font-size: 12px; color: #b3bccb; }
.log-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px; margin-bottom: 8px; }
.log-date { font-weight: 700; font-size: 13px; }
.log-date span { font-weight: 400; color: #909399; font-size: 12px; margin-left: 8px; }
.log-fields { font-size: 12px; color: #606266; margin-top: 4px; }
.log-note { font-size: 12.5px; color: #303133; margin-top: 4px; white-space: pre-wrap; }
.log-photos { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
.log-photos img { width: 88px; height: 88px; object-fit: cover; border-radius: 6px; border: 1px solid #ebeef5; }
</style>
