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
import type {FlowStage} from '@/request/FlowApi'
import {reqStatOverview} from '@/request/StatisticsApi'
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
  {key: 'm-report', label: '经营报表', hint: '收支趋势', icon: Right, path: '/backstage/z-report'},
  {key: 'm-bill', label: '收银/账单', hint: '待结算', icon: Right, path: '/backstage/y-billList?payStatus=0'},
  {key: 'm-drug', label: '药品库存', hint: '低库存/近效期', icon: Right, path: '/backstage/y-drugList'},
  {key: 'm-hosp', label: '住院管理', hint: '在院动物', icon: Right, path: '/backstage/z-hospital'},
]

const loadAll = async (): Promise<void> => {
  try {
    const s: any = await reqStatOverview()
    const yuan = (n: number) => '￥' + (n ?? 0).toFixed(2)
    const mk = (key: string, title: string, desc: string, count: number, chip: string, sub: string, path: string, cls?: string) =>
      ({key, title, desc, actionPath: path, actionLabel: '查看', count, items: [{key, petName: chip, sub, stageTitle: title, nextLabel: '查看', actionPath: path, cls}]} as FlowStage)
    stages.value = [
      mk('rev', '今日营收', '营业收入', Math.round(s.todayRevenue || 0), yuan(s.todayRevenue), '今日营业收入 · 月 ' + yuan(s.monthRevenue), '/backstage/z-report', 'hot'),
      mk('appt', '今日挂号', '预约/到店', s.todayAppoint || 0, (s.todayAppoint || 0) + ' 笔', '今日预约 · 已接诊 ' + (s.todayRecord || 0), '/backstage/y-appointmentList'),
      mk('pay', '待结算', '未收款账单', s.pendingBills || 0, (s.pendingBills || 0) + ' 笔', '未收款账单', '/backstage/y-billList?payStatus=0', s.pendingBills ? 'danger' : ''),
      mk('dis', '待发药', '已收款待发药', s.pendingDispense || 0, (s.pendingDispense || 0) + ' 笔', '待发药', '/backstage/y-billList'),
      mk('exam', '待检/待影像', '医技积压', (s.pendingLab || 0) + (s.pendingImage || 0), (s.pendingLab || 0) + '/' + (s.pendingImage || 0), '化验 ' + (s.pendingLab || 0) + ' · 影像 ' + (s.pendingImage || 0), '/doctorPage/z-examResult'),
      mk('hosp', '住院中', '在院动物', s.inHospital || 0, (s.inHospital || 0) + ' 例', '在院动物', '/backstage/z-hospital'),
      mk('vac', '到期疫苗', '待提醒', s.dueVaccine || 0, (s.dueVaccine || 0) + ' 只', '到期/逾期免疫', '/doctorPage/z-immune'),
      mk('drug', '药品预警', '低库存/近效期', (s.lowStock || 0) + (s.expiringDrug || 0), '低 ' + (s.lowStock || 0) + ' · 近效 ' + (s.expiringDrug || 0), '低库存 ' + (s.lowStock || 0) + ' · 近效期 ' + (s.expiringDrug || 0), '/backstage/y-drugList', (s.lowStock || s.expiringDrug) ? 'warn' : ''),
    ]
    if (s.lowStock) pushEvent(`${s.lowStock} 种药品库存偏低`, 'warn')
    if (s.pendingBills) pushEvent(`${s.pendingBills} 笔账单待收银`, 'warn')
    if (s.inHospital) pushEvent(`${s.inHospital} 例动物在院`, 'info')
    lastRefresh.value = `${p(new Date().getHours())}:${p(new Date().getMinutes())}:${p(new Date().getSeconds())}`
  } catch (e) {
    pushEvent('加载失败，稍后重试', 'err')
  }
}
const onNavigate = (path: string): void => { if (path) router.push(path) }
const onLogout = (): void => { reqLogout().then(res => { clearPwdCheckFlag(); if (res.code === 1) router.push('/login') }).catch(() => ElMessage.info('已取消')) }

let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => { loadAll(); timer = setInterval(loadAll, 30000) })
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>
