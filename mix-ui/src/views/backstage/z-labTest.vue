<template>
  <!--操作框-->
  <BackOpCol role="检验项" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.testName" clearable placeholder="检验项名" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-select v-model="qp.testGroup" clearable placeholder="全部分组" size="large" style="width: 110px" @change="nowQuery">
      <el-option v-for="t in labGroupOpts" :key="t.dictId" :label="t.dictKey" :value="t.dictKey"/>
    </el-select>
    <el-select v-model="qp.isPanel" clearable placeholder="全部类型" size="large" style="width: 110px" @change="nowQuery">
      <el-option label="单项" :value="0"/>
      <el-option label="套组" :value="1"/>
    </el-select>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">
        已选 <b>{{ roleIdList.length }}</b> 项
      </span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="list.records"
            max-height="calc(100vh - 130px)"
            row-key="testId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="类型" width="80">
      <template #default="scope">
        <el-tag v-if="scope.row.isPanel" size="small" type="warning">套组</el-tag>
        <el-tag v-else size="small">单项</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="检验项/套组" min-width="160" prop="testName" show-overflow-tooltip/>
    <el-table-column label="分组" width="90">
      <template #default="scope">
        <el-tag size="small">{{ scope.row.testGroup || '未分组' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="单位" width="90" prop="testUnit" show-overflow-tooltip/>
    <el-table-column label="参考范围" min-width="120" prop="refRange" show-overflow-tooltip/>
    <el-table-column label="单价/开单价(元)" width="120">
      <template #default="scope">{{ scope.row.isPanel ? (scope.row.panelPrice ?? '-') : (scope.row.testPrice ?? '-') }}</template>
    </el-table-column>
    <el-table-column label="进价(元)" width="100">
      <template #default="scope">{{ scope.row.isPanel && scope.row.panelCost != null ? scope.row.panelCost : '—' }}</template>
    </el-table-column>
    <el-table-column label="状态" width="80">
      <template #default="scope">
        <el-tag v-if="scope.row.active" size="small" type="success">启用</el-tag>
        <el-tag v-else size="small" type="info">停用</el-tag>
      </template>
    </el-table-column>
    <el-table-column fixed="right" label="操作" width="120">
      <template #default="scope">
        <el-button-group>
          <el-button :icon="Edit" circle title="编辑" type="warning" @click="showDialog(scope.row)"/>
          <el-button :icon="Delete" circle title="删除" type="danger" @click="delOne(scope.row)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="list.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="560px" :close-on-click-modal="false">
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="是否套组">
        <el-switch v-model="form.isPanel" active-text="套组(一次收费)" inactive-text="单项" @change="onPanelSwitch"/>
      </el-form-item>
      <el-form-item label="名称" prop="testName">
        <el-input v-model="form.testName" clearable maxlength="64" :placeholder="form.isPanel ? '如：生化30项' : '如：白细胞计数'"/>
      </el-form-item>
      <el-form-item label="分组" prop="testGroup">
        <el-select v-model="form.testGroup" filterable placeholder="选择分组（在系统设置维护）">
          <el-option v-for="t in labGroupOpts" :key="t.dictId" :label="t.dictKey" :value="t.dictKey"/>
        </el-select>
      </el-form-item>

      <!-- 单项字段 -->
      <template v-if="!form.isPanel">
        <el-form-item label="单位" prop="testUnit">
          <el-input v-model="form.testUnit" clearable maxlength="32" placeholder="如：10^9/L"/>
        </el-form-item>
        <el-form-item label="参考范围" prop="refRange">
          <el-input v-model="form.refRange" clearable maxlength="64" placeholder="如：6.0-17.0"/>
        </el-form-item>
        <el-form-item label="单价(元)" prop="testPrice">
          <el-input-number v-model="form.testPrice" :max="99999" :min="0" :precision="2" :step="1" style="width: 100%"/>
        </el-form-item>
      </template>

      <!-- 套组字段 -->
      <template v-else>
        <el-form-item label="开单价(元)" prop="panelPrice">
          <el-input-number v-model="form.panelPrice" :max="99999" :min="0" :precision="2" :step="1" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="进价(元)" prop="panelCost">
          <el-input-number v-model="form.panelCost" :max="99999" :min="0" :precision="2" :step="1" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="成员项目" prop="memberIds">
          <el-select v-model="memberIds" multiple filterable placeholder="选择该套组包含的单项的项目" style="width: 100%">
            <el-option-group v-for="g in singleGroupKeys" :key="g" :label="g">
              <el-option v-for="t in singleByGroup[g]" :key="t.testId" :label="t.testName" :value="t.testId"/>
            </el-option-group>
          </el-select>
          <div class="member-tip">套组按一次开单价收费，下面成员只录结果、不重复计费</div>
        </el-form-item>
      </template>

      <el-form-item label="状态">
        <el-switch v-model="form.active" active-text="启用" inactive-text="停用"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="modalView = false">取消</el-button>
      <el-button type="primary" @click="formSubmit">确认</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref, computed} from 'vue'
import {Delete, Edit} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {exampleLabTest, LabTest} from "@/model/entiy/LabTest";
import {reqDictByType} from "@/request/DictApi";
import {SysDict} from "@/model/entiy/SysDict";
import {Page} from "@/model/DO/Page";
import {reqAddLabPanel, reqAddLabTest, reqDelLabPanel, reqDelLabTest, reqLabPanels, reqLabTestList, reqLabTestPage, reqUpdateLabPanel, reqUpdateLabTest} from "@/request/LabApi";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";

// 化验分组字典（来自系统设置）
const labGroupOpts = ref<SysDict[]>([])
reqDictByType('lab_group').then(r => { labGroupOpts.value = r || [] })

/**
 ┌───────────────────────────────────┐
 │=============表格查询相关============│
 └───────────────────────────────────┘
 */
const qp = reactive({
  testName: '',
  testGroup: '',
  isPanel: null as number | null,
  numPage: 1,
  pageSize: 20
})
const list = ref<Page<LabTest>>({records: [], total: 0})
const changePuB = (val: PageQuery): void => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
const sendQuery = (): void => {
  // 「全部类型」时不带 isPanel 参数（后端 null = 不过滤），避免依赖 axios 对 null 的序列化行为
  const {isPanel, ...rest} = qp
  const params = isPanel == null ? rest : {...rest, isPanel}
  reqLabTestPage(params).then(res => {
    list.value = res
  })
}
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
const roleIdList = ref<number[]>([])
const handleSelectionChange = (val: LabTest[]): void => {
  roleIdList.value = val.map(obj => obj.testId)
}
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm(
      '确定删除选中的检验项吗？已出化验单保留历史快照不受影响。',
      '删除多个确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    batchDel(roleIdList.value)
  }).catch(() => ElMessage.info('删除取消'))
}
const delOne = (row: LabTest): void => {
  ElMessageBox.confirm(
      row.testId !== undefined && row.testId > 0
          ? '确定删除该检验项吗？已出化验单保留历史快照不受影响。'
          : '该项已被历史化验单引用，将改为停用。',
      '删除确认',
      {confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    const fn = row.isPanel ? reqDelLabPanel : reqDelLabTest
    fn(row.testId).then(res => sureFlush(res))
  }).catch(() => ElMessage.info('删除取消'))
}
const batchDel = (ids: number[]): void => {
  Promise.all(ids.map(id => {
    const row = list.value.records.find(r => r.testId === id)
    return (row?.isPanel ? reqDelLabPanel : reqDelLabTest)(id)
  })).then(() => sendQuery())
}
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}

