<template>
  <div class="notice-self">
    <div class="ns-head">
      <div>
        <h2>消息通知</h2>
        <p>检验/影像报告出具或修订后，系统会在这里通知您。</p>
      </div>
      <el-button size="small" type="primary" plain :disabled="!hasUnread" @click="readAll">全部标为已读</el-button>
    </div>
    <el-empty v-if="!loading && !list.length" description="暂无通知"/>
    <div v-for="n in list" :key="n.notifyId" class="ns-item" :class="{unread: !n.isRead}" @click="markRead(n)">
      <div class="ns-top">
        <b>{{ n.title }}</b>
        <span class="ns-time">{{ (n.createTime || '').replace('T', ' ').slice(0, 16) }}</span>
      </div>
      <p>{{ n.content }}</p>
      <el-tag v-if="!n.isRead" size="small" type="danger" effect="plain">未读 · 点击查看详情</el-tag>
      <el-button v-if="n.refType === 'lab_order' || n.refType === 'image_order'" link type="primary" size="small"
                 @click.stop="goPet">前往宠物档案查看
      </el-button>
      <el-button v-if="n.refType === 'foster'" link type="primary" size="small"
                 @click.stop="goFoster">查看我的寄养
      </el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {ClientNotification, reqNotifyRead, reqNotifyReadAll, reqNotifySelf} from "@/request/NotifyApi";

const router = useRouter()
const list = ref<ClientNotification[]>([])
const loading = ref(true)
const hasUnread = computed(() => list.value.some(n => !n.isRead))
/** 通知角标在父组件，读完后广播让它即时刷新 */
const broadcast = (): void => { window.dispatchEvent(new Event('mixpet:notifyChanged')) }
const loadList = (): void => {
  reqNotifySelf().then(data => { list.value = data || [] })
      .catch(() => ElMessage.error('通知加载失败，请稍后重试'))
      .finally(() => { loading.value = false })
}
const markRead = (n: ClientNotification): void => {
  if (n.isRead) return
  reqNotifyRead(n.notifyId).then(() => {
    n.isRead = true
    broadcast()
  }).catch(() => ElMessage.error('操作失败'))
}
const readAll = (): void => {
  reqNotifyReadAll().then(() => {
    list.value.forEach(n => { n.isRead = true })
    broadcast()
  }).catch(() => ElMessage.error('操作失败'))
}
const goPet = (): void => { router.push('/reception/clientCenter/clientPet') }
const goFoster = (): void => { router.push('/reception/clientCenter/clientFoster') }
onMounted(loadList)
</script>

<style lang="scss" scoped>
.notice-self { padding: 4px 0; }
.ns-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 18px;
  h2 { margin: 0 0 6px; font-size: 20px; color: #253858; }
  p { margin: 0; font-size: 13px; color: #8a96a8; }
}
.ns-item {
  padding: 14px 16px; margin-bottom: 10px; border-radius: 10px;
  border: 1px solid #e8edf3; background: #fff; cursor: pointer;
  transition: border-color .2s;
  &:hover { border-color: #409eff; }
  &.unread { background: #f2f7ff; border-left: 3px solid #409eff; }
  p { margin: 6px 0 8px; font-size: 13px; color: #5b6b82; }
}
.ns-top { display: flex; justify-content: space-between; align-items: center; }
.ns-top b { font-size: 14px; color: #253858; }
.ns-time { font-size: 12px; color: #98a3b3; }
</style>
