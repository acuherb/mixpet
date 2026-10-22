<template>
  <div class="doctor-home">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand">
          <span class="brand-mark">
            <img v-if="hosp.logo" :src="'/api/common/download?name=' + hosp.logo" alt="logo"/>
            <template v-else>{{ hosp.name ? hosp.name.charAt(0) : 'DR' }}</template>
          </span>
          <span class="brand-name">{{ hosp.name ? hosp.name + ' · 医生工作台' : 'mixpet 医生工作台' }}</span>
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
          <div class="stat-badge" :class="{hot: todayPending.length > 0}">
            <span class="stat-num">{{ todayPending.length }}</span>
            <span class="stat-lbl">今日待接诊</span>
          </div>
          <div class="stat-badge" :class="{hot: todo.treating > 0}">
            <span class="stat-num">{{ todo.treating }}</span>
            <span class="stat-lbl">接诊中</span>
          </div>
          <div class="stat-badge" :class="{hot: todoTotal > 0}">
            <span class="stat-num">{{ todoTotal }}</span>
            <span class="stat-lbl">待办事项</span>
          </div>
        </div>
      </div>

      <!--今日待办：医生打开工作台第一眼看到今天要处理的事-->
      <div class="panel today-panel">
        <div class="panel-head">
          <div class="ph-tit">
            <span class="ph-ico ico-today"><el-icon><Sunny/></el-icon></span>
            <div>
              <h3>今日待办</h3>
              <p>今天要看诊的病人，与尚未完结的就诊</p>
            </div>
          </div>
          <router-link to="/doctorPage/d-appoint">
            <el-button type="primary" round>
              全部挂号<el-icon style="margin-left:4px"><ArrowRight/></el-icon>
            </el-button>
          </router-link>
        </div>

        <div class="panel-body">
          <el-alert v-if="loadError" title="数据加载失败，请检查网络" type="error" show-icon :closable="false" style="margin-bottom:12px">
            <el-button size="small" type="primary" @click="reloadAll">重新加载</el-button>
          </el-alert>
          <!-- 今日工作流：待接诊挂号 + 接诊中文案按时间混排成一条线 -->
          <TodayTimeline :appoints="pendingAppoints" :records="treatingRecords" :go-treat="goTreat"/>
        </div>
      </div>

      <!--核心功能区：在线咨询 + 诊疗待办-->
      <div class="dash-grid">
        <!--在线咨询卡片-->
        <div class="panel chat-panel">
          <div class="panel-head">
            <div class="ph-tit">
              <span class="ph-ico ico-chat"><el-icon><ChatDotRound/></el-icon></span>
              <div>
                <h3>在线咨询<span v-if="totalUnread" class="unread-badge">{{ totalUnread > 99 ? '99+' : totalUnread }}</span></h3>
                <p>回复顾客的在线咨询消息，新消息会自动提醒</p>
              </div>
            </div>
            <router-link to="/doctorPage/d-chat">
              <el-button type="success" round>
                进入咨询<el-icon style="margin-left:4px"><ArrowRight/></el-icon>
              </el-button>
            </router-link>
          </div>

          <div class="panel-body">
            <div v-if="sortedClients.length" class="client-list">
              <div v-for="g in sortedClients" :key="g.roleId" class="client-row" @click="openChat(g.roleId)">
                <div class="client-ava">
                  <el-avatar :size="44" round>{{ g.roleName?.charAt(0) }}</el-avatar>
                  <span class="dot"></span>
                </div>
                <div class="client-main">
                  <div class="client-name">{{ g.roleName }}</div>
                  <div class="client-status">点击进入会话</div>
                </div>
                <span v-if="g.unread" class="client-unread">
                  {{ g.unread > 99 ? '99+' : g.unread }}
                </span>
              </div>
            </div>
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

        <!--诊疗待办卡片：接诊后卡在中间环节的事项-->
        <div class="panel todo-panel">
          <div class="panel-head">
            <div class="ph-tit">
              <span class="ph-ico ico-todo"><el-icon><Bell/></el-icon></span>
              <div>
                <h3>诊疗待办</h3>
                <p>接诊后卡在中间环节的事项，点击去处理</p>
              </div>
            </div>
            <router-link to="/doctorPage/d-record">
              <el-button type="warning" round>病例中心</el-button>
            </router-link>
          </div>
          <div class="panel-body">
            <div class="todo-grid">
              <div class="todo-item" @click="$router.push('/doctorPage/z-examResult?tab=lab&status=0')">
                <span class="todo-num" :class="{warn: todo.labPending > 0}">{{ todo.labPending }}</span>
                <span class="todo-lbl">待出结果化验</span>
              </div>
              <div class="todo-item" @click="$router.push('/doctorPage/z-examResult?tab=image&status=0')">
                <span class="todo-num" :class="{warn: todo.imagePending > 0}">{{ todo.imagePending }}</span>
                <span class="todo-lbl">待出报告影像</span>
              </div>
              <div class="todo-item" @click="$router.push('/doctorPage/z-surgery')">
                <span class="todo-num" :class="{warn: todo.surgeryPending > 0}">{{ todo.surgeryPending }}</span>
                <span class="todo-lbl">待手术/待记录</span>
              </div>
              <div class="todo-item" @click="$router.push('/doctorPage/z-follow')">
                <span class="todo-num" :class="{warn: todo.followPending > 0}">{{ todo.followPending }}</span>
                <span class="todo-lbl">待随访</span>
              </div>
              <div class="todo-item" @click="$router.push('/doctorPage/d-record')">
                <span class="todo-num" :class="{warn: todo.treating > 0}">{{ todo.treating }}</span>
                <span class="todo-lbl">接诊中病历</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!--诊疗功能入口（带待办预览）-->
      <div class="future-grid">
        <div class="future-card has-todo">
          <router-link to="/doctorPage/d-lab" class="future-link">
            <span class="f-ico ico-lab"><el-icon><FirstAidKit/></el-icon></span>
            <div class="f-txt">
              <h4>化验管理</h4>
              <p>检验检查开单与查看</p>
            </div>
            <div class="f-preview">
              <span class="preview-num" :class="{warn: todo.labPending > 0}">{{ todo.labPending }}</span>
              <span class="preview-lbl">待出结果</span>
            </div>
            <el-tag size="small" type="success" effect="plain">去开单</el-tag>
          </router-link>
        </div>
        <div class="future-card has-todo">
          <router-link to="/doctorPage/z-examResult?tab=lab" class="future-link">
            <span class="f-ico ico-lab"><el-icon><Document/></el-icon></span>
            <div class="f-txt">
              <h4>化验结果录入</h4>
              <p>按检验单逐项填写结果</p>
            </div>
            <div class="f-preview">
              <span class="preview-num" :class="{warn: todo.labPending > 0}">{{ todo.labPending }}</span>
              <span class="preview-lbl">待录入</span>
            </div>
            <el-tag size="small" type="warning" effect="plain">去录入</el-tag>
          </router-link>
        </div>
        <div class="future-card has-todo">
          <router-link to="/doctorPage/z-hospital" class="future-link">
            <span class="f-ico ico-ward"><el-icon><HomeFilled/></el-icon></span>
            <div class="f-txt">
              <h4>住院管理</h4>
              <p>住院床位照护安排</p>
            </div>
            <div class="f-preview">
              <span class="preview-num" :class="{warn: stat.inHospital > 0}">{{ stat.inHospital }}</span>
              <span class="preview-lbl">在院</span>
            </div>
            <el-tag size="small" type="success" effect="plain">去办理</el-tag>
          </router-link>
        </div>
        <div class="future-card has-todo">
          <router-link to="/doctorPage/d-image" class="future-link">
            <span class="f-ico ico-other"><el-icon><Camera/></el-icon></span>
            <div class="f-txt">
              <h4>影像检查</h4>
              <p>X光/B超/CT开单</p>
            </div>
            <div class="f-preview">
              <span class="preview-num" :class="{warn: todo.imagePending > 0}">{{ todo.imagePending }}</span>
              <span class="preview-lbl">待出报告</span>
            </div>
            <el-tag size="small" type="success" effect="plain">去开单</el-tag>
          </router-link>
        </div>
        <div class="future-card has-todo">
          <router-link to="/doctorPage/z-examResult?tab=image" class="future-link">
            <span class="f-ico ico-other"><el-icon><Picture/></el-icon></span>
            <div class="f-txt">
              <h4>影像报告录入</h4>
              <p>按模板填写影像报告</p>
            </div>
            <div class="f-preview">
              <span class="preview-num" :class="{warn: todo.imagePending > 0}">{{ todo.imagePending }}</span>
              <span class="preview-lbl">待录入</span>
            </div>
            <el-tag size="small" type="warning" effect="plain">去录入</el-tag>
          </router-link>
        </div>
        <div class="future-card has-todo">
          <router-link to="/doctorPage/z-immune" class="future-link">
            <span class="f-ico ico-other"><el-icon><Bell/></el-icon></span>
            <div class="f-txt">
              <h4>免疫接种</h4>
              <p>疫苗接种与记录</p>
            </div>
            <div class="f-preview">
              <span class="preview-num" :class="{warn: stat.dueVaccine > 0}">{{ stat.dueVaccine }}</span>
              <span class="preview-lbl">近30天应接种</span>
            </div>
            <el-tag size="small" type="success" effect="plain">去查看</el-tag>
          </router-link>
        </div>
        <!-- 挂号列表：医生直达表格视图（登记明细/历史挂号，不用进后台） -->
        <div class="future-card">
          <router-link to="/doctorPage/y-appointmentList" class="future-link">
            <span class="f-ico ico-appoint"><el-icon><Calendar/></el-icon></span>
            <div class="f-txt">
              <h4>挂号列表</h4>
              <p>表格视图 · 登记明细/历史挂号检索</p>
            </div>
            <el-tag size="small" type="success" effect="plain">去查看</el-tag>
          </router-link>
        </div>
        <!-- 运营协同：跳转后台（收银/档案由前台或管理员处理，医生偶尔查询） -->
        <div class="future-card backstage-link">
          <router-link to="/backstage/y-billList" class="future-link">
            <span class="f-ico ico-cash"><el-icon><Money/></el-icon></span>
            <div class="f-txt">
              <h4>收银台 <el-tag size="small" type="info" effect="plain">后台</el-tag></h4>
              <p>跳转至后台 · 代收/查看账单</p>
            </div>
            <el-tag size="small" type="info" effect="plain">跳转</el-tag>
          </router-link>
        </div>
        <div class="future-card backstage-link">
          <router-link to="/backstage/y-clientList" class="future-link">
            <span class="f-ico ico-client"><el-icon><User/></el-icon></span>
            <div class="f-txt">
              <h4>客户管理 <el-tag size="small" type="info" effect="plain">后台</el-tag></h4>
              <p>跳转至后台 · 顾客/宠物档案</p>
            </div>
            <el-tag size="small" type="info" effect="plain">跳转</el-tag>
          </router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, ref} from "vue";
