<template>
  <div class="list-page">
  <!--操作框-->
  <BackOpCol role="用户" @addRole="addRoleB" @delBatch="delBatchB" @query="nowQuery">
    <el-input v-model="qp.clientName" clearable placeholder="姓名 / 手机号 / 身份证" size="large" @input="debQuery" @keyup.enter="nowQuery"/>
    <template #left>
      <span v-if="roleIdList.length > 0" class="sel-tip">
        已选 <b>{{ roleIdList.length }}</b> 项
      </span>
    </template>
  </BackOpCol>
  <!--列表展示-->
  <el-table :data="clientList.records"
            max-height="calc(100vh - 130px)"
            row-key="clientId"
            stripe
            @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="30"/>
    <el-table-column label="图像" width="60">
      <template #default="scope">
        <MyAvatar :name="scope.row.clientPhoto"/>
      </template>
    </el-table-column>
    <el-table-column label="用户名" prop="clientName" width="100"/>
    <el-table-column label="账号" prop="clientUsername" width="130"/>
    <el-table-column label="性别" width="70">
      <template #default="scope">
        <TagSex :sex="scope.row.clientGender"/>
      </template>
    </el-table-column>
    <el-table-column label="生日" prop="clientBirthday" sortable width="110"/>
    <el-table-column label="年龄" width="90">
      <template #default="scope">{{ getAge(scope.row.clientBirthday, scope.row.clientBirthdayEstimated) }}</template>
    </el-table-column>
    <el-table-column label="联系方式" prop="clientTel" width="130"/>
    <el-table-column label="状态" width="70">
      <template #default="scope">
        <el-tag v-if="scope.row.active" type="success" size="small">正常</el-tag>
        <el-tag v-else type="info" size="small">停用</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="密码状态" width="90">
      <template #default="scope">
        <el-tag v-if="scope.row.clientPwdChanged" type="success" size="small">已修改</el-tag>
        <el-tag v-else type="warning" size="small">未修改</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="会员等级" width="90">
      <template #default="scope">
        <el-tag :type="levelTag(scope.row.clientLevel)">{{ levelText(scope.row.clientLevel) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="积分" prop="clientPoints" width="70"/>
    <el-table-column label="余额" width="90">
      <template #default="scope">{{ scope.row.clientBalance ?? 0 }} 元</template>
    </el-table-column>
    <el-table-column label="简介" prop="clientInfo" min-width="120" show-overflow-tooltip/>
    <el-table-column fixed="right" label="操作" width="230">
      <template #default="scope">
        <el-button-group>
          <el-button :icon="Edit" circle type="warning" title="编辑" @click="showDialog(scope.row)"/>
          <el-button :icon="Coin" circle type="primary" title="充值余额" @click="recharge(scope.row)"/>
          <el-button v-if="scope.row.active" circle type="warning" title="停用账号" @click="toggleActive(scope.row)">停</el-button>
          <el-button v-else circle type="success" title="启用账号" @click="toggleActive(scope.row)">启</el-button>
          <el-button :icon="Key" circle type="info" title="重置密码" @click="resetPwd(scope.row)"/>
          <el-button :icon="Delete" circle type="danger" title="删除" @click="delOne(scope.row)"/>
        </el-button-group>
      </template>
    </el-table-column>
  </el-table>
  <!--分页条-->
  <BackPage :total="clientList.total" @changePu="changePuB"/>
  <!--修改、新增时的模态框-->
  <el-dialog v-model="modalView" :title="modalTit" draggable width="60%"
               :close-on-click-modal="false" :close-on-press-escape="false"
               @open="onDialogOpen">
    <!--头像上传框-->
    <UpImg :photoNm="form.clientPhoto" @upPhoto="changePhoto"/>
    <!--表单-->
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="姓名" prop="clientName">
        <el-input v-model="form.clientName" clearable placeholder="用户姓名"/>
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.clientGender">
          <el-radio :label="true" border>男</el-radio>
          <el-radio :label="false" border>女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="modalTit==='新增用户'" label="密码">
        <el-input v-model="form.clientPassword" clearable placeholder="123456"/>
      </el-form-item>

      <el-divider content-position="left">生日与年龄</el-divider>
      <el-form-item label="生日">
        <el-date-picker
            v-model="form.clientBirthday"
            format="YYYY-MM-DD"
            placeholder="选择出生日期"
            type="date"
            value-format="YYYY-MM-DD"
            @change="onBirthdayChange"
        />
      </el-form-item>
      <el-form-item label="年龄">
        <el-input
            v-model="ageInput"
            clearable
            placeholder="例如：35岁 / 约35岁"
            @change="onAgeInput"
        />
      </el-form-item>

      <el-divider content-position="left">联系方式</el-divider>
      <el-form-item label="手机号" prop="clientTel">
        <el-input v-model="form.clientTel" maxlength="11" show-word-limit inputmode="numeric" @input="sanitizeTel" clearable placeholder="11位手机号"/>
        <div style="font-size: 12px; color: #999; margin-top: 4px;">
          手机号即登录账号（初始密码 123456），顾客用它登录即可查看宠物和就诊记录
        </div>
      </el-form-item>
      <el-form-item label="备用电话">
        <el-input v-model="form.clientTel2" clearable placeholder="选填"/>
      </el-form-item>
      <el-form-item label="地址">
        <el-input v-model="form.clientAddress" clearable placeholder="选填"/>
      </el-form-item>
      <el-form-item label="身份证">
        <el-input v-model="form.clientIdCard" clearable placeholder="选填"/>
      </el-form-item>

      <el-divider content-position="left">会员信息</el-divider>
      <el-form-item label="会员等级">
        <el-select v-model="form.clientLevel" style="width: 200px;">
          <el-option :value="0" label="普通"/>
          <el-option :value="1" label="银卡"/>
          <el-option :value="2" label="金卡"/>
          <el-option :value="3" label="钻石"/>
        </el-select>
      </el-form-item>
      <el-form-item label="积分">
        <el-input-number v-model="form.clientPoints" :min="0" controls-position="right"/>
      </el-form-item>
      <el-form-item label="余额">
        <el-input-number v-model="form.clientBalance" :min="0" :precision="2" :step="10" controls-position="right"/>
      </el-form-item>
      <el-form-item label="客户来源">
        <el-select v-model="form.clientSource" clearable placeholder="选填" style="width: 200px;">
          <el-option value="自然到店" label="自然到店"/>
          <el-option value="线上" label="线上"/>
          <el-option value="转介绍" label="转介绍"/>
          <el-option value="其他" label="其他"/>
        </el-select>
      </el-form-item>
      <el-form-item label="黑名单">
        <el-radio-group v-model="form.clientBlacklist">
          <el-radio :label="false" border>正常</el-radio>
          <el-radio :label="true" border>拉黑</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-divider content-position="left">其他</el-divider>
      <el-form-item label="简介">
        <el-input v-model="form.clientInfo" type="textarea" :rows="2" placeholder="用户近况"/>
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
  </div>
</template>

<script lang="ts" setup>

import {onMounted, reactive, ref} from 'vue'

const ageInput = ref<string>('')
const onDialogOpen = () => {
  ageInput.value = form.value.clientBirthday ? getAge(form.value.clientBirthday) : ''
}
const onBirthdayChange = () => {
  ageInput.value = form.value.clientBirthday ? getAge(form.value.clientBirthday) : ''
  form.value.clientBirthdayEstimated = false
}
const onAgeInput = () => {
  if (!ageInput.value) return
  const r = parseAgeToBirthday(ageInput.value)
  if (r) {
    form.value.clientBirthday = r.birthday
    form.value.clientBirthdayEstimated = r.estimated
  }
}
import {Coin, Delete, Edit, Key} from '@element-plus/icons-vue'
import BackOpCol from "@/components/row/BackOpCol.vue";
import {debounce} from "@/utils/DebounceUtil";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery, YClientList} from "@/model/VO/BackQuery";
import {getAge, parseAgeToBirthday} from "@/utils/TimeUtil";
import {Client, exampleClient} from "@/model/entiy/Client";
import TagSex from "@/components/show/TagSex.vue";
import {reqAddClient, reqActivateClient, reqCheckClientRef, reqClientList, reqDeactivateClient, reqDelClient, reqDelClientBatch, reqRechargeClient, reqResetPwd, reqUpdateClient} from "@/request/ClientApi";
import {Page} from "@/model/DO/Page";
import {ElMessage, ElMessageBox, FormInstance, FormRules} from "element-plus";
import {Res} from "@/request/Res";
import UpImg from "@/components/UpImg.vue";
import MyAvatar from "@/components/show/MyAvatar.vue";


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
const qp: YClientList = reactive({
  clientName: '',
  numPage: 1,
  pageSize: 15
})
// 列表展示
const clientList = ref<Page<Client>>({records: [], total: 0})
// 分页条
const changePuB = (val: PageQuery) => {
  qp.numPage = val.numPage
  qp.pageSize = val.pageSize
  sendQuery()
}
// 数据总览
const sendQuery = (): void => {
  reqClientList(qp).then(res => {
    clientList.value = res
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
const handleSelectionChange = (val: Client[]): void => {
  roleIdList.value = val.map(obj => obj.clientId)
}
// 批量删除
const delBatchB = (): void => {
  if (roleIdList.value.length == 0) return
  ElMessageBox.confirm(
      '这些被选中者，将迎来一场浩大而又凄美的结局。这是对他们选择和坚持的回报，是既悲痛又美丽的终结。',
      '删除多个确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqDelClientBatch(roleIdList.value).then(res => sureFlush(res))
  }).catch(() => {
    ElMessage.info('删除取消')
  })
}
// 删除单个
// 为客户充值余额
const recharge = (row: Client): void => {
  ElMessageBox.prompt(`客户【${row.clientName}】当前余额 ￥${row.clientBalance ?? 0}，输入充值金额：`, '充值余额', {
    confirmButtonText: '充值',
    cancelButtonText: '取消',
    inputPattern: /^([1-9]\d{0,4})(\.\d{1,2})?$/,
    inputErrorMessage: '请输入 0.01~99999 之间的金额'
  }).then(({value}) => {
    reqRechargeClient(row.clientId, Number(value)).then(res => {
      if (res.code === 1) sendQuery()
    })
  }).catch(() => ElMessage.info('取消充值'))
}

const delOne = (row: Client): void => {
  // 先检查关联
  reqCheckClientRef(row.clientId).then(refRes => {
    const canDelete = refRes.code === 1
    const msg = canDelete
        ? `确认彻底删除客户【${row.clientName}】吗？该客户名下没有任何宠物或预约，删除后可在回收站恢复。`
        : `${refRes.msg}\n\n是否改为「停用」？停用后该客户不再出现在默认列表，但历史数据保留。`

    if (canDelete) {
      ElMessageBox.confirm(msg, '删除确认', {
        confirmButtonText: '彻底删除',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        reqDelClient(row.clientId).then(res => sureFlush(res))
      }).catch(() => ElMessage.info('已取消'))
    } else {
      ElMessageBox.confirm(msg, '改为停用？', {
        confirmButtonText: '停用',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        reqDeactivateClient(row.clientId).then(res => {
          if (res.code === 1) {
            ElMessage.success('已停用')
            sendQuery()
          } else ElMessage.error(res.msg || '操作失败')
        })
      }).catch(() => ElMessage.info('已取消'))
    }
  })
}

// 确定请求的返回值，然后刷新
const sureFlush = (res: Res): void => {
  if (res.code === 1) sendQuery()
}

// 启用/停用
const toggleActive = (row: Client): void => {
  const api = row.active ? reqDeactivateClient : reqActivateClient
  const action = row.active ? '停用' : '启用'
  ElMessageBox.confirm(`确认${action}客户【${row.clientName}】吗？`, `${action}确认`, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    api(row.clientId).then(res => {
      if (res.code === 1) {
        ElMessage.success(action + '成功')
        sendQuery()
      } else ElMessage.error(res.msg || '操作失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}

// 重置密码为 123456
const resetPwd = (row: Client): void => {
  ElMessageBox.confirm(
      `确认将【${row.clientName}】的密码重置为 123456 吗？`,
      '重置密码确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    reqResetPwd(row.clientId).then((res: Res) => {
      if (res.code === 1) ElMessage.success('密码已重置为 123456')
      else ElMessage.error(res.msg || '重置失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}

/**
 ┌───────────────────────────────────┐
 │=============新增修改按钮============│
 └───────────────────────────────────┘
 */
// 模态框
const modalView = ref(false)
const modalTit = ref<"新增用户" | "修改用户">("修改用户")
// 新增时展示模态框
const addRoleB = (): void => {
  form.value = exampleClient()
  myFormRef.value?.resetFields()
  modalTit.value = "新增用户"
  modalView.value = true
}
// 修改时展示模态框
const showDialog = (row: Client) => {
  myFormRef.value?.clearValidate()
  // 赋值，防止响应式引用
  form.value.clientId = row.clientId
  form.value.clientName = row.clientName
  form.value.clientUsername = row.clientUsername
  form.value.clientGender = row.clientGender
  form.value.clientBirthday = row.clientBirthday
  form.value.clientTel = row.clientTel
  form.value.clientBirthdayEstimated = row.clientBirthdayEstimated
  form.value.clientTel2 = row.clientTel2
  form.value.clientAddress = row.clientAddress
  form.value.clientIdCard = row.clientIdCard
  form.value.clientLevel = row.clientLevel
  form.value.clientPoints = row.clientPoints
  form.value.clientBalance = row.clientBalance
  form.value.clientSource = row.clientSource
  form.value.clientBlacklist = row.clientBlacklist
  form.value.clientInfo = row.clientInfo
  form.value.clientPhoto = row.clientPhoto
  // 显示
  modalView.value = true
  modalTit.value = "修改用户"
}

/**
 ┌───────────────────────────────────┐
 │=============表单校验相关============│
 └───────────────────────────────────┘
 */
// 表单的数据
const levelText = (lv: number) => ['普通', '银卡', '金卡', '钻石'][lv] || '普通'
const levelTag = (lv: number) => ['info', '', 'warning', 'danger'][lv] || 'info'

const sanitizeTel = () => {
  if (form.value.clientTel) {
    form.value.clientTel = form.value.clientTel.replace(/\D/g, '').slice(0, 11)
  }
}

const form = ref<Client>(exampleClient()) // 空的默认值
const myFormRef = ref<FormInstance>()
// 校验表单并提交
const formSubmit = async (): Promise<void> => {
  try {
    await myFormRef.value?.validate()
  } catch {
    return
  }
  const api = modalTit.value === "新增用户" ? reqAddClient : reqUpdateClient
  api(form.value).then((res: Res) => {
    if (res.code === 1) {
      modalView.value = false
      sendQuery()
      ElMessage.success(modalTit.value === "新增用户" ? '添加成功' : '修改成功')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}
// 表单校验规则
const rules = reactive<FormRules>({
  "clientName": [
    {required: true, message: '请输入姓名', trigger: 'blur'},
  ],
  "clientTel": [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur'},
  ],
})
// 头像参数的改变:子传父
const changePhoto = (val: string): void => {
  form.value.clientPhoto = val
}
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

.list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.list-page :deep(.el-table) {
  flex: 1;
  min-height: 0;
}


</style>
