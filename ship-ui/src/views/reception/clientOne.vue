<template>
  <DetailTable
      v-if="!modalView"
      :role-photo="client.clientPhoto"
      :tab-con='[
        {tit: "姓名", con: client.clientName},
        {tit: "性别", con: client.clientGender ? "男" : "女"},
        {tit: "电话", con: client.clientTel || "--"},
        {tit: "备用电话", con: client.clientTel2 || "--"},
        {tit: "邮箱", con: client.clientEmail || "--"},
        {tit: "出生日期", con: (client.clientBirthdayEstimated ? "约 " : "") + (client.clientBirthday || "--")},
        {tit: "年龄", con: getAge(client.clientBirthday, client.clientBirthdayEstimated)},
        {tit: "地址", con: client.clientAddress || "--"},
        {tit: "身份证", con: client.clientIdCard || "--"},
        {tit: "会员等级", con: levelText(client.clientLevel)},
        {tit: "积分", con: client.clientPoints ?? 0},
        {tit: "余额", con: (client.clientBalance ?? 0) + " 元"},
        {tit: "客户来源", con: client.clientSource || "--"},
        {tit: "黑名单", con: client.clientBlacklist ? "是" : "否"},
        {tit: "简介", con: client.clientInfo || "--"},
      ]'
      @upPwd="openPwdDialog"
      @upRole="updateRole"
  />
  <div v-if="!modalView" style="text-align: center; margin-top: 8px;">
    <el-button type="info" @click="openEmailDialog">绑定/修改邮箱</el-button>
  </div>
  <!--修改页面-->
  <div v-show="modalView">
    <h2>修改个人资料</h2>
    <!--头像上传框-->
    <UpImg :photoNm="form.clientPhoto" @upPhoto="changePhoto"/>
    <!--表单-->
    <el-form ref="myFormRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="姓名" prop="clientName">
        <el-input v-model="form.clientName" clearable placeholder="用户姓名"/>
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group v-model="form.clientGender">
          <el-radio :label="true" border>男</el-radio>
          <el-radio :label="false" border>女</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="生日">
        <el-date-picker
            v-model="form.clientBirthday"
            format="YYYY-MM-DD"
            placeholder="选择出生日期"
            type="date"
            value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="clientTel">
        <el-input v-model="form.clientTel" maxlength="11" show-word-limit inputmode="numeric" @input="sanitizeTel" clearable placeholder="11位手机号"/>
        <div style="font-size: 12px; color: #999; margin-top: 4px;">
          手机号即登录账号，修改后请用新手机号登录
        </div>
      </el-form-item>
      <el-form-item label="备用电话">
        <el-input v-model="form.clientTel2" clearable placeholder="选填"/>
      </el-form-item>
      <el-form-item label="地址">
        <el-input v-model="form.clientAddress" clearable placeholder="选填"/>
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.clientInfo" type="textarea" :rows="2" placeholder="用户近况"/>
      </el-form-item>
    </el-form>
    <span class="dialog-footer">
        <el-button @click="modalView = false">取消</el-button>
        <el-button type="primary" @click="formSubmit">
          确认
        </el-button>
      </span>
  </div>
  <!-- 修改密码弹窗 -->
  <el-dialog v-model="pwdDialog" title="修改密码" width="420px" :close-on-click-modal="false">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
      <el-form-item label="原密码" prop="oldPwd">
        <el-input v-model="pwdForm.oldPwd" type="password" show-password placeholder="请输入原密码"/>
      </el-form-item>
      <el-form-item label="新密码" prop="newPwd">
        <el-input v-model="pwdForm.newPwd" type="password" show-password placeholder="至少6位"/>
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPwd">
        <el-input v-model="pwdForm.confirmPwd" type="password" show-password placeholder="再次输入新密码"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialog = false">取消</el-button>
      <el-button type="primary" @click="submitPwd">确认修改</el-button>
    </template>
  </el-dialog>

  <!-- 绑定邮箱弹窗 -->
  <el-dialog v-model="emailDialog" title="绑定/修改邮箱" width="90%" style="max-width: 480px;" :close-on-click-modal="false">
    <div style="font-size: 12px; color: #999; margin-bottom: 12px;">
      邮箱用于找回密码。绑定后如果忘记密码，可自行通过邮箱重置。
    </div>
    <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="70px">
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="emailForm.email" placeholder="your@qq.com"/>
      </el-form-item>
      <el-form-item label="验证码" prop="code">
        <div style="display: flex; gap: 8px; width: 100%;">
          <el-input v-model="emailForm.code" style="flex: 1;" placeholder="4位验证码"/>
          <el-button :disabled="mailBtn.dis" @click="sendMailCode" style="flex-shrink: 0;">{{ mailBtn.btnTxt }}</el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emailDialog = false">取消</el-button>
      <el-button type="primary" @click="submitEmail">确认绑定</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {Client, exampleClient} from "@/model/entiy/Client";
import DetailTable from "@/components/card/DetailTable.vue";
import {onMounted, reactive, ref} from "vue";
import {getAge} from '@/utils/TimeUtil'
import {reqLoginUser} from "@/request/PowerApi";
import UpImg from "@/components/UpImg.vue";
import {FormInstance, FormRules} from "element-plus";
import {reqUpdateClientSelf, reqUpdatePwd, reqUpdateEmail} from "@/request/ClientApi";
import {reqSendRegMail} from "@/request/PowerApi";
import {ElMessage} from "element-plus";
const levelText = (lv: number) => ['普通', '银卡', '金卡', '钻石'][lv] || '普通'