import {ArrowRight, Bell, Camera, ChatDotRound, Document, FirstAidKit, HomeFilled, Money, Picture, Sunny, User} from "@element-plus/icons-vue";
import {AppointDto2} from "@/model/DO/AppointDto2";
import {ChatGroup} from "@/model/DO/ChatGroup";
import {reqDoctorAppoint, reqGetLoginD} from "@/request/DoctorApi";
import {reqDoctorMsgGroup} from "@/request/MsgApi";
import {reqRecordPage, reqRecordTodo} from "@/request/RecordApi";
import {reqStatOverview} from "@/request/StatisticsApi";
import {hospital} from "@/utils/hospital";
import TodayTimeline from "@/components/doctor/TodayTimeline.vue";
import {RecordDto} from "@/model/DO/RecordDto";
import {RecordTodo} from "@/model/VO/RecordFull";
import {StatOverview} from "@/model/entiy/StatOverview";
import {reqLogout} from "@/request/PowerApi";
import {clearPwdCheckFlag} from "@/router";
import {useRouter} from "vue-router";
import {ElMessage, ElMessageBox, ElNotification} from "element-plus";
import {SwitchButton} from "@element-plus/icons-vue";
import {Msg} from "@/model/entiy/Msg";

const timeText = ref<string>('--:--:--')
// 医院信息（全局单例，顶栏品牌位展示）
const hosp = hospital
let timeTimer: ReturnType<typeof setInterval> | undefined
let dataTimer: ReturnType<typeof setInterval> | undefined

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
const router = useRouter()

