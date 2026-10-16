<template>
  <div class="find-page">
    <!-- 背景装饰 -->
    <div class="find-bg" aria-hidden="true">
      <span class="blob b1"></span>
      <span class="blob b2"></span>
      <span class="blob b3"></span>
    </div>

    <div class="find-card">
      <div class="find-head">
        <h2>找回密码</h2>
        <p>通过注册邮箱验证后重置密码</p>
      </div>

      <el-form ref="formRef" :model="findPwd" :rules="rules" label-width="84px" class="find-form">
        <el-form-item label="账号" prop="username">
          <el-input v-model="findPwd.username" clearable placeholder="注册时的手机号(账号)"/>
        </el-form-item>
        <el-form-item label="邮箱" prop="mail">
          <el-input v-model="findPwd.mail" clearable placeholder="注册时绑定的邮箱"/>
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="findPwd.code" clearable maxlength="6" placeholder="邮箱验证码"/>
            <el-button :disabled="mailBtn.dis" @click="sendMail">{{ mailBtn.btnTxt }}</el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="findPwd.password" show-password placeholder="至少 6 位"/>
        </el-form-item>
      </el-form>

      <div class="find-actions">
        <el-button :loading="submitting" size="large" type="primary" @click="toFind">重置密码</el-button>
        <div class="find-foot">想起密码了？<router-link to="/login">返回登录</router-link></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref} from "vue";
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {useRouter} from "vue-router";
import {reqFindPwd, reqSendFindMail} from "@/request/PowerApi";
import {RegisterVo} from "@/model/VO/RegisterVo";
import {noMail} from "@/utils/MailUtil";

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

// 找回密码只需 账号/邮箱/验证码/新密码，其余字段占位（后端 find 不使用）
const findPwd = reactive<RegisterVo>({
  username: "",
  password: "",
  name: "找回密码",
  sex: false,
  mail: "",
  code: "",
  birthday: "",
  tel: "",
  tel2: "",
  address: "",
  idCard: ""
})

const rules: FormRules = {
  username: [{required: true, message: '请输入账号', trigger: 'blur'}],
  mail: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '邮箱格式不正确', trigger: 'blur'}
  ],
  code: [{required: true, message: '请输入验证码', trigger: 'blur'}],
  password: [
    {required: true, message: '请输入新密码', trigger: 'blur'},
    {min: 6, message: '新密码至少 6 位', trigger: 'blur'}
  ]
}

const mailBtn = reactive({btnTxt: "发送验证码", dis: false})
const sendMail = (): void => {
  if (noMail(findPwd.mail)) return;
  mailBtn.dis = true;
  let countDown = 30;
  let intVal = setInterval(() => {
    if (countDown === 0) {
      clearInterval(intVal)
      mailBtn.dis = false;
      mailBtn.btnTxt = "发送验证码";
      return
    }
    mailBtn.btnTxt = "等待中(" + countDown + ")";
    countDown--;
  }, 1000)
  reqSendFindMail(findPwd.mail, findPwd.username)
}

const toFind = async (): Promise<void> => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  reqFindPwd(findPwd).then(res => {
    submitting.value = false
    if (res.code === 1) {
      ElMessage.success("密码重置成功，请返回登录")
      setTimeout(() => router.push("/login"), 1000)
    } else {
      ElMessage.error(res.msg || "重置失败")
    }
  }).catch(() => { submitting.value = false })
}
</script>

<style lang="scss" scoped>
.find-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #1E6FA8 0%, #4aa3df 45%, #6fc9b0 100%);
  overflow: hidden;
}

/* 背景光斑 */
.find-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .blob {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, .14);
  }

  .b1 { width: 460px; height: 460px; top: -140px; left: -120px; }
  .b2 { width: 380px; height: 380px; bottom: -140px; right: -90px; background: rgba(255, 255, 255, .10); }
  .b3 { width: 240px; height: 240px; top: 38%; right: 16%; background: rgba(255, 255, 255, .08); }
}

/* 找回密码卡片 */
.find-card {
  position: relative;
  width: 480px;
  max-width: 96vw;
  padding: 32px 40px 28px;
  box-sizing: border-box;
  background: rgba(255, 255, 255, .97);
  border-radius: 20px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, .25);
}

.find-head {
  text-align: center;
  margin-bottom: 22px;

  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    color: #1F2D3D;
    letter-spacing: 1px;
  }

  p {
    margin: 8px 0 0;
    font-size: 13px;
    color: #909399;
  }
}

/* 表单：单列居中 */
.find-form {
  max-width: 400px;
  margin: 0 auto;
}

/* 验证码 + 按钮 同一行 */
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;

  .el-input { flex: 1; }
  .el-button { flex-shrink: 0; }
}

/* 提交区 */
.find-actions {
  max-width: 400px;
  margin: 6px auto 0;

  .el-button {
    width: 100%;
    height: 44px;
    font-size: 16px;
    border-radius: 10px;
  }
}

.find-foot {
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
  color: #606266;

  a {
    color: #1E6FA8;
    text-decoration: none;

    &:hover { text-decoration: underline; }
  }
}
</style>
