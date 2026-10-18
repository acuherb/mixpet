<template>
  <div class="reception-home">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link :to="isLogin ? '/reception/clientCenter' : '/'" class="brand">
          <span class="brand-mark">
            <img v-if="hosp.logo" :src="'/api/common/download?name=' + hosp.logo" alt="logo"/>
            <template v-else>PET</template>
          </span>
          <span class="brand-name">{{ hosp.name || 'mixpet' }}</span>
        </router-link>
        <ul class="nav">
          <li v-if="isLogin" class="drop">
            <span class="user-trigger">
              <MyAvatar :name="user.clientPhoto"/>
              <span class="user-name">{{ user.clientName || '个人中心' }}</span>
              <span class="caret">▾</span>
            </span>
            <ul class="drop-list">
              <li>
                <router-link to="/reception/clientCenter">个人资料</router-link>
              </li>
              <li>
                <router-link to="/reception/clientCenter/clientPet">宠物信息</router-link>
              </li>
              <li>
                <router-link to="/reception/clientCenter/clientAppoint">挂号查看</router-link>
              </li>
              <li><a class="danger" @click="myLogout">退出登录</a></li>
            </ul>
          </li>
          <li v-else>
            <router-link class="nav-cta" to="/login">前往登录</router-link>
          </li>
        </ul>
      </div>
    </header>

    <WindRoll :is-login="isLogin"/>

    <main class="page" v-loading="pageLoading">
      <!-- 公告 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">网站公告</h2>
            <p class="section-desc">最新通知与动态</p>
          </div>
          <router-link class="section-action" to="/reception/noticeList">查看全部 →</router-link>
        </div>
        <div v-if="notices.length" class="notice-grid">
          <div
              v-for="notice in notices.slice(0, 6)"
              :key="notice.noticeId"
              class="notice-card"
              @click="openDialog(notice)"
          >
            <div class="notice-icon-wrap">
              <el-icon class="notice-icon" color="var(--brand)"><Bell/></el-icon>
            </div>
            <div class="notice-card-body">
              <div class="notice-title">{{ notice.noticeTitle }}</div>
              <div class="notice-time">{{ noticeTime(notice.createTime) }}</div>
              <div class="notice-arrow">查看详情 →</div>
            </div>
          </div>
        </div>
        <div v-else class="notice-empty">暂无公告</div>
      </section>

      <MyDialog :tit="noticeDialog.tit" :content="noticeDialog.content" ref="myShow"/>

      <!-- 用户入口 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">用户入口</h2>
            <p class="section-desc">常用功能快捷入口</p>
          </div>
          <router-link class="section-action" to="/reception/clientCenter">个人中心 →</router-link>
        </div>
        <MyRow>
          <LinkCard v-for="card in userCard"
                    :key="card.tit"
                    :body="card.body"
                    :router-path="card.routerPath"
                    :tit="card.tit"
                    :type="card.type"
                    :badge="card.routerPath === '/reception/chat' ? totalUnread : 0"
                    need-login
                    :guest="!isLogin"
                    @need-login="goLogin"
          />
        </MyRow>
      </section>

      <!-- 待领养 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">待领养宠物</h2>
            <p class="section-desc">它们等待一个温暖的家</p>
          </div>
          <router-link class="section-action" to="/reception/petSee">查看全部 →</router-link>
        </div>
        <PetCard :card-list="petCardTxt.records"/>
      </section>

      <!-- 医生 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">医生展示</h2>
            <p class="section-desc">执业医生团队</p>
          </div>
          <router-link class="section-action" to="/reception/doctorSee">查看全部 →</router-link>
        </div>
        <DoctorCard :card-list="doctorCardTxt.records"/>
      </section>

      <!-- 寄养 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">寄养宠物展示</h2>
            <p class="section-desc">在院寄养中的宠物</p>
          </div>
          <router-link class="section-action" to="/reception/fosterSee">查看全部 →</router-link>
        </div>
        <FosterCard :card-list="fosterCardTxt.records"/>
      </section>
    </main>

    <el-button v-show="showTop" class="back-top" circle type="primary" :icon="ArrowUp" title="回到顶部"
               @click="scrollTop"/>

    <VueFoot/>
  </div>
</template>