// 今日待接诊：就诊日期为今天且未处理，按时间先后排序
const todayStr = (): string => {
  const t = new Date()
  return `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`
}
const isToday = (d: string | null): boolean => !!d && d.slice(0, 10) === todayStr()
// 直达接诊：带挂号单 id 进接诊页，页面自动打开该单的接诊向导
const goTreat = (appointmentId: number): void => {
  router.push('/doctorPage/d-appoint?appointmentId=' + appointmentId)
}
// 今日待接诊（时间线按时间混排，无需再截断队列）
const todayPending = computed<AppointDto2[]>(() =>
    pendingAppoints.value
        .filter(a => isToday(a.appointmentDate))
        .sort((x, y) => (x.appointmentDate || '').localeCompare(y.appointmentDate || ''))
)
// 接诊中的病历（未完结，真实列表）
const treatingRecords = ref<RecordDto[]>([])
const loadTreating = (): void => {
  reqRecordPage({
    status: 0, numPage: 1, pageSize: 20,
    petName: '', clientName: '', doctorName: '', diagnosis: '', startTime: '', endTime: ''
  }).then(res => { treatingRecords.value = res?.records || [] }).catch(() => { loadError.value = true })
}
// 待办合计（卡壳事项 + 接诊中）
const todoTotal = computed(() =>
    todo.value.labPending + todo.value.imagePending + todo.value.surgeryPending
    + todo.value.followPending + todo.value.treating
)

