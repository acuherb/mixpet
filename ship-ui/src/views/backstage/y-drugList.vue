<template>
  <!--操作框-->
  <BackOpCol role="药品" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.drugName" clearable placeholder="药品名 / 规格" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-select v-model="qp.drugType" clearable placeholder="全部分类" size="large" style="width: 110px" @change="nowQuery">
      <el-option v-for="t in drugTypeOpts" :key="t.dictId" :label="t.dictKey" :value="t.dictKey"/>
    </el-select>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">
        已选 <b>{{ roleIdList.length }}</b> 项
      </span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="drugList.records"
            max-height="calc(100vh - 130px)"
            row-key="drugId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="药品名" min-width="130" prop="drugName" show-overflow-tooltip/>
    <el-table-column label="规格" min-width="110" prop="drugSpec" show-overflow-tooltip/>
    <el-table-column label="分类" prop="drugType" width="80">
      <template #default="scope">
        <el-tag size="small">{{ scope.row.drugType || '未分类' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="单价(元)" prop="drugPrice" sortable width="100"/>
    <el-table-column label="库存" prop="drugStock" sortable width="110">
      <template #default="scope">
        <el-tag v-if="scope.row.drugStock === 0" size="small" type="danger">缺货</el-tag>
        <el-tag v-else-if="scope.row.drugStock !== null && scope.row.drugStock <= warnOf(scope.row)" size="small" type="warning">
          {{ scope.row.drugStock }}
        </el-tag>
        <span v-else>{{ scope.row.drugStock }}</span>
      </template>
    </el-table-column>
    <el-table-column label="有效期至" width="120">
      <template #default="scope">
        <span v-if="scope.row.expireDate">{{ scope.row.expireDate }}</span>
        <span v-else>—</span>
      </template>
    </el-table-column>
    <el-table-column label="接种间隔" width="100">
      <template #default="scope">
        <span v-if="scope.row.intervalDays">{{ scope.row.intervalDays }}天</span>
        <span v-else>—</span>
      </template>
    </el-table-column>
    <el-table-column label="状态" width="80">
      <template #default="scope">
        <el-tag v-if="scope.row.active" size="small" type="success">启用</el-tag>
        <el-tag v-else size="small" type="info">停用</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="说明" min-width="120" prop="drugInfo" show-overflow-tooltip/>
    <el-table-column fixed="right" label="操作" width="210">
      <template #default="scope">
        <el-button-group>
          <el-button :icon="Edit" circle title="编辑" type="warning" @click="showDialog(scope.row)"/>
          <el-button size="small" title="入库" type="primary" @click="openIn(scope.row)">入库</el-button>
          <el-button size="small" title="出入库台账" type="info" @click="openLedger(scope.row)">台账</el-button>
          <el-button :icon="Delete" circle title="删除" type="danger" @click="delOne(scope.row.drugId)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="drugList.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="500px">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="药品名" prop="drugName">
        <el-input v-model="form.drugName" clearable maxlength="64" placeholder="如：阿莫西林克拉维酸钾片"/>
      </el-form-item>
      <el-form-item label="规格" prop="drugSpec">
        <el-input v-model="form.drugSpec" clearable maxlength="64" placeholder="如：625mg×7片"/>
      </el-form-item>
      <el-form-item label="分类" prop="drugType">
        <el-select v-model="form.drugType" filterable placeholder="选择分类（在系统设置维护）">
          <el-option v-for="t in drugTypeOpts" :key="t.dictId" :label="t.dictKey" :value="t.dictKey"/>
        </el-select>
      </el-form-item>
      <el-form-item label="单位" prop="drugUnit">
        <el-select v-model="form.drugUnit" filterable placeholder="选择单位（在系统设置维护）">
          <el-option v-for="u in drugUnitOpts" :key="u.dictId" :label="u.dictKey" :value="u.dictKey"/>
        </el-select>
      </el-form-item>
      <el-form-item label="单价(元)" prop="drugPrice">
        <el-input-number v-model="form.drugPrice" :max="99999" :min="0" :precision="2" :step="1" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="库存" prop="drugStock">
        <el-input-number v-model="form.drugStock" :max="99999" :min="0" :step="10" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="预警阈值">
        <el-input-number v-model="form.warningStock" :max="9999" :min="0" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="有效期至">
        <el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" placeholder="选择有效期" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.active" active-text="启用" inactive-text="停用"/>
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="form.drugInfo" :rows="2" maxlength="255" type="textarea"/>
      </el-form-item>
      <template v-if="form.drugType === '疫苗'">
        <el-form-item label="品牌/厂家">
          <el-input v-model="form.brand" clearable maxlength="64" placeholder="如：辉瑞 / 英特威"/>
        </el-form-item>
        <el-form-item label="预防疾病">
          <el-input v-model="form.prevent" clearable maxlength="128" placeholder="如：犬瘟/细小/狂犬病"/>
        </el-form-item>
        <el-form-item label="接种间隔(天)">
          <el-input-number v-model="form.intervalDays" :min="0" :max="3650" style="width: 100%"/>
        </el-form-item>
      </template>
    </el-form>
    <template #footer>
      <el-button @click="modalView = false">取消</el-button>
      <el-button type="primary" @click="formSubmit">确认</el-button>
    </template>
  </el-dialog>

  <!--入库弹窗-->
  <el-dialog v-model="inView" draggable title="药品入库" width="420px">
    <el-form label-width="90px">
      <el-form-item label="药品">
        <span>{{ inDrug?.drugName }}（当前库存 {{ inDrug?.drugStock ?? 0 }}）</span>
      </el-form-item>
      <el-form-item label="入库数量">
        <el-input-number v-model="inQty" :min="1" :max="99999" style="width: 100%"/>
      </el-form-item>
      <el-form-item label="有效期至">
        <el-date-picker v-model="inExpire" placeholder="可留空" style="width: 100%" type="date" value-format="YYYY-MM-DD"/>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="inNote" maxlength="128" placeholder="如：采购入库"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="inView = false">取消</el-button>
      <el-button type="primary" @click="submitIn">确认入库</el-button>
    </template>
  </el-dialog>

  <!--出入库台账抽屉-->
  <el-drawer v-model="ledgerView" :title="ledgerTitle" size="560px">
    <el-table :data="ledger.records" border max-height="80vh" size="small">
      <el-table-column label="时间" prop="createTime" width="160"/>
      <el-table-column label="类型" width="90">
        <template #default="s">
          <el-tag :type="s.row.changeType === 2 ? 'warning' : 'success'" size="small">{{ changeTypeText(s.row.changeType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="数量" prop="quantity" width="60"/>
      <el-table-column label="变动后" prop="afterStock" width="70"/>
      <el-table-column label="操作人" prop="operator" show-overflow-tooltip/>
      <el-table-column label="备注" prop="note" show-overflow-tooltip/>
    </el-table>
    <BackPage :total="ledger.total" @changePu="ledgerPage"/>
  </el-drawer>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {Delete, Edit} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {exampleDrug, Drug} from "@/model/entiy/Drug";
import {reqDictByType} from "@/request/DictApi";
import {SysDict} from "@/model/entiy/SysDict";
import {DrugRecord, changeTypeText} from "@/model/entiy/DrugRecord";
import {Page} from "@/model/DO/Page";
import {reqAddDrug, reqDelDrug, reqDelDrugBatch, reqDrugInbound, reqDrugPage, reqDrugRecordPage, reqUpdateDrug} from "@/request/DrugApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

// 分类/单位字典（来自系统设置）
const drugTypeOpts = ref<SysDict[]>([])
const drugUnitOpts = ref<SysDict[]>([])
reqDictByType('drug_type').then(r => { drugTypeOpts.value = r || [] })
reqDictByType('drug_unit').then(r => { drugUnitOpts.value = r || [] })

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
const qp = reactive({
  drugName: '',
  drugType: '',
  numPage: 1,
  pageSize: 15
})
// 列表展示
const drugList = ref<Page<Drug>>({records: [], total: 0})
// 分页条
const changePuB = (val: PageQuery) => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
// 数据总览
const sendQuery = (): void => {
  reqDrugPage(qp).then(res => {
    drugList.value = res
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
const handleSelectionChange = (val: Drug[]): void => {
  roleIdList.value = val.map(obj => obj.drugId)
}
// 批量删除
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm(
      '确定删除选中的药品吗？历史处方不受影响。',
      '删除多个确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDelDrugBatch(roleIdList.value).then(res => sureFlush(res))
  }).catch(() => ElMessage.info('删除取消'))
}
// 删除单个
const delOne = (id: number): void => {
  ElMessageBox.confirm(
      '确定删除该药品吗？历史处方不受影响。',
      '删除单个确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqDelDrug(id).then(res => sureFlush(res))
  }).catch(() => ElMessage.info('删除取消'))
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
const modalTit = ref<"新增药品" | "修改药品">("修改药品")
// 添加
const addRoleB = (): void => {
  form.value = exampleDrug()
  myFormRef.value?.resetFields()
  modalTit.value = "新增药品"
  modalView.value = true
}
// 修改时展示模态框
const showDialog = (row: Drug) => {
  myFormRef.value?.clearValidate()
  // 赋值，防止响应式引用
  form.value = {...row}
  modalView.value = true
  modalTit.value = "修改药品"
}
// 低库存阈值（未设置则默认5）
const warnOf = (row: Drug): number => (row.warningStock && row.warningStock > 0) ? row.warningStock : 5

// 入库弹窗
const inView = ref(false)
const inDrug = ref<Drug | null>(null)
const inQty = ref(1)
const inExpire = ref<string | null>(null)
const inNote = ref('')
const openIn = (row: Drug): void => {
  inDrug.value = row
  inQty.value = 1
  inExpire.value = null
  inNote.value = ''
  inView.value = true
}
const submitIn = (): void => {
  if (!inDrug.value) return
  reqDrugInbound({
    drugId: inDrug.value.drugId,
    quantity: inQty.value,
    expireDate: inExpire.value || undefined,
    note: inNote.value || undefined
  }).then((res: Res) => {
    if (res.code === 1) {
      inView.value = false
      sendQuery()
    }
  })
}

// 出入库台账抽屉
const ledgerView = ref(false)
const ledgerTitle = ref('出入库台账')
const ledger = ref<Page<DrugRecord>>({records: [], total: 0})
const ledgerDrug = ref<Drug | null>(null)
const ledgerQp = reactive({numPage: 1, pageSize: 10})
const openLedger = (row: Drug): void => {
  ledgerDrug.value = row
  ledgerQp.numPage = 1
  ledgerTitle.value = `出入库台账 - ${row.drugName}`
  ledgerView.value = true
  loadLedger()
}
const loadLedger = (): void => {
  reqDrugRecordPage({drugId: ledgerDrug.value?.drugId, numPage: ledgerQp.numPage, pageSize: ledgerQp.pageSize})
      .then((res: Page<DrugRecord>) => { ledger.value = res })
}
const ledgerPage = (val: PageQuery): void => {
  ledgerQp.numPage = val.numPage
  ledgerQp.pageSize = val.pageSize
  loadLedger()
}

/**
 ┌───────────────────────────────────┐
 │=============表单校验相关============│
 └───────────────────────────────────┘
 */
// 表单的数据
const form = ref<Drug>(exampleDrug()) // 空的默认值
const myFormRef = ref<FormInstance>()
// 校验表单并提交
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      modalView.value = false
      if (modalTit.value == "新增药品") reqAddDrug(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改药品") reqUpdateDrug(form.value).then(res => sureFlush(res))
    }
  })
}
// 表单校验规则
const rules = reactive<FormRules>({
  "drugName": [
    {required: true, message: '请输入药品名', trigger: 'blur'},
  ],
  "drugPrice": [
    {required: true, message: '请输入单价', trigger: 'blur'},
  ],
  "drugStock": [
    {required: true, message: '请输入库存', trigger: 'blur'},
  ]
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
</style>
