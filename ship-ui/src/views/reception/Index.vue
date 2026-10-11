<template>
  <div class="reception-home">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/reception/clientCenter" class="brand">
          <span class="brand-mark">PET</span>
          <span class="brand-name">pet-hms</span>
        </router-link>
        <ul class="nav">
          <li><a @click="openDialog(notices[0])">网站公告</a></li>
          <li>
            <router-link to="/reception/chat">咨询医生</router-link>
          </li>
          <li v-if="isLogin" class="drop">
            <span>个人中心</span>
            <span class="caret">▾</span>
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
            <router-link class="nav-cta" to="/">前往登录</router-link>
          </li>
        </ul>
      </div>
    </header>

    <WindRoll :is-login="isLogin"/>

    <main class="page">
      <!-- 公告 -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2 class="section-title">网站公告</h2>
            <p class="section-desc">最新通知与动态</p>
          </div>
        </div>
        <MyRow>
          <NoticeBtn v-for="notice in notices" :key="notice.noticeId" @click="openDialog(notice)">
            {{ notice.noticeTitle }}
          </NoticeBtn>
        </MyRow>
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

    <VueFoot/>
  </div>
</template>

<script lang="ts" setup>
import {onBeforeMount, reactive, ref} from "vue";
import {Pet} from "@/model/entiy/Pet";
import {Page} from "@/model/DO/Page";
import PetCard from "@/components/zoneView/PetCard.vue";
import DoctorCard from "@/components/zoneView/DoctorCard.vue";
import FosterCard from "@/components/card/FosterCard.vue";
import {DoctorDto} from "@/model/DO/DoctorDto";
import {FosterCardDto} from "@/model/DO/FosterCardDto";
import {NoticeDto} from "@/model/DO/NoticeDto";
import {reqLoginUser, reqLogout} from "@/request/PowerApi";
import {reqFourDoctor} from "@/request/DoctorApi";
import {reqFourPet} from "@/request/PetApi";
import {reqFosterPet} from "@/request/FosterApi";
import {reqFourNotice} from "@/request/NoticeApi";
import {useRouter} from "vue-router";
import VueFoot from "@/components/show/VueFoot.vue";
import WindRoll from "@/components/show/WindRoll.vue";
import MyRow from "@/components/row/MyRow.vue";
import NoticeBtn from "@/components/button/NoticeBtn.vue";
import LinkCard from "@/components/card/LinkCard.vue";
import MyDialog from "@/components/show/MyDialog.vue";

/* 生命周期 */
const isLogin = ref<boolean>(false)
onBeforeMount(() => {
  reqFourDoctor().then(res => {
    doctorCardTxt.records = res.records
  })
  reqFourPet().then(res => {
    petCardTxt.records = res.records
  })
  reqFosterPet(4).then(res => {
    fosterCardTxt.records = res.records
  })
  reqFourNotice().then(res => {
    notices.value = res
  })
  reqLoginUser().then(res => {
    isLogin.value = (res.clientId != null)
  })
})

/* 公告栏 */
const notices = ref<NoticeDto[]>([])
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
  reqLogout().then(res => {
    if (res.code === 204) router.push("/")
  })
}
</script>

<style lang="scss" scoped>
.reception-home {
  min-height: 100vh;
  background: #F5F7FA;
}

/* 顶部导航 */
.topbar {
  background: #fff;
  border-bottom: 1px solid #E4E7ED;
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
  color: #1E6FA8;
  font-size: 18px;
  letter-spacing: .3px;

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

  > li {
    position: relative;
    list-style: none;
  }

  > li > a {
    display: inline-block;
    padding: 8px 14px;
    border-radius: 6px;
    font-size: 14px;
    color: #1F2D3D;
    cursor: pointer;
    transition: background .15s, color .15s;

    &:hover {
      background: #EAF3FA;
      color: #1E6FA8;
    }
  }

  .nav-cta {
    background: #1E6FA8;
    color: #fff;

    &:hover {
      background: #2A82C2;
      color: #fff;
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
  color: #1F2D3D;
  cursor: pointer;
  transition: background .15s, color .15s;

  &:hover {
    background: #EAF3FA;
    color: #1E6FA8;
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
        color: #1E6FA8;
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
  color: #1E6FA8;
  white-space: nowrap;

  &:hover {
    text-decoration: underline;
  }
}

/* 公告弹窗里的换行文本 */
.textNotice {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
