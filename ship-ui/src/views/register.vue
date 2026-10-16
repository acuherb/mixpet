<template>
  <div class="register-page">
    <!-- 背景装饰 -->
    <div class="reg-bg" aria-hidden="true">
      <span class="blob b1"></span>
      <span class="blob b2"></span>
      <span class="blob b3"></span>
    </div>

    <div class="reg-card">
      <div class="reg-head">
        <h2>注册账号</h2>
        <p>带 <em>*</em> 为必填项；选填项注册后可在「个人资料」中补充修改</p>
      </div>

      <el-form ref="formRef" :model="regVo" :rules="rules" label-width="76px" class="reg-form">
        <el-form-item label="手机号" prop="tel">
          <el-input v-model="regVo.tel" clearable maxlength="11" placeholder="手机号即登录账号" @input="sanitizeTel"/>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="regVo.password" show-password placeholder="至少 5 位"/>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="regVo.name" clearable placeholder="真实姓名"/>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="regVo.sex">
            <el-radio :label="true">男</el-radio>
            <el-radio :label="false">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="邮箱" prop="mail">
          <el-input v-model="regVo.mail" clearable placeholder="email@qq.com"/>
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="regVo.code" clearable maxlength="6" placeholder="邮箱验证码"/>
            <el-button :disabled="mailBtn.dis" @click="sendMail">{{ mailBtn.btnTxt }}</el-button>
          </div>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="regVo.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选填"
                          style="width:100%"/>
        </el-form-item>
        <el-form-item label="备用电话">
          <el-input v-model="regVo.tel2" clearable maxlength="11" placeholder="选填" @input="sanitizeTel2"/>
        </el-form-item>
        <el-form-item label="地址" class="full">
          <el-input v-model="regVo.address" clearable placeholder="选填"/>
        </el-form-item>
        <el-form-item label="身份证" class="full">
          <el-input v-model="regVo.idCard" clearable maxlength="18" placeholder="选填，18 位"/>
        </el-form-item>
      </el-form>

      <div class="reg-actions">
        <el-button :loading="submitting" size="large" type="primary" @click="toReg">提交注册</el-button>
        <div class="reg-foot">已有账号？<router-link to="/login">直接登录</router-link></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref} from "vue";
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {useRouter} from "vue-router";
import {reqRegister, reqSendRegMail} from "@/request/PowerApi";
import {RegisterVo} from "@/model/VO/RegisterVo";
import {noMail} from "@/utils/MailUtil";

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const regVo = reactive<RegisterVo>({
  username: "",
  password: "",
  name: "",
  sex: true,
  mail: "",
  code: "",
  birthday: "",
  tel: "",
  tel2: "",
  address: "",
  idCard: ""
})

const sanitizeTel = (): void => { regVo.tel = regVo.tel.replace(/\D/g, '').slice(0, 11) }
const sanitizeTel2 = (): void => { regVo.tel2 = (regVo.tel2 || '').replace(/\D/g, '').slice(0, 11) }

const rules: FormRules = {
  tel: [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 5, message: '密码至少 5 位', trigger: 'blur'}
  ],
  name: [{required: true, message: '请输入姓名', trigger: 'blur'}],
  mail: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '邮箱格式不正确', trigger: 'blur'}
  ],
  code: [{required: true, message: '请输入验证码', trigger: 'blur'}]
}

const mailBtn = reactive({btnTxt: "发送验证码", dis: false})
const sendMail = (): void => {
  if (noMail(regVo.mail)) return;
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
  reqSendRegMail(regVo.mail)
}

const toReg = async (): Promise<void> => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  regVo.username = regVo.tel  // 手机号即账号
  reqRegister(regVo).then(res => {
    submitting.value = false
    if (res.code === 1) {
      // 自助注册需管理员审核通过，不再自动登录
      ElMessage.success(res.msg || "注册成功，请等待管理员审核通过后登录")
      setTimeout(() => router.push("/login"), 1200)
    } else {
      ElMessage.error(res.msg || "注册失败")
    }
  }).catch(() => { submitting.value = false })
}
</script>

<style lang="scss" scoped>
.register-page {
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
.reg-bg {
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

/* 注册卡片 */
.reg-card {
  position: relative;
  width: 760px;
  max-width: 96vw;
  max-height: 92vh;
  overflow-y: auto;
  padding: 32px 44px 28px;
  box-sizing: border-box;
  background: rgba(255, 255, 255, .97);
  border-radius: 20px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, .25);
}

.reg-head {
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

    em {
      color: #F56C6C;
      font-style: normal;
      padding: 0 2px;
    }
  }
}

/* 表单：响应式两列 */
.reg-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 22px;
  max-width: 640px;
  margin: 0 auto;

  .full { grid-column: 1 / -1; }
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
.reg-actions {
  max-width: 640px;
  margin: 6px auto 0;

  .el-button {
    width: 100%;
    height: 44px;
    font-size: 16px;
    border-radius: 10px;
  }
}

.reg-foot {
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

/* 手机端：单列 */
@media (max-width: 600px) {
  .reg-card { padding: 24px 20px 20px; }
  .reg-form { grid-template-columns: 1fr; }
}
</style>
