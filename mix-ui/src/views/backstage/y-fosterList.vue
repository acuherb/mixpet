<template>
  <!--操作框-->
  <BackOpCol :writable="isManager" role="寄养" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.petName" clearable placeholder="宠物名 / 品种" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-input v-model="qp.clientName" clearable placeholder="寄养人姓名 / 手机号" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">
        已选 <b>{{ roleIdList.length }}</b> 项
      </span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="fosterList.records"
            max-height="calc(100vh - 130px)"
            row-key="fosterId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="订单号" prop="fosterCode"/>
    <el-table-column label="入托/到期" min-width="170">
      <template #default="scope">
        <span style="font-size:12px">{{ scope.row.inDate || '—' }} → {{ scope.row.fosterTerm || '—' }}</span>
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
    <el-table-column label="预估/结算" prop="fosterMoney" sortable width="100">
      <template #default="scope">
        <span :style="{color: scope.row.status === 2 ? '#67C23A' : '#E6A23C', fontWeight: 600}">￥{{ scope.row.fosterMoney }}</span>
      </template>
    </el-table-column>
    <el-table-column label="宠物名" prop="petName" min-width="90" show-overflow-tooltip/>
    <el-table-column label="寄养人" prop="clientName" min-width="90" show-overflow-tooltip/>
    <el-table-column label="订单备注" prop="fosterInfo" min-width="110" show-overflow-tooltip/>
    <el-table-column fixed="right" label="操作" width="180">
      <template #default="scope">
        <el-button-group v-if="isManager">
          <el-button v-if="(scope.row.status ?? 0) === 0" size="small" type="primary" @click="startFoster(scope.row)">开始寄养</el-button>
          <el-button v-if="(scope.row.status ?? 0) === 1" size="small" type="success" @click="openBack(scope.row)">接回结算</el-button>
          <el-button :icon="Edit" circle type="warning" @click="showDialog(scope.row)"/>
          <el-button :icon="Delete" circle type="danger" @click="delOne(scope.row.fosterId)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="fosterList.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="640px" :close-on-click-modal="false">
    <!--表单-->
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
      <!-- 按天计费分项：日均 = 笼位 + 清洁 + 餐费 -->
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
        （接回时按实际天数结算，多退少补）
      </div>
      <el-form-item label="订单备注" prop="fosterInfo">
        <el-input v-model="form.fosterInfo" clearable placeholder="无"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button round @click="modalView = false">取消</el-button>
        <el-button round type="primary" @click="formSubmit">确认</el-button>
      </span>
    </template>
  </el-dialog>

  <!--接回结算弹窗：按实际天数自动算，分项小计可核对，差额多退少补-->
  <el-dialog v-model="backView" title="接回结算" draggable width="480px" :close-on-click-modal="false">
    <div v-if="backRow" class="back-box">
      <div class="back-row back-head">
        <span>{{ backRow.petName }}（{{ backRow.clientName }}）· 订单 {{ backRow.fosterCode }}</span>
      </div>
      <div class="back-row"><span>入托日期</span><b>{{ backRow.inDate || '—' }}</b></div>
      <div class="back-row"><span>实际接回日期（可改）</span>
        <el-date-picker v-model="backDate" format="YYYY-MM-DD" placeholder="默认今天"
                        size="small" style="width:140px" type="date" value-format="YYYY-MM-DD"/>
      </div>
      <div class="back-row"><span>实际寄养天数</span><b>{{ actualDays() }} 天</b></div>
      <div class="back-row"><span>笼位费 ￥{{ n2(backRow.dailyFee) }}/天</span><b>￥{{ (n(backRow.dailyFee) * actualDays()).toFixed(2) }}</b></div>
      <div class="back-row"><span>清洁费 ￥{{ n2(backRow.cleanFee) }}/天</span><b>￥{{ (n(backRow.cleanFee) * actualDays()).toFixed(2) }}</b></div>
      <div class="back-row"><span>餐费 ￥{{ n2(backRow.mealFee) }}/天</span><b>￥{{ (n(backRow.mealFee) * actualDays()).toFixed(2) }}</b></div>
      <div class="back-row back-total"><span>结算金额（可改）</span>
        <el-input-number v-model="backMoney" :min="0" :max="99999" :precision="2" :step="10"
                         controls-position="right" size="small" style="width:130px"/>
      </div>
      <div class="back-row" v-if="backRow.fosterMoney > 0">
        <span>登记时预估/预付</span><b>￥{{ backRow.fosterMoney }}</b>
      </div>
      <div class="back-row" v-if="backRow.fosterMoney > 0">
        <span>{{ backMoney >= backRow.fosterMoney ? '需补收' : '应退还' }}</span>
        <b :style="{color: backMoney >= backRow.fosterMoney ? '#f56c6c' : '#67C23A'}">￥{{ Math.abs(backMoney - backRow.fosterMoney).toFixed(2) }}</b>
      </div>
      <div class="back-tip">确认后由服务端按天数与单价重算费用，计入收银台账单；请到收费列表收款</div>
    </div>
    <template #footer>
      <el-button round @click="backView = false">取消</el-button>
      <el-button round type="primary" :loading="backSaving" @click="submitBack">确认接回</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {Delete, Edit} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {canManage, useAuthLv} from '@/utils/PermissionUtil'

