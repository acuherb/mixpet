<template>
  <div class="login-page" :style="{ background: `linear-gradient(135deg, ${backColor.begin} 0%, ${backColor.end} 100%)` }">
    <!--登陆卡片-->
    <h2 class="my-h2">轻触牌间喵，进入影中境</h2>
    <router-link class="back-home" to="/">
      <el-icon><HomeFilled/></el-icon>
      返回首页
    </router-link>
    <!-- 用户登录 -->
    <div :style="{ transform: pairClient }" class="mixJade">
      <img alt="管理员入口" height="48" src="/mia.svg" width="64" @click="clientToAdmin">
      <LoginForm :role="RoleEnum.CLIENT" @loginSuc="getLoginRole"/>
      <div>
        <el-button round type="success" @click="$router.push('/')">游客浏览</el-button>
        <el-button round type="warning" @click="$router.push('/register')">账号注册</el-button>
        <el-button round type="danger" @click="$router.push('/findPwd')">找回密码</el-button>
      </div>
    </div>
    <!-- 医生登录 -->
    <div :style="{ transform: pairAdmin }" class="mixJade">
      <img alt="普通入口" height="48" src="/mia.svg" width="64" @click="adminToClient">
      <LoginForm :role="RoleEnum.DOCTOR" @loginSuc="getLoginRole"/>
      <div style="color: #6c757d">智慧唯在沉思中得享</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import LoginForm from "@/components/zoneView/LoginForm.vue";
import {reactive, ref} from "vue";
import {reqGetLogin, reqLoginUser} from "@/request/PowerApi";
import {useRouter} from "vue-router";
import {RoleEnum} from "@/model/VO/LoginVo";
import {HomeFilled} from "@element-plus/icons-vue";

// 登录成功后按角色跳转。
// 注意：不要在此处 onMounted 自动跳转——已登录用户点"前往登录"或退出登录后落到本页时，
// 会被立刻弹回工作台，导致登录页无法停留（表现为"登录页出问题/登不出"）。
const router = useRouter()
const getLoginRole = () => {
  reqGetLogin().then(resp => {
    if (resp === RoleEnum.CLIENT) {
      reqLoginUser().then(user => {
        if (user && user.clientPwdChanged === false) router.push("/changePwd")
        else router.push("/")
      })
    } else {
      // 医生 / 管理员（以及角色接口异常时）统一进入后台工作台，避免卡在登录页
      router.push("/backstage")
    }
  }).catch(() => {
    // 角色获取失败也兜底进入后台，避免登录页无法离开
    router.push("/backstage")
  })
}

const pairClient = ref<string>("rotateY(0deg)")
const pairAdmin = ref<string>("rotateY(180deg)")
const backColor = reactive({
  begin: "MediumVioletRed",
  end: "MediumOrchid"
})

const clientToAdmin = () => {
  pairClient.value = "rotateY(180deg)"
  pairAdmin.value = "rotateY(0deg)";
  backColor.begin = "ForestGreen"
  backColor.end = "DeepSkyBlue"
}

const adminToClient = () => {
  pairAdmin.value = "rotateY(-180deg)";
  pairClient.value = "rotateY(0deg)"
  backColor.begin = "MediumVioletRed"
  backColor.end = "MediumOrchid"
}
</script>

<style lang="scss" scoped>
/*整个登录组件*/
.login-page {
  width: 100vw;
  height: 100vh;
  overflow: hidden;

  /* 欢迎文字 */
  .back-home {
    position: fixed;
    top: 18px;
    left: 20px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 7px 14px;
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.22);
    backdrop-filter: blur(4px);
    color: #fff;
    font-size: 13px;
    text-decoration: none;
    transition: background 0.2s;
    z-index: 10;
  }
  .back-home:hover { background: rgba(255, 255, 255, 0.35); }

  .my-h2 {
    position: fixed;
    top: 12vh;
    color: #FAFAFA;
    font-size: 28px;
    letter-spacing: 4px;
    text-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
    left: 50%;
    transform: translateX(-50%);
    z-index: 20;
  }
}

/* 翻转的卡片 */
.mixJade {
  position: absolute;
  top: 50%;
  width: 420px;
  left: 50%;
  margin-left: -210px;
  margin-top: -220px;
  min-height: 440px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25), 0 8px 20px rgba(0, 0, 0, 0.15);
  background-color: #FAFAFA;
  border-radius: 20px;
  backface-visibility: hidden;
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  -webkit-user-select: none;
  user-select: none;
  padding: 40px 36px 32px;
  box-sizing: border-box;
  z-index: 20;

  /* 底部两个辅助按钮的容器 */
  > div:last-child {
    margin-top: 20px;
    display: flex;
    justify-content: center;
    gap: 8px;
    flex-wrap: wrap;
  }
}

/* 小屏适配 */
@media (max-width: 480px) {
  .mixJade {
    width: calc(100vw - 40px);
    left: 20px;
    margin-left: 0;
    margin-top: -220px;
    padding: 32px 24px 24px;
  }

  .my-h2 {
    font-size: 20px !important;
    letter-spacing: 2px !important;
    width: 90%;
    text-align: center;
  }
}
</style>