// 在线咨询客户列表（医生谈过话的所有客户，含各自的未读数）
const groupList = ref<ChatGroup[]>([])
// 诊疗待办：接诊后卡在中间环节的事项
const todo = ref<RecordTodo>({treating: 0, labPending: 0, imagePending: 0, surgeryPending: 0, followPending: 0})
// 统计概览：住院/免疫等全局待办
const stat = ref<StatOverview>({
  todayRevenue: 0, monthRevenue: 0, pendingBills: 0, inHospital: 0,
  pendingLab: 0, pendingImage: 0, dueVaccine: 0, todayAppoint: 0,
  todayRecord: 0, clientTotal: 0, pendingDispense: 0, lowStock: 0,
  expiringDrug: 0, revenueTrend: [], doctorTop: []
})
// 数据加载失败态（工作台关键数据，失败时提示重试）
const loadError = ref(false)
// 有未读的客户置顶
const sortedClients = computed<ChatGroup[]>(() =>
    [...groupList.value].sort((a, b) => (b.unread || 0) - (a.unread || 0))
)
// 未读总数（标题角标）
const totalUnread = computed(() =>
    groupList.value.reduce((s, g) => s + (g.unread || 0), 0)
)

const loadClients = (): void => {
  reqDoctorMsgGroup(0).then(res => groupList.value = res || [])
}

// WebSocket：医生工作台实时接收客户咨询消息并提醒
const wsUrl = (location.protocol === "https:" ? "wss://" : "ws://") + location.host + "/chat"
let ws: WebSocket | null = null

const clientNameOf = (cid: number): string =>
    groupList.value.find(g => g.roleId === cid)?.roleName || `客户${cid}`

const previewOf = (m: Msg): string => {
  if (m.isImg) return '［图片］'
  if (m.msgType === 'FILE') return `［文件］${m.msgName || ''}`
  return m.msgContent || ''
}