const authLv = useAuthLv()
const isManager = computed(() => canManage(authLv.value))
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery, YFosterList} from "@/model/VO/BackQuery";
import {exampleFoster, Foster} from "@/model/entiy/Foster";
import {FosterDto} from "@/model/DO/FosterDto";
import {Page} from "@/model/DO/Page";
import {reqAddFoster, reqBackFoster, reqDelFoster, reqDelFosterBatch, reqFosterList, reqUpdateFoster} from "@/request/FosterApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";
import {reqClientNameHavePet} from "@/request/ClientApi";
import {NameDo} from "@/model/DO/NameDo";
import {reqPetNameByClientId} from "@/request/PetApi";

/**
 ┌───────────────────────────────────┐
 │=============生命周期相关============│
 └───────────────────────────────────┘
 */
onMounted(() => {
  sendQuery()
})

/**
 ┌───────────────────────────────────┐
 │=============表格查询相关============│
 └───────────────────────────────────┘
 */
// 查询的参数
const qp: YFosterList = reactive({
  petName: '',
  clientName: '',
  numPage: 1,
  pageSize: 20
})
// 列表展示
const fosterList = ref<Page<FosterDto>>({records: [], total: 0})
// 分页条
const changePuB = (val: PageQuery) => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
// 数据总览
const sendQuery = (): void => {
  reqFosterList(qp).then(res => {
    fosterList.value = res
  })
}
// 搜索：输入即查（防抖），查询回到第一页
const nowQuery = (): void => {
  qp.numPage = 1
  sendQuery()
}
const debQuery = debounce(nowQuery, 400)

/**
 ┌───────────────────────────────────┐
 │=============数据删除相关============│
 └───────────────────────────────────┘
 */
// 多选与反选
const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: Foster[]): void => {
  roleIdList.value = val.map(obj => obj.fosterId)
}
// 批量删除
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm(
      '确定删除选中的记录吗？删除后不可恢复。',
      '删除多个确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqDelFosterBatch(roleIdList.value).then(res => sureFlush(res))
  }).catch(() => {
    ElMessage.info('删除取消')
  })
}
// 删除单个
const delOne = (id: number): void => {
  ElMessageBox.confirm(
      `删除后不可恢复，请确认。`,
      '删除单个确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqDelFoster(id).then(res => sureFlush(res))
  }).catch(() => {
    ElMessage.info('删除取消')
  })
}
// 确定请求的返回值，然后刷新
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}

/**
 ┌───────────────────────────────────┐
 │=============新增修改按钮============│
 └───────────────────────────────────┘
 */
// 模态框
const modalView = ref(false)
const modalTit = ref<"新增寄养" | "修改寄养">("修改寄养")
// 添加
const addRoleB = (): void => {
  const f = exampleFoster()
  f.inDate = todayStr()
  f.status = 0
  f.dailyFee = 60
  f.cleanFee = 10
  f.mealFee = 20
  f.fosterMoney = 90
  form.value = f
  myFormRef.value?.resetFields()
  reqClientNameHavePet(null).then(res => {
    clientNameL.value = res;
  })
  modalTit.value = "新增寄养"
  modalView.value = true
}
// 修改时展示模态框
const showDialog = (row: Foster) => {
  myFormRef.value?.clearValidate()
  // 赋值，防止响应式引用
  form.value.fosterId = row.fosterId
  form.value.fosterInfo = row.fosterInfo
  form.value.fosterTerm = row.fosterTerm
  form.value.fosterMoney = row.fosterMoney
  form.value.clientId = row.clientId
  form.value.petId = row.petId
  form.value.inDate = row.inDate
  form.value.dailyFee = row.dailyFee ?? 0
  form.value.cleanFee = row.cleanFee ?? 0
  form.value.mealFee = row.mealFee ?? 0
  form.value.status = row.status
  // ===赋值完成===
  reqClientNameHavePet(form.value.clientId).then(res => {
    clientNameL.value = res;
  })
  modalView.value = true
  modalTit.value = "修改寄养"
}

/**
 ┌───────────────────────────────────┐
 │=========按天计费与状态流转==========│
 └───────────────────────────────────┘
 */
