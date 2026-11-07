<template>
  <FlowPipeline
    title="前台接待流"
    brand-name="接待"
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
import {Right, User} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import FlowPipeline from '@/views/doctorPage/FlowPipeline.vue'
import type {FlowStage} from '@/request/FlowApi'
import {reqAppointList} from '@/request/AppointApi'
import {reqBillPage} from '@/request/BillApi'
import {reqLogout} from '@/request/PowerApi'
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
  {key: 'r-appt', label: '预约挂号', hint: '今日预约队列', icon: User, path: '/backstage/y-appointmentList'},
  {key: 'r-pay', label: '收银台', hint: '待结算账单', icon: Right, path: '/backstage/y-billList?payStatus=0'},
]

const isToday = (d: string | null): boolean => {
  if (!d) return false
  const t = new Date()
  const today = `${t.getFullYear()}-${p(t.getMonth() + 1)}-${p(t.getDate())}`
  return d.slice(0, 10) === today
}

const loadAll = async (): Promise<void> => {
  try {
    const [appt, bills] = await Promise.all([
      reqAppointList({numPage: 1, pageSize: 200, seaName: '', seaType: 0}),
      reqBillPage({numPage: 1, pageSize: 50, payStatus: 0}),
    ])
    const a = (appt?.records || []) as any[]
    const b = (bills?.records || []) as any[]
    const today = a.filter(x => isToday(x.appointmentDate))
    const done = today.filter(x => x.isSuc)
    const wait: FlowStage = {key: 'appt', title: '今日预约', desc: '全部预约·按到店引导', actionPath: '/backstage/y-appointmentList', actionLabel: '去挂号', count: today.length, items: today.map(x => ({
      key: 'a' + x.appointmentId, petName: x.petName, sub: (x.clientName || '') + ' · ' + (x.appointmentDate || '').slice(11, 16), stageTitle: '今日预约', nextLabel: '办理/分诊', actionPath: '/backstage/y-appointmentList'
    }))}
    const pay: FlowStage = {key: 'pay', title: '待收银', desc: '已完结·未收款', actionPath: '/backstage/y-billList?payStatus=0', actionLabel: '去收银', count: b.length, items: b.map(x => ({
      key: 'b' + x.billId, petName: x.petName, sub: (x.clientName || '') + ' · ￥' + (x.realMoney ?? x.totalMoney ?? 0).toFixed(2), stageTitle: '待收银', nextLabel: '收款', actionPath: '/backstage/y-billList?payStatus=0'
    }))}
    const doneStage: FlowStage = {key: 'done', title: '今日已诊', desc: '已接诊完成', actionPath: '/backstage/y-appointmentList', actionLabel: '查看', count: done.length, items: done.slice(0, 12).map(x => ({
      key: 'd' + x.appointmentId, petName: x.petName, sub: (x.clientName || '') + ' · 已完成', stageTitle: '今日已诊', nextLabel: '复盘', actionPath: '/backstage/y-appointmentList'
    }))}
    stages.value = [wait, pay, doneStage]
    if (b.length) pushEvent(`${b.length} 笔账单待收银`, 'warn')
    if (today.length > done.length) pushEvent(`${today.length - done.length} 位预约客户在店待引导`, 'info')
    lastRefresh.value = `${p(new Date().getHours())}:${p(new Date().getMinutes())}:${p(new Date().getSeconds())}`
  } catch (e) {
    pushEvent('加载失败，稍后重试', 'err')
  }
}
const onNavigate = (path: string): void => { if (path) router.push(path) }
const onLogout = (): void => { reqLogout().then(res => { clearPwdCheckFlag(); if (res.code === 1) router.push('/login') }).catch(() => ElMessage.info('已取消')) }

let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => { loadAll(); timer = setInterval(loadAll, 20000) })
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>