// 收到客户消息：刷新客户列表（含未读数）并弹出提醒
const handleChatMsg = (m: Msg): void => {
  const cid = m.clientId
  if (cid == null) return
  loadClients()
  ElNotification({
    title: '新的咨询消息',
    message: `${clientNameOf(cid)}：${previewOf(m)}`,
    type: 'success',
    duration: 10000,
    position: 'top-right',
    onClick: () => { ElNotification.closeAll(); router.push(`/doctorPage/d-chat?clientId=${cid}`) }
  })
}

// 点击某客户，进入与该客户的会话（进入后由后端标记已读，返回时刷新未读）
const openChat = (cid: number): void => {
  router.push(`/doctorPage/d-chat?clientId=${cid}`)
}

onMounted(() => {
  tick()
  timeTimer = setInterval(tick, 1000)
  loadData()
  loadTreating()
  loadClients()
  reqRecordTodo().then(res => { if (res) todo.value = res })
  reqStatOverview().then(res => { if (res) stat.value = res })
  // 每 60 秒刷新待办数据：检验/影像结果由护士录完后，医生的「待出结果」自动减少
  dataTimer = setInterval(() => {
    loadData()
    loadTreating()
    reqRecordTodo().then(res => { if (res) todo.value = res })
    reqStatOverview().then(res => { if (res) stat.value = res })
  }, 60000)
  ws = new WebSocket(wsUrl)
  ws.onmessage = (ev) => {
    try {
      handleChatMsg(JSON.parse(ev.data) as Msg)
    } catch { /* 忽略非消息帧 */ }
  }
})

const loadData = () => {
  reqDoctorAppoint().then(res => appoints.value = res).catch(() => { loadError.value = true })
  // 取医生姓名用于问候
  reqGetLoginD().then(d => {
    if (d?.doctorName) doctorName.value = d.doctorName
  })
}
// 一键重载（加载失败时）
const reloadAll = (): void => {
  loadError.value = false
  loadData()
  loadTreating()
  loadClients()
  reqRecordTodo().then(res => { if (res) todo.value = res })
}

// 退出登录
const doLogout = (): void => {
  ElMessageBox.confirm('确定退出登录吗？', '退出登录', {
    confirmButtonText: '确定退出', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqLogout().then(res => {
      clearPwdCheckFlag()
      if (res.code === 204) router.push('/login')
    })
  }).catch(() => ElMessage.info('已取消'))
}