/**
 ┌───────────────────────────────────┐
 │=============成员单项源============│
 └───────────────────────────────────┘
 */
const singleTests = ref<LabTest[]>([])
reqLabTestList().then(res => { singleTests.value = (res || []).filter(t => !t.isPanel) })
const singleByGroup = computed(() => {
  const map: Record<string, LabTest[]> = {}
  singleTests.value.forEach(t => {
    const g = t.testGroup || '其他'
    if (!map[g]) map[g] = []
    map[g].push(t)
  })
  return map
})
const singleGroupKeys = computed(() => Object.keys(singleByGroup.value))

/**
 ┌───────────────────────────────────┐
 │=============新增修改按钮============│
 └───────────────────────────────────┘
 */
const modalView = ref(false)
const modalTit = ref<"新增检验项" | "修改检验项">("修改检验项")
const memberIds = ref<number[]>([])
const addRoleB = (): void => {
  form.value = exampleLabTest()
  memberIds.value = []
  myFormRef.value?.resetFields()
  modalTit.value = "新增检验项"
  modalView.value = true
}
const showDialog = (row: LabTest): void => {
  myFormRef.value?.clearValidate()
  form.value = {...row}
  memberIds.value = []
  if (row.isPanel && row.testId) {
    // 载入套组成员
    reqLabPanels().then(panels => {
      const p = (panels || []).find(x => x.panel.testId === row.testId)
      memberIds.value = (p?.members || []).map(m => m.testId)
    })
  }
  modalView.value = true
  modalTit.value = "修改检验项"
}
const onPanelSwitch = (): void => {
  // 切换类型时清空对方专属字段，避免脏数据
  if (form.value.isPanel) { form.value.testPrice = 0; form.value.testUnit = ''; form.value.refRange = '' }
  else { form.value.panelPrice = 0; form.value.panelCost = 0; memberIds.value = [] }
}

/**
 ┌───────────────────────────────────┐
 │=============表单校验相关========────│
 └───────────────────────────────────┘
 */
const form = ref<LabTest>(exampleLabTest())
const myFormRef = ref<FormInstance>()
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (!valid) return
    modalView.value = false
    const isPanel = !!form.value.isPanel
    if (modalTit.value == "新增检验项") {
      if (isPanel) reqAddLabPanel({panel: form.value, memberIds: memberIds.value}).then(res => sureFlush(res))
      else reqAddLabTest(form.value).then(res => sureFlush(res))
    } else {
      if (isPanel) reqUpdateLabPanel({panel: form.value, memberIds: memberIds.value}).then(res => sureFlush(res))
      else reqUpdateLabTest(form.value).then(res => sureFlush(res))
    }
  })
}
const rules = reactive<FormRules>({
  "testName": [
    {required: true, message: '请输入名称', trigger: 'blur'},
  ]
})

/**
 ┌───────────────────────────────────┐
 │=============生命周期相关============│
 └───────────────────────────────────┘
 */
onMounted(() => {
  sendQuery()
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
.member-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
