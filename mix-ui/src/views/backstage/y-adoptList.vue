<template>
  <!--操作框-->
  <BackOpCol role="领养" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.clientName" clearable placeholder="申请人姓名 / 手机号" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <el-input v-model="qp.petName" clearable placeholder="宠物名 / 品种" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">
        已选 <b>{{ roleIdList.length }}</b> 项
      </span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="adoptList.records"
            max-height="calc(100vh - 130px)"
            row-key="adoptId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="简介" prop="adoptInfo"/>
    <el-table-column label="定金" prop="adoptMoney" sortable/>
    <el-table-column :formatter="removeT" label="创建时间" prop="createTime" sortable/>
    <el-table-column label="宠物名" prop="petName"/>
    <el-table-column label="申请人" prop="clientName"/>
    <el-table-column label="是否通过" prop="inAdopt" sortable>
      <template #default="scope">
        <el-tag v-if="scope.row.inAdopt===2">待审核</el-tag>
        <el-tag v-else-if="scope.row.inAdopt===1" type="success">已通过</el-tag>
        <el-tag v-else-if="scope.row.inAdopt===3" type="info">已退养</el-tag>
        <el-tag v-else type="danger">已拒绝</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="拒绝理由" width="180">
      <template #default="scope">
        <el-tooltip v-if="scope.row.inAdopt===0 && scope.row.refuseReason"
                    :content="scope.row.refuseReason" placement="top">
          <span style="color:#f56c6c">{{ scope.row.refuseReason }}</span>
        </el-tooltip>
        <span v-else>—</span>
      </template>
    </el-table-column>
    <el-table-column fixed="right" label="操作">
      <template #default="scope">
        <el-button-group>
          <el-button v-if="scope.row.inAdopt===2" :icon="Check" circle type="success"
                     @click="adoptAdopt(scope.row.adoptId,true)"/>
          <el-button v-if="scope.row.inAdopt===2" :icon="Close" circle type="info"
                     @click="adoptAdopt(scope.row.adoptId,false)"/>
          <el-button v-if="scope.row.inAdopt===1" circle type="warning"
                     @click="undoAdopt(scope.row.adoptId)">撤</el-button>
          <el-button v-if="scope.row.inAdopt===1" :icon="Edit" circle type="warning"
                     @click="showDialog(scope.row)"/>
          <el-button v-if="scope.row.inAdopt===0" :icon="Refresh" circle type="success"
                     @click="restartAdopt(scope.row.adoptId)"/>
          <el-button v-if="scope.row.inAdopt!==1" :icon="Delete" circle type="danger" @click="delOne(scope.row.adoptId)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="adoptList.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="60%">
    <!--表单-->
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="简介" prop="adoptInfo">
        <el-input v-model="form.adoptInfo" clearable placeholder="无"/>
      </el-form-item>
      <el-form-item label="定金" prop="adoptMoney">
        <el-input-number
            v-model="form.adoptMoney"
            :max="5000"
            :min="100"
            :step="100"
        />
      </el-form-item>
      <el-form-item label="宠物" prop="petId">
        <el-select v-model="form.petId" filterable placeholder="选择宠物">
          <el-option v-for="d in petNameL" :key="d.roleId" :label="d.roleName" :value="d.roleId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="申请人" prop="clientId">
        <el-select-v2 v-model="form.clientId" filterable :options="clientOpts"
                      placeholder="输入姓名检索用户" style="width:100%"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit">
          确认
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue'
import {Check, Close, Delete, Edit, Refresh} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery, YAdoptList} from "@/model/VO/BackQuery";
import {Adopt, exampleAdopt} from "@/model/entiy/Adopt";
import {AdoptDto} from "@/model/DO/AdoptDto";
import {moveT} from "@/utils/TimeUtil";
import {
  reqAddAdopt,
  reqAdoptAdopt,
  reqAdoptList,
  reqDelAdopt,
  reqDelAdoptBatch,
  reqUndoAdopt,
  reqRestartAdopt,
  reqUpdateAdopt
} from "@/request/AdoptApi";
import {Page} from "@/model/DO/Page";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";
import {NameDo} from "@/model/DO/NameDo";
import {reqPetNoClient} from "@/request/PetApi";
import {reqClientName} from "@/request/ClientApi";

/**
 ┌───────────────────────────────────┐
 │=============生命周期相关============│
 └───────────────────────────────────┘
 */
onMounted(() => {
  sendQuery()
  reqClientName().then(res => {
    clientNameL.value = res
  })
  reqPetNoClient().then(res => {
    petNameL.value = res
  })
})

/**
 ┌───────────────────────────────────┐
 │=============表格查询相关============│
 └───────────────────────────────────┘
 */