<script lang="ts" setup>
import {onBeforeMount, onBeforeUnmount, onMounted, reactive, ref} from "vue";
import {Pet} from "@/model/entiy/Pet";
import {Page} from "@/model/DO/Page";
import PetCard from "@/components/zoneView/PetCard.vue";
import DoctorCard from "@/components/zoneView/DoctorCard.vue";
import FosterCard from "@/components/card/FosterCard.vue";
import {DoctorDto} from "@/model/DO/DoctorDto";
import {FosterCardDto} from "@/model/DO/FosterCardDto";
import {NoticeDto} from "@/model/DO/NoticeDto";
import {reqLoginUser, reqLogout} from "@/request/PowerApi";
import {reqClientGetNearDoctor, reqClientMsgGroup} from "@/request/MsgApi";
import {ElMessage, ElMessageBox, ElNotification} from "element-plus";
import {Msg} from "@/model/entiy/Msg";
import {getDisplayTime} from "@/utils/TimeUtil";
import {hospital} from "@/utils/hospital";
import {reqFourDoctor} from "@/request/DoctorApi";
import {reqFourPet} from "@/request/PetApi";
import {reqFosterPet} from "@/request/FosterApi";
import {reqFourNotice} from "@/request/NoticeApi";
import {useRouter} from "vue-router";
import VueFoot from "@/components/show/VueFoot.vue";
import WindRoll from "@/components/show/WindRoll.vue";
import MyRow from "@/components/row/MyRow.vue";
import LinkCard from "@/components/card/LinkCard.vue";
import MyDialog from "@/components/show/MyDialog.vue";
import {ArrowUp, Bell} from "@element-plus/icons-vue";
import MyAvatar from "@/components/show/MyAvatar.vue";

/* 生命周期 */
const isLogin = ref<boolean>(false)
const pageLoading = ref<boolean>(true)
// 当前登录用户（顶栏展示头像与昵称）
const user = ref<{ clientName: string, clientPhoto: string }>({clientName: '', clientPhoto: ''})

onBeforeMount(() => {
  // 并行加载首页数据，完成后关闭 loading
  Promise.all([
    reqFourDoctor().then(res => { doctorCardTxt.records = res.records }),
    reqFourPet().then(res => { petCardTxt.records = res.records }),
    reqFosterPet(6).then(res => { fosterCardTxt.records = res.records }),
    reqFourNotice().then(res => { notices.value = res }),
  ]).catch(() => {}).finally(() => { pageLoading.value = false })

  reqLoginUser().then(res => {
    // 未登录时后端返回空，需做空值保护
    isLogin.value = !!(res && res.clientId != null)
    if (isLogin.value) user.value = {clientName: res.clientName || '', clientPhoto: res.clientPhoto || ''}
    if (res && res.clientId != null && res.clientPwdChanged === false) {
      router.push("/changePwd")
    }
    // 登录后才加载未读并连接医生回复提醒
    if (isLogin.value) {
      loadUnread()
      connectChat()
    }
  })
})

// 未登录点击需登录入口时，引导去登录
const goLogin = (): void => {
  ElMessage.warning('请先登录')
  setTimeout(() => router.push('/login'), 600)
}

// 回到顶部
const showTop = ref<boolean>(false)
const handleScroll = (): void => { showTop.value = (window.scrollY || 0) > 400 }
const scrollTop = (): void => { window.scrollTo({top: 0, behavior: 'smooth'}) }
onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

/* 医院信息（全局单例，App 已加载；供 Hero 与品牌位展示） */
const hosp = hospital

/* 公告栏 */
const notices = ref<NoticeDto[]>([])
const noticeTime = (t: string | null | undefined): string => (t ? getDisplayTime(t) : '')
const myShow = ref<InstanceType<typeof MyDialog> | null>(null)
const noticeDialog = reactive({tit: "标题", content: "文本内容"})
const openDialog = (notice: NoticeDto) => {
  if (!notice) return
  noticeDialog.tit = notice.noticeTitle
  noticeDialog.content = notice.textNotice
  myShow.value?.showMe()
}

/* 用户卡片 */
interface UserCard {
  tit: string;
  body: string;
  type: "primary" | "success" | "warning";
  routerPath: string;
}

const userCard: UserCard[] = [
  {
    "tit": "挂号信息",
    "body": "在此查看自己的历史挂号",
    "type": "primary",
    "routerPath": "/reception/clientCenter/clientAppoint"
  }, {
    "tit": "宠物信息",
    "body": "查看自己的名下宠物",
    "type": "warning",
    "routerPath": "/reception/clientCenter/clientPet"
  }, {
    "tit": "咨询医生",
    "body": "向医生发起咨询",
    "type": "success",
    "routerPath": "/reception/chat"
  },
]

/* 卡片数据 */
const petCardTxt: Page<Pet> = reactive({records: [], total: 4})
const doctorCardTxt: Page<DoctorDto> = reactive({records: [], total: 4})
const fosterCardTxt: Page<FosterCardDto> = reactive({records: [], total: 3})