// 设置展示信息
const client = ref<Client>(exampleClient())
// 获取用户信息
onMounted(() => {
  reqLoginUser().then(res => {
    client.value = res
  })
})

/**
 ┌───────────────────────────────────┐
 │=============修改个人资料============│
 └───────────────────────────────────┘
 */
// 模态框
const modalView = ref(false)
const form = reactive<Client>(exampleClient()) // 空的默认值
// 修改时展示模态框
const updateRole = () => {
  myFormRef.value?.clearValidate()
  // 赋值，防止响应式引用
  form.clientName = client.value.clientName
  form.clientUsername = client.value.clientUsername
  form.clientGender = client.value.clientGender
  form.clientBirthday = client.value.clientBirthday
  form.clientBirthdayEstimated = client.value.clientBirthdayEstimated
  form.clientTel = client.value.clientTel
  form.clientTel2 = client.value.clientTel2
  form.clientAddress = client.value.clientAddress
  form.clientInfo = client.value.clientInfo
  form.clientPhoto = client.value.clientPhoto
  // 显示
  modalView.value = true
}
// 手机号只保留数字，且最多 11 位
const sanitizeTel = () => {
  if (form.clientTel) {
    form.clientTel = form.clientTel.replace(/\D/g, '').slice(0, 11)
  }
}

// 表单的数据
const myFormRef = ref<FormInstance>()
// 校验表单并提交
const formSubmit = async (): Promise<void> => {
  await myFormRef.value?.validate((valid: boolean): void => {
    if (valid) {
      // 校验通过，提交
      reqUpdateClientSelf(form).then(res => {
        if (res.code === 1) location.reload()
      })
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
  form.clientPhoto = val
}

/*============ 修改密码 ============*/
const pwdDialog = ref(false)
const pwdForm = reactive({oldPwd: '', newPwd: '', confirmPwd: ''})
const pwdFormRef = ref<FormInstance>()
const pwdRules = reactive<FormRules>({
  oldPwd: [{required: true, message: '请输入原密码', trigger: 'blur'}],
  newPwd: [
    {required: true, message: '请输入新密码', trigger: 'blur'},
    {min: 6, message: '新密码至少6位', trigger: 'blur'},
  ],
  confirmPwd: [
    {required: true, message: '请再次输入新密码', trigger: 'blur'},
    {
      validator: (_r, v, cb) => v === pwdForm.newPwd ? cb() : cb(new Error('两次密码不一致')),
      trigger: 'blur'
    },
  ],
})
const openPwdDialog = () => {
  pwdForm.oldPwd = ''
  pwdForm.newPwd = ''
  pwdForm.confirmPwd = ''
  pwdFormRef.value?.clearValidate()
  pwdDialog.value = true
}
const submitPwd = async () => {
  try { await pwdFormRef.value?.validate() } catch { return }
  reqUpdatePwd({oldPwd: pwdForm.oldPwd, newPwd: pwdForm.newPwd}).then(res => {
    if (res.code === 1) {
      pwdDialog.value = false
      ElMessage.success('修改成功，下次请用新密码登录')
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  })
}

/*============ 绑定邮箱 ============*/
const emailDialog = ref(false)
const emailForm = reactive({email: '', code: ''})
const emailFormRef = ref<FormInstance>()
const emailRules = reactive<FormRules>({
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '邮箱格式不正确', trigger: 'blur'},
  ],
  code: [{required: true, message: '请输入验证码', trigger: 'blur'}],
})
const mailBtn = reactive({btnTxt: '发送验证码', dis: false})
const openEmailDialog = () => {
  emailForm.email = client.value.clientEmail || ''
  emailForm.code = ''
  emailFormRef.value?.clearValidate()
  emailDialog.value = true
}
const sendMailCode = () => {
  if (!emailForm.email) { ElMessage.warning('请先填写邮箱'); return }
  mailBtn.dis = true
  reqSendRegMail(emailForm.email).then(res => {
    if (res.code === 1) {
      ElMessage.success('验证码已发送，请查收邮件')
      let cd = 30
      mailBtn.btnTxt = cd + 's'
      const timer = setInterval(() => {
        cd--
        if (cd <= 0) {
          clearInterval(timer)
          mailBtn.dis = false
          mailBtn.btnTxt = '发送验证码'
        } else {
          mailBtn.btnTxt = cd + 's'
        }
      }, 1000)
    } else {
      mailBtn.dis = false
      ElMessage.error(res.msg || '发送失败')
    }
  })
}
const submitEmail = async () => {
  try { await emailFormRef.value?.validate() } catch { return }
  reqUpdateEmail({email: emailForm.email, code: emailForm.code}).then(res => {
    if (res.code === 1) {
      emailDialog.value = false
      ElMessage.success('邮箱绑定成功')
      reqLoginUser().then(r => { client.value = r })
    } else {
      ElMessage.error(res.msg || '绑定失败')
    }
  })
}
</script>

<style lang="scss" scoped>
.el-form-item {
  width: 50%;
}
</style>