// 查询的参数
const qp: YAdoptList = reactive({
  petName: '',
  clientName: '',
  numPage: 1,
  pageSize: 20
})
// 列表展示
const adoptList = ref<Page<AdoptDto>>({records: [], total: 0})
const removeT = (row: Adopt) => {
  return moveT(row.createTime)
}
// 分页条
const changePuB = (val: PageQuery) => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
// 数据总览
const sendQuery = (): void => {
  reqAdoptList(qp).then(res => {
    adoptList.value = res
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
const handleSelectionChange = (val: Adopt[]): void => {
  roleIdList.value = val.map(obj => obj.adoptId)
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
    reqDelAdoptBatch(roleIdList.value).then(res => sureFlush(res))
  }).catch(() => {
    ElMessage.info('删除取消')
  })
}
// 删除单个
const delOne = (id: number): void => {
  ElMessageBox.confirm(
      '确定删除这条领养单吗？删除后不可恢复（不影响宠物档案）。',
      '删除确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqDelAdopt(id).then(res => sureFlush(res))
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
const modalTit = ref<"新增领养" | "修改领养">("修改领养")
// 新增
const addRoleB = (): void => {
  form.value = exampleAdopt()
  myFormRef.value?.resetFields()
  modalTit.value = "新增领养"
  modalView.value = true
}
// 修改时展示模态框
const showDialog = (row: Adopt) => {
  myFormRef.value?.clearValidate()
  // 赋值，防止响应式引用
  form.value.adoptId = row.adoptId
  form.value.adoptInfo = row.adoptInfo
  form.value.adoptMoney = row.adoptMoney
  form.value.petId = row.petId
  form.value.clientId = row.clientId
  // 关键：把审核状态也带上，避免提交时被 exampleAdopt() 的默认值 2 覆盖
  form.value.inAdopt = row.inAdopt
  // ===赋值完成===
  modalView.value = true
  modalTit.value = "修改领养"
}
// 审核通过
const adoptAdopt = (adoptId: number, isAdopt: boolean) => {
  if (isAdopt) {
    reqAdoptAdopt({adoptId, isAdopt: true}).then(res => sureFlush(res))
  } else {
    ElMessageBox.prompt('请填写拒绝理由', '拒绝领养', {
      confirmButtonText: '确定拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：家庭环境不适合',
      inputValidator: (v: string) => (v && v.trim().length > 0) ? true : '理由不能为空',
      type: 'warning',
    }).then(({value}) => {
      reqAdoptAdopt({adoptId, isAdopt: false, refuseReason: value.trim()})
          .then(res => sureFlush(res))
    }).catch(() => ElMessage.info('已取消'))
  }
}

// 重启被拒绝的领养单
const restartAdopt = (adoptId: number): void => {
  ElMessageBox.confirm(
      '重启后该单子回到待审核状态，拒绝理由清空。确定重启吗？',
      '重启领养单',
      {confirmButtonText: '重启', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqRestartAdopt(adoptId).then(res => {
      if (res.code === 1) {
        ElMessage.success('已重启')
        sendQuery()
      } else {
        ElMessage.error(res.msg || '重启失败')
      }
    })
  }).catch(() => ElMessage.info('已取消'))
}

// 撤回已通过的领养
const undoAdopt = (adoptId: number): void => {
  ElMessageBox.confirm(
      '撤回后宠物将重新变为无主状态，出现在待领养列表。确定撤回吗？',
      '撤回领养',
      {
        confirmButtonText: '撤回',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqUndoAdopt(adoptId).then(res => {
      if (res.code === 1) {
        ElMessage.success('已撤回')
        sendQuery()
      } else {
        ElMessage.error(res.msg || '撤回失败')
      }
    })
  }).catch(() => ElMessage.info('已取消'))
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
const form = ref<Adopt>(exampleAdopt()) // 空的默认值
const myFormRef = ref<FormInstance>()
// 校验表单并提交
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      // 校验通过，提交
      modalView.value = false
      if (modalTit.value == "新增领养") reqAddAdopt(form.value).then(res => sureFlush(res))
      else if (modalTit.value === "修改领养") reqUpdateAdopt(form.value).then(res => sureFlush(res))
      else ElMessageBox.alert('操作未完成，请检查填写内容后重试')
    }
  })
}
// 表单校验规则
const rules = reactive<FormRules>({
  "adoptInfo": [
    {required: true, message: '请输入描述', trigger: 'blur'},
  ],
  "adoptMoney": [
    {required: true, message: '请给钱', trigger: 'blur'},
  ],
  "clientId": [
    {required: true, message: '请选择用户', trigger: 'blur'},
  ],
  "petId": [
    {required: true, message: '请选择宠物', trigger: 'blur'},
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
