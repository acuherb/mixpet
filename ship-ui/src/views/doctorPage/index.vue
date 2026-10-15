<template>
  <div class="doctor-home">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand">
          <span class="brand-mark">DR</span>
          <span class="brand-name">pet-hms 医生工作台</span>
        </div>
        <ul class="nav">
          <li><router-link to="/backstage">
            <el-icon><User/></el-icon>
            个人中心
          </router-link></li>
          <li><a class="nav-logout" title="退出登录" @click="doLogout">
            <el-icon><SwitchButton/></el-icon>
            退出登录
          </a></li>
        </ul>
      </div>
    </header>

    <main class="page">
      <div class="page-head">
        <div class="ph-left">
          <h1 class="page-title">工作台</h1>
          <p class="page-desc">
            <span class="clock-dot"></span>
            当前时间 <span class="clock-num">{{ timeText }}</span>
            <span class="hello">，{{ doctorName }}，欢迎回来</span>
          </p>
        </div>
        <div class="ph-right">
          <div class="stat-badge">
            <span class="stat-num">{{ pendingCount }}</span>
            <span class="stat-lbl">待办挂号</span>
          </div>
          <div class="stat-badge">
            <span class="stat-num">{{ doneCount }}</span>
            <span class="stat-lbl">已处理</span>
          </div>
        </div>
      </div>

      <!--核心功能区：处理挂号 + 在线咨询-->
      <div class="dash-grid">
        <!--处理挂号卡片-->
        <div class="panel appoint-panel">
          <div class="panel-head">
            <div class="ph-tit">
              <span class="ph-ico ico-appoint"><el-icon><Calendar/></el-icon></span>
              <div>
                <h3>处理挂号</h3>
                <p>查看待处理挂号并进行接诊</p>
              </div>
            </div>
            <router-link to="/doctorPage/d-appoint">
              <el-button type="primary" round>
                全部挂号<el-icon style="margin-left:4px"><ArrowRight/></el-icon>
              </el-button>
            </router-link>
          </div>

          <div class="panel-body">
            <template v-if="pendingAppoints.length">
              <div v-for="a in pendingAppoints.slice(0, 3)" :key="a.appointmentId" class="appoint-row">
                <div class="ar-ava">{{ a.petName?.charAt(0) || '宠' }}</div>
                <div class="ar-main">
                  <div class="ar-title">
                    <span class="ar-name">{{ a.petName }}</span>
                    <el-tag size="small" effect="plain">{{ a.clientName }}</el-tag>
                    <el-tag size="small" type="warning" effect="light">未处理</el-tag>
                  </div>
                  <div class="ar-info">{{ a.appointmentInfo }}</div>
                </div>
                <div class="ar-time">{{ moveT(a.appointmentDate).slice(0, 16) }}</div>
              </div>
              <p v-if="pendingCount > 3" class="more-tip">还有 {{ pendingCount - 3 }} 条待办挂号，点击「全部挂号」查看</p>
            </template>
            <div v-else class="empty-box">
              <el-icon :size="40"><CircleCheck/></el-icon>
              <p>太棒了，暂无待处理挂号</p>
            </div>
          </div>
        </div>

        <!--在线咨询卡片-->
        <div class="panel chat-panel">
          <div class="panel-head">
            <div class="ph-tit">
              <span class="ph-ico ico-chat"><el-icon><ChatDotRound/></el-icon></span>
              <div>
                <h3>在线咨询</h3>
                <p>回复顾客的在线咨询消息</p>
              </div>
            </div>
            <router-link to="/doctorPage/d-chat">
              <el-button type="success" round>
                进入咨询<el-icon style="margin-left:4px"><ArrowRight/></el-icon>
              </el-button>
            </router-link>
          </div>

          <div class="panel-body">
            <template v-if="nearClient">
              <div class="client-row">
                <div class="client-ava">
                  <el-avatar :size="56" round>{{ nearClient.roleName?.charAt(0) }}</el-avatar>
                  <span class="dot"></span>
                </div>
                <div class="client-main">
                  <div class="client-name">{{ nearClient.roleName }}</div>
                  <div class="client-status">最近在线的咨询客户</div>
                </div>
              </div>
            </template>
            <div v-else class="empty-box">
              <el-icon :size="40"><ChatDotRound/></el-icon>
              <p>暂无在线咨询客户</p>
            </div>
          </div>

          <div class="panel-foot">
            <router-link to="/doctorPage/d-chat">
              <el-button plain type="success" style="width:100%">查看消息并回复</el-button>
            </router-link>
          </div>
        </div>
      </div>

      <!--预留功能位-->
      <div class="future-grid">
        <div class="future-card">
          <router-link to="/doctorPage/d-lab" class="future-link">
            <span class="f-ico ico-lab"><el-icon><FirstAidKit/></el-icon></span>
            <div class="f-txt">
              <h4>化验管理</h4>
              <p>检验检查开单与查看</p>
            </div>
            <el-tag size="small" type="success" effect="plain">去开单</el-tag>
          </router-link>
        </div>
        <div class="future-card">
          <router-link to="/backstage/z-hospital" class="future-link">
            <span class="f-ico ico-ward"><el-icon><HomeFilled/></el-icon></span>
            <div class="f-txt">
              <h4>住院管理</h4>
              <p>住院床位照护安排</p>
            </div>
            <el-tag size="small" type="success" effect="plain">去办理</el-tag>
          </router-link>
        </div>
        <div class="future-card">
          <router-link to="/doctorPage/d-image" class="future-link">
            <span class="f-ico ico-other"><el-icon><Camera/></el-icon></span>
            <div class="f-txt">
              <h4>影像检查</h4>
              <p>X光/B超/CT开单</p>
            </div>
            <el-tag size="small" type="success" effect="plain">去开单</el-tag>
          </router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, ref} from "vue";