/* 退出登录 */
const router = useRouter()
const myLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '退出登录', {
    confirmButtonText: '确定退出', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    reqLogout().then(() => {
      // 无论返回码，退出后重置前端登录态并回到未登录首页
      isLogin.value = false
      totalUnread.value = 0
      if (ws) { ws.close(1000, '退出登录'); ws = null }
      router.push("/")
    }).catch(() => ElMessage.error('退出失败，请重试'))
  }).catch(() => ElMessage.info('已取消'))
}

/* 在线咨询未读（医生发来、顾客未读）与医生回复实时提醒 */
const totalUnread = ref<number>(0)
const loadUnread = (): void => {
  reqClientGetNearDoctor().then(doc => {
    reqClientMsgGroup(doc.roleId).then(list => {
      totalUnread.value = (list || []).reduce((s, g) => s + (g.unread || 0), 0)
    })
  })
}
const previewOf = (m: Msg): string => {
  if (m.isImg) return '［图片］'
  if (m.msgType === 'FILE') return `［文件］${m.msgName || ''}`
  return m.msgContent || ''
}
const wsUrl = (location.protocol === "https:" ? "wss://" : "ws://") + location.host + "/chat"
let ws: WebSocket | null = null
const connectChat = (): void => {
  if (ws) return
  ws = new WebSocket(wsUrl)
  ws.onmessage = (ev) => {
    try {
      const m = JSON.parse(ev.data) as Msg
      loadUnread()
      ElNotification({
        title: '医生回复了您',
        message: previewOf(m),
        type: 'success',
        duration: 10000,
        position: 'top-right',
        onClick: () => { ElNotification.closeAll(); router.push('/reception/chat') }
      })
    } catch { /* 忽略非消息帧 */ }
  }
}
onBeforeUnmount(() => {
  if (ws) { ws.close(1000, '离开首页'); ws = null }
  ElNotification.closeAll()
})
</script>

<style lang="scss" scoped>
.reception-home {
  min-height: 100vh;
  background: #F5F7FA;
}

/* 顶部导航 */
.topbar {
  /* 菜单栏背景随主题色联动（品牌渐变） */
  background: linear-gradient(135deg, var(--brand), var(--brand-dark));
  border-bottom: none;
  position: sticky;
  top: 0;
  z-index: 100;
  user-select: none;
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
  color: #fff;
  font-size: 18px;
  letter-spacing: .3px;

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
    overflow: hidden;

    img { width: 100%; height: 100%; object-fit: cover; }
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

  > li {
    position: relative;
    list-style: none;
  }

  > li > a {
    display: inline-block;
    padding: 8px 14px;
    border-radius: 6px;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.92);
    cursor: pointer;
    transition: background .15s, color .15s;

    &:hover {
      background: rgba(255, 255, 255, 0.16);
      color: #fff;
    }
  }

  .nav-cta {
    background: #fff;
    color: var(--brand);

    &:hover {
      background: #fff;
      color: var(--brand-dark);
    }
  }
}

/* 下拉菜单 */
.drop {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  transition: background .15s, color .15s;

  &:hover {
    background: rgba(255, 255, 255, 0.16);
    color: #fff;
  }

  .caret {
    font-size: 10px;
    color: #909399;
    transition: transform .2s;
  }

  &:hover .caret {
    transform: rotate(180deg);
  }

  .drop-list {
    position: absolute;
    top: calc(100% + 6px);
    right: 0;
    min-width: 150px;
    background: #fff;
    border: 1px solid #E4E7ED;
    border-radius: 8px;
    box-shadow: 0 6px 16px rgba(31, 45, 61, 0.10);
    padding: 6px;
    margin: 0;
    list-style: none;
    opacity: 0;
    visibility: hidden;
    transform: translateY(-4px);
    transition: opacity .15s, transform .15s, visibility .15s;

    li a {
      display: block;
      padding: 8px 12px;
      border-radius: 6px;
      font-size: 14px;
      color: #1F2D3D;
      cursor: pointer;

      &:hover {
        background: #F5F7FA;
        color: var(--brand);
      }
    }

    .danger {
      color: #F56C6C;

      &:hover {
        background: #FEF0F0;
        color: #F56C6C;
      }
    }
  }

  &:hover .drop-list {
    opacity: 1;
    visibility: visible;
    transform: translateY(0);
  }
}

/* 主体 */
.page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  box-sizing: border-box;
}

