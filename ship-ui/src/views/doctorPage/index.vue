<template>
  <div class="doctor-home">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand">
          <span class="brand-mark">DR</span>
          <span class="brand-name">pet-hms 医生工作台</span>
        </div>
        <ul class="nav">
          <li><router-link to="/backstage">个人中心</router-link></li>
          <li><router-link to="/doctorPage/d-appoint">处理挂号</router-link></li>
          <li><router-link to="/doctorPage/d-chat">在线咨询</router-link></li>
        </ul>
      </div>
    </header>

    <main class="page">
      <div class="page-head">
        <h1 class="page-title">工作台</h1>
        <p class="page-desc">
          <span class="clock-dot"></span>
          当前时间 <span class="clock-num">{{ timeText }}</span>
        </p>
      </div>

      <MyRow>
        <div class="entry-card">
          <div class="entry-head">
            <h3>个人中心</h3>
            <p>查看与维护个人信息</p>
          </div>
          <router-link to="/backstage">
            <el-button plain size="large">进入</el-button>
          </router-link>
        </div>

        <div class="entry-card">
          <div class="entry-head">
            <h3>处理挂号</h3>
            <p>查看待处理挂号并进行安排</p>
          </div>
          <router-link to="/doctorPage/d-appoint">
            <el-button size="large" type="primary">处理挂号</el-button>
          </router-link>
        </div>

        <div class="entry-card">
          <div class="entry-head">
            <h3>在线咨询</h3>
            <p>回复顾客的在线咨询</p>
          </div>
          <router-link to="/doctorPage/d-chat">
            <el-button plain size="large">进入咨询</el-button>
          </router-link>
        </div>
      </MyRow>
    </main>
  </div>
</template>

<script lang="ts" setup>
import {onBeforeUnmount, onMounted, ref} from "vue";
import MyRow from "@/components/row/MyRow.vue";

const timeText = ref<string>('--:--:--')
let timeTimer: ReturnType<typeof setInterval> | undefined

const pad = (n: number) => n.toString().padStart(2, '0')
const tick = () => {
  const t = new Date()
  timeText.value = `${pad(t.getHours())}:${pad(t.getMinutes())}:${pad(t.getSeconds())}`
}

onMounted(() => {
  tick()
  timeTimer = setInterval(tick, 1000)
})

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
  margin-bottom: 24px;

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
  }
}

/* 入口卡片 */
.entry-card {
  width: 320px;
  max-width: 92vw;
  background: #fff;
  border: 1px solid #E4E7ED;
  border-radius: 8px;
  padding: 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 20px;
  box-shadow: 0 1px 2px rgba(31, 45, 61, 0.04);
  transition: box-shadow .2s, border-color .2s, transform .2s;

  &:hover {
    border-color: #1E6FA8;
    box-shadow: 0 6px 16px rgba(31, 45, 61, 0.10);
    transform: translateY(-1px);
  }

  .entry-head {
    h3 {
      margin: 0 0 6px;
      font-size: 16px;
      font-weight: 600;
      color: #1F2D3D;
    }

    p {
      margin: 0;
      font-size: 13px;
      color: #909399;
      line-height: 1.5;
    }
  }
}
</style>