const todayStr = (): string => {
  const t = new Date()
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`
}
const n = (v: number | null | undefined): number => v == null ? 0 : Number(v)
const n2 = (v: number | null | undefined): string => n(v).toFixed(2)
// 日均 = 笼位 + 清洁 + 餐费
const dailySum = (): number => n(form.value.dailyFee) + n(form.value.cleanFee) + n(form.value.mealFee)
// 预计天数（入托→预计接回，至少1天）
const planDays = (): number => {
  if (!form.value.inDate || !form.value.fosterTerm) return 0
  const a = new Date(form.value.inDate).getTime()
  const b = new Date(form.value.fosterTerm).getTime()
  if (isNaN(a) || isNaN(b) || b <= a) return 0
  return Math.round((b - a) / 86400000)
}
const planTotal = (): number => dailySum() * planDays()

/** 开始寄养：待入托 → 寄养中（记录实际入托日） */
const startFoster = (row: FosterDto): void => {
  ElMessageBox.confirm(`确认 ${row.petName} 今日入托？状态将变为「寄养中」。`, '开始寄养', {
    confirmButtonText: '确认入托', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqUpdateFoster({...row, status: 1, inDate: row.inDate || todayStr()} as Foster).then(res => {
      if (res.code === 1) { ElMessage.success('已开始寄养'); sendQuery() }
      else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}

/** 接回结算：按实际天数自动算金额（可改），差额多退少补 */
const backView = ref(false)
const backSaving = ref(false)
const backRow = ref<FosterDto | null>(null)
const backDate = ref(todayStr())
const backMoney = ref(0)
const openBack = (row: FosterDto): void => {
  backRow.value = row
  backDate.value = todayStr()
  backView.value = true
  // 默认结算金额 = 当前日均 × 已寄养天数（入托→今天）
  const inD = row.inDate ? new Date(row.inDate).getTime() : Date.now()
  const days = Math.max(1, Math.round((Date.now() - inD) / 86400000))
  const perDay = n(row.dailyFee) + n(row.cleanFee) + n(row.mealFee)
  backMoney.value = Math.round(perDay * days * 100) / 100
}
const actualDays = (): number => {
  if (!backRow.value) return 0
  const inD = backRow.value.inDate ? new Date(backRow.value.inDate).getTime() : Date.now()
  const outD = backDate.value ? new Date(backDate.value).getTime() : Date.now()
  if (isNaN(inD) || isNaN(outD)) return 1
  return Math.max(1, Math.round((outD - inD) / 86400000))
}
const submitBack = (): void => {
  if (!backRow.value) return
  backSaving.value = true
  reqBackFoster(backRow.value.fosterId!, backDate.value || '').then(res => {
    backSaving.value = false
    if (res.code === 1) {
      ElMessage.success(`接回成功，结算 ￥${backMoney.value.toFixed(2)}，已计入收银台账单`)
      backView.value = false
      sendQuery()
    } else ElMessage.error(res.msg || '操作失败')
  }).catch(() => { backSaving.value = false })
}

/**
 ┌───────────────────────────────────┐
 │=============表单校验相关============│
 └───────────────────────────────────┘
 */
// 表单的数据
const clientNameL = ref<NameDo[]>([]) // 全量 ID→姓名映射
const clientOpts = computed(() => clientNameL.value.map(c => ({label: c.roleName, value: c.roleId})))
const petNameL = ref<NameDo[]>([])
const form = ref<Foster>(exampleFoster()) // 空的默认值
const myFormRef = ref<FormInstance>()
// 校验表单并提交
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      // 校验通过，提交
      modalView.value = false
      if (modalTit.value == "新增寄养") reqAddFoster(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改寄养") reqUpdateFoster(form.value).then(res => sureFlush(res))
      else ElMessageBox.alert('操作未完成，请检查填写内容后重试')
    }
  })
}
// 表单校验规则
const rules = reactive<FormRules>({
  "fosterInfo": [
    {required: true, message: '请输入描述', trigger: 'blur'},
  ],
  "fosterTerm": [
    {required: true, message: '请输入日期', trigger: 'blur'},
  ],
  "fosterMoney": [
    {required: true, message: '给钱', trigger: 'blur'},
  ],
  "clientId": [
    {required: true, message: '请选择用户', trigger: 'blur'},
  ],
  "petId": [
    {required: true, message: '请选择宠物', trigger: 'blur'},
  ]
})

// 在用户改变时，查询宠物名
watch(() => form.value.clientId, (newValue): void => {
  if (newValue == 0 || newValue == null) return;
  reqPetNameByClientId(newValue, <number>form.value.petId).then(res => {
    petNameL.value = res
  })
})
</script>

<style scoped>
.sel-tip {
  margin-right: 12px;
  font-size: 14px;
  color: #409eff;
}
.sel-tip b {
  font-size: 16px;
  margin: 0 2px;
}

/* 寄养登记：日均汇总条 */
.foster-sum {
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #f0f7fc;
  border: 1px solid #c6e2ff;
  border-radius: 8px;
  font-size: 13px;
  color: #1F2D3D;

  b { color: var(--brand); }
}

/* 接回结算弹窗 */
.back-box {
  .back-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 7px 2px;
    font-size: 13px;
    color: #606266;
    border-bottom: 1px dashed #ebeef5;

    b { color: #1F2D3D; font-variant-numeric: tabular-nums; }

    &.back-head {
      border-bottom: none;
      padding-bottom: 2px;
      font-weight: 700;
      color: #1F2D3D;
    }

    &.back-total {
      border-bottom: none;
      padding-top: 10px;
      font-size: 14px;
      font-weight: 700;
    }
  }

  .back-tip {
    margin-top: 10px;
    padding: 8px 12px;
    background: #fdf6ec;
    border: 1px solid #f5dab1;
    border-radius: 8px;
    font-size: 12px;
    color: #8a6d3b;
    line-height: 1.6;
  }
}

</style>
