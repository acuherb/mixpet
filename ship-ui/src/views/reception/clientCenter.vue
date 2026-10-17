<template>
  <div class="client-center">
    <!--导航-->
    <div class="radio-group">
      <label>
        <input v-model="checkValue" name="radio" type="radio" value="/reception">
        <span style="color: red">返回主页</span>
      </label>
      <label>
        <input v-model="checkValue" name="radio" type="radio" value="/reception/clientCenter">
        <span>个人资料</span>
      </label>
      <label>
        <input v-model="checkValue" name="radio" type="radio" value="/reception/clientCenter/clientPet">
        <span>宠物信息</span>
      </label>
      <label>
        <input v-model="checkValue" name="radio" type="radio" value="/reception/clientCenter/clientAppoint">
        <span>挂号查看<i v-if="todayAppointCount > 0" class="nav-badge">{{ todayAppointCount > 99 ? '99+' : todayAppointCount }}</i></span>
      </label>
      <label>
        <input v-model="checkValue" name="radio" type="radio" value="/reception/chat">
        <span>在线咨询</span>
      </label>
    </div>
    <!--内容区-->
    <div class="content-box">
      <router-view></router-view>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {onBeforeUnmount, onMounted, ref, watch} from 'vue'
import router from "@/router";
import {Msg} from "@/model/entiy/Msg";
import {ElNotification} from "element-plus";
import {reqClientAppoint} from "@/request/AppointApi";

const checkValue = ref<string>(router.currentRoute.value.fullPath)
watch(checkValue, () => {
  router.push(checkValue.value)
})

// 今日待就诊角标（挂号查看入口，对称医生工作台的今日待接诊）
const todayAppointCount = ref<number>(0)
const loadTodayAppoint = (): void => {
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  reqClientAppoint().then(list => {
    todayAppointCount.value = (list || []).filter(a => !a.isSuc && (a.appointmentDate || '').slice(0, 10) === todayStr).length
  })
}
onMounted(loadTodayAppoint)

const previewOf = (m: Msg): string => {
  if (m.isImg) return '［图片］'
  if (m.msgType === 'FILE') return `［文件］${m.msgName || ''}`
  return m.msgContent || ''
}

// WebSocket：顾客在个人中心时，实时接收医生回复并提醒（未读角标已在首页“咨询医生”入口展示）
const wsUrl = (location.protocol === "https:" ? "wss://" : "ws://") + location.host + "/chat"
let ws: WebSocket | null = null
onMounted(() => {
  ws = new WebSocket(wsUrl)
  ws.onmessage = (ev) => {
    try {
      const m = JSON.parse(ev.data) as Msg
      ElNotification({
        title: '医生回复了您',
        message: previewOf(m),
        type: 'success',
        duration: 6000,
        position: 'top-right',
        onClick: () => { ElNotification.closeAll(); router.push('/reception/chat') }
      })
    } catch { /* 忽略非消息帧 */ }
  }
})
onBeforeUnmount(() => {
  ws?.close(1000, '离开个人中心')
  ElNotification.closeAll()
})
</script>

<style lang="scss" scoped>
$txt-color: #337ecc;
$round-color: #DBA21C;
$bord-color: #887456;
.client-center {
  width: 100%;
  max-width: 100vw;
  min-height: calc(100vh - 80px);
  box-sizing: border-box;
  padding: 0 40px 40px;
}

/* 内容区：全屏自适应，白底，轻阴影 */
.content-box {
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  padding: 28px 36px;
  min-height: 500px;
  box-sizing: border-box;
}

/* 导航角标：今日待就诊（红色小圆标） */
.nav-badge {
  display: inline-block;
  min-width: 16px;
  margin-left: 4px;
  padding: 0 4px;
  background: #f56c6c;
  color: #fff;
  border-radius: 8px;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
  font-style: normal;
  font-weight: 600;
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 20px 0;

  label {
    max-width: 240px;
    display: flex;
    cursor: pointer;
    font-weight: 500;
    position: relative;
    overflow: hidden;
    margin-bottom: 0;

    input {
      display: none;

      &:checked + span {
        background-color: $txt-color;
        color: white;

        &:before {
          box-shadow: inset 0 0 0 0.4375em $round-color;
        }
      }
    }

    span {
      display: flex;
      align-items: center;
      font-size: 15px;
      padding: 10px 20px 10px 12px;
      border-radius: 99em;
      transition: 0.25s ease;
      color: $txt-color;
      border: 1px solid transparent;

      &:hover {
        background-color: #b3e19d;
      }

      &:before {
        display: flex;
        flex-shrink: 0;
        content: "";
        background-color: #fff;
        width: 1.5em;
        height: 1.5em;
        border-radius: 50%;
        margin-right: 0.375em;
        transition: 0.25s ease;
        box-shadow: inset 0 0 0 0.125em $bord-color;
      }
    }
  }
}

@media (max-width: 768px) {
  .client-center {
    padding: 0 12px 24px;
  }
  .content-box {
    padding: 16px;
  }
}
</style>
