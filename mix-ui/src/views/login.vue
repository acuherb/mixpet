<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-bg" aria-hidden="true">
      <span class="blob b1"></span>
      <span class="blob b2"></span>
      <span class="blob b3"></span>
    </div>

    <div class="login-card">
      <div class="login-head">
        <h2>欢迎登录</h2>
        <p>mixpet 宠物医疗服务平台</p>
      </div>

      <!-- 角色切换 -->
      <div class="role-switch">
        <el-button :type="role === RoleEnum.CLIENT ? 'primary' : 'default'" round
                   @click="role = RoleEnum.CLIENT">用户登录
        </el-button>
        <el-button :type="role === RoleEnum.DOCTOR ? 'primary' : 'default'" round
                   @click="role = RoleEnum.DOCTOR">医生登录
        </el-button>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @keyup.enter="toLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" clearable placeholder="请输入账号" size="large">
            <template #prefix><el-icon><User/></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" placeholder="请输入密码" size="large" show-password type="password">
            <template #prefix><el-icon><Lock/></el-icon></template>
          </el-input>
        </el-form-item>

        <div class="login-row">
          <label class="remember">
            <input v-model="form.remember" type="checkbox"/> 记住我
          </label>
          <router-link class="link" to="/findPwd">忘记密码？</router-link>
        </div>

        <el-button :loading="loading" class="login-btn" size="large" type="primary" @click="toLogin">登录</el-button>
      </el-form>

      <div class="login-foot">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
        <span class="sep">·</span>
        <router-link to="/">游客浏览</router-link>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {ElMessage, FormInstance, FormRules} from "element-plus";
import {Lock, User} from "@element-plus/icons-vue";
import {reqGetLogin, reqLogin, reqLoginUser} from "@/request/PowerApi";
import {LoginVo, RoleEnum} from "@/model/VO/LoginVo";

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
// 登录角色：默认用户，可切换医生
const role = ref<RoleEnum>(RoleEnum.CLIENT)
const form = reactive({username: "", password: "", remember: true})

const rules: FormRules = {
  username: [{required: true, message: '请输入账号', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}]
}

// 登录
const toLogin = async (): Promise<void> => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  const vo: LoginVo = {username: form.username, password: form.password, remember: form.remember, role: role.value}
  reqLogin(vo).then(resp => {
    loading.value = false
    if (resp.code === 1) getLoginRole()
    else ElMessage.error(resp.msg || "登录失败")
  }).catch(() => { loading.value = false })
}

// 登录成功后按角色跳转（失败时停留登录页并提示，不盲目进后台）：
// 顾客需首次改密（pwdChanged=false 先去 changePwd），医生/管理员进后台
const getLoginRole = async (): Promise<void> => {
  try {
    const resp = await reqGetLogin()
    if (resp === RoleEnum.CLIENT) {
      try {
        const user = await reqLoginUser()
        if (user && user.clientPwdChanged === false) router.push("/changePwd")
        else router.push("/")
      } catch {
        loading.value = false
        ElMessage.error('获取用户信息失败，请重试')
      }
    } else {
      router.push("/doctorPage/d-console")
    }
  } catch {
    loading.value = false
    ElMessage.error('获取登录信息失败，请重试')
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  background: linear-gradient(135deg, var(--brand) 0%, #4aa3df 45%, #6fc9b0 100%);
  overflow: hidden;
}

/* 背景光斑 */
.login-bg {
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

/* 登录卡片 */
.login-card {
  position: relative;
  width: 440px;
  max-width: 96vw;
  padding: 32px 40px 28px;
  box-sizing: border-box;
  background: rgba(255, 255, 255, .97);
  border-radius: 20px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, .25);
}

.login-head {
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

/* 角色切换 */
.role-switch {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 22px;
}

.login-form {
  max-width: 360px;
  margin: 0 auto;
}

.login-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 16px;

  .remember {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #606266;
    cursor: pointer;

    input { width: 15px; height: 15px; accent-color: var(--brand); cursor: pointer; }
  }

  .link {
    font-size: 13px;
    color: var(--brand);
    text-decoration: none;

    &:hover { text-decoration: underline; }
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 10px;
}

.login-foot {
  text-align: center;
  margin-top: 16px;
  font-size: 13px;
  color: #606266;

  a {
    color: var(--brand);
    text-decoration: none;

    &:hover { text-decoration: underline; }
  }

  .sep {
    margin: 0 8px;
    color: #C0C4CC;
  }
}
</style>
