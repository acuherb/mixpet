<template>
  <FlowPipeline
    title="经营驾驶舱"
    brand-name="经营"
    :stages="stages"
    :events="events"
    :last-refresh="lastRefresh"
    :commands="commands"
    @navigate="onNavigate"
    @logout="onLogout"
  />
</template>

<script lang="ts" setup>
import {onBeforeUnmount, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {Right} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import FlowPipeline from '@/views/doctorPage/FlowPipeline.vue'
import {reqManagerPipeline, type FlowStage} from '@/request/FlowApi'
import {reqLogout} from '@/request/PowerApi'
import {subscribeFlow} from '@/utils/chatSocket'
import {clearPwdCheckFlag} from '@/router'

const router = useRouter()
const stages = ref<FlowStage[]>([])
const lastRefresh = ref('')
const events = ref<{id: number; text: string; time: string; cls: string; icon: any}[]>([])
let evtId = 0
const p = (n: number) => n.toString().padStart(2, '0')
const pushEvent = (text: string, cls = 'info', icon: any = Right) => {
  const t = new Date()
  events.value.unshift({id: ++evtId, text, time: `${p(t.getHours())}:${p(t.getMinutes())}:${p(t.getSeconds())}`, cls, icon})
  if (events.value.length > 20) events.value.pop()
}
const commands = [
  {key: 'm-report', label: '经营报表', hint: '收支趋势', icon: Right, path: '/backstage/z-report'},
  {key: 'm-bill', label: '收银/账单', hint: '待结算', icon: Right, path: '/backstage/y-billList?payStatus=0'},
  {key: 'm-drug', label: '药品库存', hint: '低库存/近效期', icon: Right, path: '/backstage/y-drugList'},
  {key: 'm-hosp', label: '住院管理', hint: '在院动物', icon: Right, path: '/backstage/z-hospital'},
]

let snap = {pay: 0, drug: 0, appt: 0}
const loadAll = async (): Promise<void> => {
  try {
    const vo = await reqManagerPipeline()
    stages.value = vo.stages || []
    const now = {pay: 0, drug: 0, appt: 0}
    for (const s of stages.value) if (s.key in now) (now as any)[s.key] = s.count
    if (snap.pay && now.pay < snap.pay) pushEvent(`${snap.pay - now.pay} 笔账单已结清`, 'ok')
    if (snap.appt && now.appt > snap.appt) pushEvent(`今日挂号增至 ${now.appt} 笔`, 'info')
    snap = now
    lastRefresh.value = `${p(new Date().getHours())}:${p(new Date().getMinutes())}:${p(new Date().getSeconds())}`
  } catch (e) {
    pushEvent('加载失败（可能需要管理权限），稍后重试', 'err')
  }
}
const onNavigate = (path: string): void => { if (path) router.push(path) }
const onLogout = (): void => { reqLogout().then(res => { clearPwdCheckFlag(); if (res.code === 1) router.push('/login') }).catch(() => ElMessage.info('已取消')) }

let timer: ReturnType<typeof setInterval> | undefined
let unsub: (() => void) | null = null
const onRefresh = (): void => { loadAll() }
onMounted(() => {
  loadAll()
  timer = setInterval(loadAll, 30000)
  unsub = subscribeFlow((e: any) => {
    if (e?.text) pushEvent(e.text, 'ok')
    loadAll()
  })
  window.addEventListener('mixpet:flow-refresh', onRefresh as EventListener)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  unsub?.(); unsub = null
  window.removeEventListener('mixpet:flow-refresh', onRefresh as EventListener)
})
</script>