onBeforeUnmount(() => {
  if (timeTimer) clearInterval(timeTimer)
  if (dataTimer) clearInterval(dataTimer)
  ws?.close(1000, '离开工作台')
  ElNotification.closeAll()
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
  color: var(--brand);
  font-size: 16px;

  .brand-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 6px;
    background: var(--brand);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1px;
    overflow: hidden;

    img { width: 100%; height: 100%; object-fit: cover; }
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
        color: var(--brand);
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
      color: var(--brand);
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
        color: var(--brand);
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
        background: linear-gradient(135deg, var(--brand), #4aa3df);
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

/* 在线咨询客户列表 */
.client-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 264px;
  overflow-y: auto;
}

.client-row {
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(135deg, #F0FBF9, #EAF7FF);
  border-radius: 12px;
  padding: 12px 14px;
  cursor: pointer;
  transition: box-shadow .2s, transform .2s;

  &:hover {
    box-shadow: 0 4px 14px rgba(30, 111, 168, .16);
    transform: translateY(-1px);
  }

  .client-ava {
    position: relative;
    flex-shrink: 0;

    .dot {
      position: absolute;
      right: 0;
      bottom: 2px;
      width: 11px;
      height: 11px;
      border-radius: 50%;
      border: 2px solid #fff;
      background: #67C23A;
    }
  }

  .client-main {
    flex: 1;
    min-width: 0;
  }

  .client-name {
    font-size: 16px;
    font-weight: 700;
    color: #1F2D3D;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .client-status {
    margin-top: 2px;
    font-size: 12px;
    color: #909399;
  }

  .client-unread {
    flex-shrink: 0;
    min-width: 22px;
    height: 22px;
    padding: 0 6px;
    border-radius: 11px;
    background: #F56C6C;
    color: #fff;
    font-size: 12px;
    line-height: 22px;
    text-align: center;
    box-sizing: border-box;
  }
}

/* 今日待办面板（工作台最显眼位置） */
.today-panel {
  border: 1px solid #f0d9b5;
  background: linear-gradient(180deg, #fffdf8, #ffffff);

  .panel-body { padding-top: 4px; }
}

.todo-sec {
  margin-bottom: 18px;

  &:last-child { margin-bottom: 0; }
}

.todo-sec-tit {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #1F2D3D;

  .el-tag { margin-left: 2px; }
}

.todo-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.todo-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #fafbfc;
  border: 1px solid #edf0f3;
  border-radius: 10px;
  cursor: pointer;
  transition: box-shadow .2s, transform .15s, border-color .2s;

  &:hover {
    border-color: #c6e2ff;
    box-shadow: 0 4px 14px rgba(30, 111, 168, .12);
    transform: translateY(-1px);
  }

  .tr-time {
    flex-shrink: 0;
    font-size: 12px;
    color: #909399;
    font-variant-numeric: tabular-nums;
  }

  .tr-pet {
    flex-shrink: 0;
    min-width: 64px;
    font-size: 14px;
    font-weight: 600;
    color: #1F2D3D;
  }

  .tr-owner {
    flex-shrink: 0;
    font-size: 12px;
    color: #606266;
  }

  .tr-info {
    flex: 1;
    min-width: 0;
    font-size: 13px;
    color: #606266;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.todo-empty {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  padding: 14px;
  background: #f6faf6;
  border-radius: 8px;
  color: #67C23A;
  font-size: 13px;
}

/* 页头徽章：有待办时数字高亮 */
.stat-badge.hot .stat-num { color: #E6A23C; }

/* 诊疗待办面板 */
.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(96px, 1fr));
  gap: 10px;
}

.todo-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 6px;
  background: #f7f9fb;
  border: 1px solid #edf0f3;
  border-radius: 10px;
  cursor: pointer;
  transition: box-shadow .2s, transform .15s;

  &:hover {
    box-shadow: 0 4px 12px rgba(30, 111, 168, .12);
    transform: translateY(-1px);
  }

  .todo-num {
    font-size: 22px;
    font-weight: 700;
    color: #1F2D3D;

    &.warn { color: #E6A23C; }
  }

  .todo-lbl { font-size: 12px; color: #909399; }
}

/* 标题上的未读角标 */
.unread-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  margin-left: 6px;
  padding: 0 6px;
  border-radius: 10px;
  background: #F56C6C;
  color: #fff;
  font-size: 12px;
  vertical-align: middle;
  box-sizing: border-box;
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

  /* 后台跳转卡片：边框变实线，透明度提高 */
  &.backstage-link {
    border-style: solid;
    border-color: #C0C4CC;
    opacity: .65;

    &:hover {
      opacity: 1;
      border-color: var(--el-color-primary);
    }
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

    &.ico-cash {
      background: linear-gradient(135deg, #e6a23c, #f3c563);
    }

    &.ico-appoint {
      background: linear-gradient(135deg, #1E6FA8, #4ba3e0);
    }

    &.ico-client {
      background: linear-gradient(135deg, #409eff, #79bbff);
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

  /* 有待办预览的卡片 */
  &.has-todo {
    opacity: 1;
    border-style: dashed;

    &:hover {
      border-color: var(--el-color-primary);
    }
  }

  .f-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 52px;
    padding-left: 16px;
    border-left: 1px solid #E8E8E8;

    .preview-num {
      font-size: 22px;
      font-weight: 700;
      color: #303133;
      line-height: 1;

      &.warn {
        color: #E6A23C;
      }
    }

    .preview-lbl {
      font-size: 11px;
      color: #909399;
      margin-top: 4px;
    }
  }
}
</style>