import {ArrowRight, Calendar, Camera, ChatDotRound, CircleCheck, FirstAidKit, HomeFilled, User} from "@element-plus/icons-vue";
import {AppointDto2} from "@/model/DO/AppointDto2";
import {reqDoctorAppoint, reqGetLoginD} from "@/request/DoctorApi";
import {reqDoctorGetNearClient} from "@/request/MsgApi";
import {reqLogout} from "@/request/PowerApi";
import {useRouter} from "vue-router";
import {ElMessage, ElMessageBox} from "element-plus";
import {SwitchButton} from "@element-plus/icons-vue";
import {NameDo} from "@/model/DO/NameDo";
import {moveT} from "@/utils/TimeUtil";

const timeText = ref<string>('--:--:--')
let timeTimer: ReturnType<typeof setInterval> | undefined

const pad = (n: number) => n.toString().padStart(2, '0')
const tick = () => {
  const t = new Date()
  timeText.value = `${pad(t.getHours())}:${pad(t.getMinutes())}:${pad(t.getSeconds())}`
}

// 医生姓名
const doctorName = ref('医生')
// 挂号预览
const appoints = ref<AppointDto2[]>([])
const pendingAppoints = computed<AppointDto2[]>(() => appoints.value.filter(a => !a.isSuc))
const pendingCount = computed(() => pendingAppoints.value.length)
const doneCount = computed(() => appoints.value.length - pendingAppoints.value.length)
// 在线咨询客户
const nearClient = ref<NameDo | null>(null)

onMounted(() => {
  tick()
  timeTimer = setInterval(tick, 1000)
  loadData()
})

const loadData = () => {
  reqDoctorAppoint().then(res => appoints.value = res)
  reqDoctorGetNearClient().then(res => nearClient.value = res)
  // 取医生姓名用于问候
  reqGetLoginD().then(d => {
    if (d?.doctorName) doctorName.value = d.doctorName
  })
}

// 退出登录
const router = useRouter()
const doLogout = (): void => {
  ElMessageBox.confirm('确定退出登录吗？', '退出登录', {
    confirmButtonText: '确定退出', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqLogout().then(res => {
      if (res.code === 204) router.push('/login')
    })
  }).catch(() => ElMessage.info('已取消'))
}

onBeforeUnmount(() => {
  if (timeTimer) clearInterval(timeTimer)
})
</script>

<style lang="scss" scoped>
.doctor-home {
  min-height: 100vh;
  background: #F5F7FA;
  user-select: none;
}

/* 顶部导航 */
.topbar {
  background: #fff;
  border-bottom: 1px solid #E4E7ED;
}

.topbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  color: #1E6FA8;
  font-size: 16px;

  .brand-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 6px;
    background: #1E6FA8;
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1px;
  }
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  list-style: none;
  margin: 0;
  padding: 0;

  li {
    list-style: none;

    a {
      display: inline-block;
      padding: 8px 14px;
      border-radius: 6px;
      font-size: 14px;
      color: #1F2D3D;
      transition: background .15s, color .15s;

      &:hover {
        background: #EAF3FA;
        color: #1E6FA8;
      }

      &.nav-logout {
        cursor: pointer;

        &:hover {
          background: #fef0f0;
          color: #f56c6c;
        }
      }
    }
  }
}

/* 主体 */
.page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  box-sizing: border-box;
}