.section {
  margin-bottom: 40px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #E4E7ED;
}

.section-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1F2D3D;
  letter-spacing: .2px;
}

.section-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: #909399;
}

.section-action {
  font-size: 13px;
  color: var(--brand);
  white-space: nowrap;

  &:hover {
    text-decoration: underline;
  }
}

/* 公告卡片网格 */
.notice-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

.notice-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px 20px 20px 24px;
  background: linear-gradient(135deg, #FFFFFF 0%, #F8FBFF 100%);
  border: 1px solid #E4E7ED;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(.55, .5, .45, .5);
  box-shadow: 0 1px 2px rgba(31, 45, 61, 0.04);
  overflow: hidden;

  /* 左侧渐变色条 */
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: linear-gradient(180deg, var(--brand) 0%, #7BD1A8 100%);
    opacity: 0.85;
    transition: opacity 0.25s;
  }

  &:hover {
    border-color: #B6D9EC;
    box-shadow: 0 8px 22px rgba(30, 111, 168, 0.14);
    transform: translateY(-3px);
    background: linear-gradient(135deg, #FFFFFF 0%, #F0F7FD 100%);

    &::before {
      opacity: 1;
    }

    .notice-icon-wrap {
      transform: scale(1.08);
      background: linear-gradient(135deg, #EAF3FA 0%, #D6ECFA 100%);
    }

    .notice-arrow {
      color: var(--brand);
      opacity: 1;
      transform: translateX(2px);
    }
  }
}

.notice-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #F5F7FA 0%, #EAF3FA 100%);
  transition: all 0.25s;
}

.notice-icon {
  font-size: 20px;
}

.notice-card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #1F2D3D;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 42px;
}

.notice-time {
  font-size: 12px;
  color: #B0B6C4;
}

.notice-arrow {
  font-size: 12px;
  color: #909399;
  opacity: 0.7;
  transition: color 0.2s, opacity 0.2s, transform 0.2s;
}

.notice-empty {
  padding: 16px;
  text-align: center;
  font-size: 13px;
  color: #909399;
  background: #fff;
  border: 1px solid #E4E7ED;
  border-radius: 10px;
}

/* 公告弹窗里的换行文本 */
.textNotice {
  white-space: pre-wrap;
  word-break: break-all;
}

/* ========== 手机端适配 ========== */
@media (max-width: 768px) {
  /* 顶栏 */
  .topbar-inner {
    height: auto;
    min-height: 56px;
    padding: 8px 16px;
    flex-wrap: wrap;
    gap: 8px;
  }

  .brand {
    font-size: 16px;
    gap: 8px;

    .brand-mark {
      width: 26px;
      height: 26px;
      font-size: 10px;
    }
  }

  .nav {
    gap: 2px;
    flex-wrap: wrap;

    > li > a {
      padding: 6px 10px;
      font-size: 13px;
    }
  }

  .drop {
    padding: 6px 10px;
    font-size: 13px;
  }

  /* 主体 */
  .page {
    padding: 20px 12px 32px;
  }

  .section {
    margin-bottom: 28px;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 12px;
    padding-bottom: 10px;
  }

  .section-title {
    font-size: 17px;
  }

  .section-desc {
    font-size: 12px;
  }

  .section-action {
    font-size: 12px;
  }

  /* 公告卡片 */
  .notice-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .notice-card {
    padding: 14px 14px 14px 18px;
    gap: 12px;
  }

  .notice-icon-wrap {
    width: 36px;
    height: 36px;
  }

  .notice-icon {
    font-size: 18px;
  }

  .notice-title {
    font-size: 14px;
    min-height: auto;
  }

  /* 公告弹窗 */
  .textNotice {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .topbar-inner {
    padding: 6px 12px;
  }

  .brand {
    font-size: 15px;
  }

  .nav > li > a {
    padding: 5px 8px;
    font-size: 12px;
  }

  .drop {
    padding: 5px 8px;
    font-size: 12px;
  }

  .page {
    padding: 16px 10px 24px;
  }

  .section-title {
    font-size: 16px;
  }
}
/* 顶栏登录用户头像与昵称 */
.drop .user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;

  :deep(.my-avatar) { width: 30px; height: 30px; flex-shrink: 0; }

  .user-name {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-weight: 600;
    color: #1F2D3D;
  }
}

/* 回到顶部 */
.back-top {
  position: fixed;
  right: 28px;
  bottom: 40px;
  z-index: 200;
  width: 46px;
  height: 46px;
  font-size: 20px;
  box-shadow: 0 6px 18px rgba(31, 45, 61, .22);
}

</style>