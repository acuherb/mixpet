<template>
  <div class="login-card">
    <h3>{{ getRoleName }}登录</h3>
    <div class="my-input">
      <label>
        <span class="icon">
          <img alt="" src="/icon/username.svg">
        </span>
        <input
            v-model="formLogin.username"
            autocomplete="off"
            name="username"
            placeholder="请输入账号"
            type="text"
            @keyup.enter="toLogin"
        >
      </label>
    </div>
    <div class="my-input">
      <label>
        <span class="icon">
          <img alt="" src="/icon/password.svg">
        </span>
        <input
            v-model="formLogin.password"
            autocomplete="new-password"
            name="password"
            placeholder="请输入密码"
            type="password"
            @keyup.enter="toLogin"
        >
      </label>
    </div>

    <div class="remember">
      <label class="check-label">
        <input v-model="formLogin.remember" class="check-input" type="checkbox">
        <span class="check-box"></span>
        <span class="check-text">记住我</span>
      </label>
    </div>

    <MyBtn class="login-btn" @click="toLogin">登录</MyBtn>
  </div>
</template>

<script lang="ts" setup>
import {computed, reactive} from "vue";
import MyBtn from "@/components/button/MyBtn.vue";
import {reqLogin} from "@/request/PowerApi";
import {LoginVo, RoleEnum} from "@/model/VO/LoginVo";

const prop = defineProps<{
  role: RoleEnum
}>()

const emit = defineEmits<{
  (e: "loginSuc"): void
}>()

const getRoleName = computed(() => {
  switch (prop.role) {
    case RoleEnum.DOCTOR:
      return "医生";
    default:
      return "用户";
  }
})

const formLogin: LoginVo = reactive({
  username: "",
  password: "",
  remember: true,
  role: RoleEnum.CLIENT
})

const toLogin = () => {
  formLogin.role = prop.role
  reqLogin(formLogin).then(resp => {
    console.log(resp.msg)
    if (resp.code == 1) {
      emit("loginSuc")
    }
  })
}
</script>

<style lang="scss" scoped>
/* 登录卡片 */
.login-card {
  width: 100%;
  text-align: center;

  h3 {
    font-size: 22px;
    color: #1F2D3D;
    margin: 0 0 28px 0;
    font-weight: 600;
    letter-spacing: 1px;
  }
}

/* 输入框 */
.my-input {
  width: 100%;
  margin-bottom: 16px;

  label {
    display: flex;
    align-items: center;
    width: 100%;
    height: 48px;
    border: 1.5px solid #E4E7ED;
    border-radius: 10px;
    background-color: #FAFAFA;
    transition: border-color 0.2s, box-shadow 0.2s, background-color 0.2s;
    box-sizing: border-box;
    overflow: hidden;

    &:focus-within {
      border-color: #1E6FA8;
      background-color: #fff;
      box-shadow: 0 0 0 3px rgba(30, 111, 168, 0.12);
    }
  }

  .icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 100%;
    flex-shrink: 0;

    img {
      width: 18px;
      height: 18px;
      opacity: 0.55;
    }
  }

  input {
    flex: 1;
    height: 100%;
    padding: 0 16px 0 0;
    border: none;
    outline: none;
    background: transparent;
    font-size: 14px;
    color: #1F2D3D;
    box-sizing: border-box;

    &::placeholder {
      color: #B0B3B8;
      font-size: 14px;
    }

    /* 禁用浏览器自动填充的黄底 */
    &:-webkit-autofill,
    &:-webkit-autofill:hover,
    &:-webkit-autofill:focus {
      -webkit-box-shadow: 0 0 0 1000px #FAFAFA inset !important;
      -webkit-text-fill-color: #1F2D3D !important;
      transition: background-color 9999s ease-out 0s;
    }
  }
}

/* 记住我 */
.remember {
  display: flex;
  justify-content: flex-start;
  margin: 4px 0 20px;

  .check-label {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    -webkit-user-select: none;
    user-select: none;
  }

  .check-input {
    display: none;
  }

  /* 复选框方块 */
  .check-box {
    position: relative;
    width: 18px;
    height: 18px;
    flex-shrink: 0;
    border: 1.5px solid #C0C4CC;
    border-radius: 4px;
    background-color: #fff;
    transition: all 0.2s;
    box-sizing: border-box;

    /* 对勾 */
    &::after {
      content: '';
      position: absolute;
      left: 5px;
      top: 1px;
      width: 5px;
      height: 10px;
      border: solid #fff;
      border-width: 0 2px 2px 0;
      transform: rotate(45deg) scale(0);
      transition: transform 0.15s;
    }
  }

  .check-input:checked + .check-box {
    background-color: #1E6FA8;
    border-color: #1E6FA8;

    &::after {
      transform: rotate(45deg) scale(1);
    }
  }

  .check-label:hover .check-box {
    border-color: #1E6FA8;
  }

  .check-text {
    font-size: 14px;
    color: #606266;
    white-space: nowrap;
    line-height: 1;
  }
}

.login-btn {
  width: 100%;
}
</style>