.page-head {
  margin-bottom: 28px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;

  .page-title {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: #1F2D3D;
    letter-spacing: .3px;
  }

  .page-desc {
    margin: 8px 0 0;
    font-size: 13px;
    color: #909399;
    display: flex;
    align-items: center;
    gap: 8px;

    .clock-dot {
      display: inline-block;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #0F8A7E;
    }

    .clock-num {
      color: #606266;
      font-variant-numeric: tabular-nums;
      font-weight: 500;
    }

    .hello {
      color: #1E6FA8;
      font-weight: 600;
    }
  }

  .ph-right {
    display: flex;
    gap: 14px;

    .stat-badge {
      background: #fff;
      border: 1px solid #E4E7ED;
      border-radius: 12px;
      padding: 10px 22px;
      display: flex;
      flex-direction: column;
      align-items: center;
      min-width: 96px;
      box-shadow: 0 1px 2px rgba(31, 45, 61, 0.04);

      .stat-num {
        font-size: 26px;
        font-weight: 700;
        color: #1E6FA8;
        line-height: 1.1;
      }

      .stat-lbl {
        margin-top: 4px;
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

/* 核心功能区 */
.dash-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.panel {
  background: #fff;
  border: 1px solid #E4E7ED;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.05);
  overflow: hidden;
  transition: box-shadow .2s, transform .2s;

  &:hover {
    box-shadow: 0 8px 24px rgba(31, 45, 61, 0.10);
    transform: translateY(-2px);
  }
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid #F0F2F7;
  flex-wrap: wrap;
  gap: 12px;

  .ph-tit {
    display: flex;
    align-items: center;
    gap: 14px;

    h3 {
      margin: 0;
      font-size: 17px;
      font-weight: 650;
      color: #1F2D3D;
    }

    p {
      margin: 3px 0 0;
      font-size: 13px;
      color: #909399;
    }

    .ph-ico {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 46px;
      height: 46px;
      border-radius: 12px;
      font-size: 22px;
      color: #fff;

      &.ico-appoint {
        background: linear-gradient(135deg, #1E6FA8, #4aa3df);
      }

      &.ico-chat {
        background: linear-gradient(135deg, #0F8A7E, #4cbfb3);
      }
    }
  }
}

.panel-body {
  padding: 16px 22px 20px;
  min-height: 180px;
}

/* 挂号预览行 */
.appoint-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: 10px;
  background: #F7F9FC;
  margin-bottom: 10px;
  transition: background .15s;

  &:hover {
    background: #EAF3FA;
  }

  .ar-ava {
    flex-shrink: 0;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: linear-gradient(135deg, #F6AD55, #F2994A);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 16px;
  }

  .ar-main {
    flex: 1;
    min-width: 0;

    .ar-title {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .ar-name {
        font-weight: 650;
        color: #1F2D3D;
      }
    }

    .ar-info {
      margin-top: 4px;
      font-size: 12px;
      color: #909399;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .ar-time {
    flex-shrink: 0;
    font-size: 12px;
    color: #B0B6C4;
    white-space: nowrap;
  }
}

.more-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #B0B6C4;
  text-align: center;
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #C0C4CC;

  p {
    margin: 10px 0 0;
    font-size: 14px;
  }
}

/* 在线咨询客户 */
.client-row {
  display: flex;
  align-items: center;
  gap: 18px;
  background: linear-gradient(135deg, #F0FBF9, #EAF7FF);
  border-radius: 12px;
  padding: 20px;

  .client-ava {
    position: relative;

    .dot {
      position: absolute;
      right: 2px;
      bottom: 4px;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      border: 2px solid #fff;
      background: #67C23A;
    }
  }

  .client-name {
    font-size: 20px;
    font-weight: 700;
    color: #1F2D3D;
  }

  .client-status {
    margin-top: 4px;
    font-size: 13px;
    color: #909399;
  }
}

.panel-foot {
  padding: 0 22px 18px;
}

/* 预留功能位 */
.future-grid {
  margin-top: 28px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.future-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border: 1px dashed #D8DCE5;
  border-radius: 12px;
  padding: 18px 20px;
  opacity: .72;
  transition: opacity .2s;

  &:hover {
    opacity: 1;
  }

  .future-link {
    display: flex;
    align-items: center;
    gap: 16px;
    width: 100%;
    color: inherit;
    text-decoration: none;
  }

  .f-ico {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 10px;
    font-size: 22px;
    color: #fff;

    &.ico-lab {
      background: linear-gradient(135deg, #9063Cd, #b78ae2);
    }

    &.ico-ward {
      background: linear-gradient(135deg, #52b788, #74c69d);
    }

    &.ico-other {
      background: linear-gradient(135deg, #9399a5, #b6bcc9);
    }
  }

  .f-txt {
    flex: 1;

    h4 {
      margin: 0;
      font-size: 15px;
      color: #1F2D3D;
    }

    p {
      margin: 3px 0 0;
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>