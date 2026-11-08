<template>
  <div class="d-flow-shell">
    <div v-if="loadError" style="padding:48px 16px">
      <el-result icon="error" title="诊疗流暂时无法加载"
                 sub-title="数据未取到，这不代表今天没有患者。请重试，或联系管理员检查服务与数据库。">
        <template #extra><el-button type="primary" @click="loadAll">重新加载</el-button></template>
      </el-result>
    </div>
    <FlowPipeline
      v-else
      title="诊疗流"
      brand-name="mixpet"
      :stages="stages"
      :events="events"
      :last-refresh="lastRefresh"
      :commands="commands"
      @navigate="onNavigate"
      @logout="doLogout"
    >
    </FlowPipeline>

    <!-- 内联抽屉：深页在流内直接处理，不离开这条河（行云流水） -->
    <el-drawer v-model="inlineOpen" :title="inlineTitle" size="76%" direction="rtl" :before-close="closeInline">
      <router-view v-if="inlineOpen"/>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import {onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {Right, User} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import FlowPipeline from '@/views/doctorPage/FlowPipeline.vue'
import {reqDoctorPipeline, type FlowStage} from '@/request/FlowApi'
import {reqLogout} from '@/request/PowerApi'
import {subscribeFlow} from '@/utils/chatSocket'
import {clearPwdCheckFlag} from '@/router'

const router = useRouter()
const route = useRoute()

const loadError = ref(false)
const stages = ref<FlowStage[]>([])
const lastRefresh = ref('')
const events = ref<{id: number; text: string; time: string; cls: string; icon: any}[]>([])
let evtId = 0
const pushEvent = (text: string, cls = 'info', icon: any = Right) => {
  const t = new Date()
  events.value.unshift({id: ++evtId, text, time: `${p(t.getHours())}:${p(t.getMinutes())}:${p(t.getSeconds())}`, cls, icon})
  if (events.value.length > 30) events.value.pop()
}
const p = (n: number) => n.toString().padStart(2, '0')

const commands = [
  {key: 'p-wait', label: '接诊队列', hint: '待诊 → 去接诊', icon: User, path: '/doctorPage/d-appoint'},
  {key: 'p-result', label: '医技结果录入', hint: '化验 / 影像报告', icon: Right, path: '/doctorPage/z-examResult'},
  {key: 'p-pay', label: '收银台', hint: '待结算账单', icon: Right, path: '/doctorPage/y-billList?payStatus=0'},
  {key: 'p-follow', label: '随访提醒', hint: '待回访名单', icon: Right, path: '/doctorPage/z-follow'},
  {key: 'p-rx', label: '开处方', hint: '进入接诊开单', icon: Right, path: '/doctorPage/d-appoint'},
]

let snap = {wait: 0, treat: 0, result: 0, pay: 0, follow: 0}
const loadAll = async (): Promise<void> => {
  try {
    const vo = await reqDoctorPipeline()
    loadError.value = false
    stages.value = vo.stages || []
    const now = {wait: 0, treat: 0, result: 0, pay: 0, follow: 0}
    for (const s of stages.value) if (s.key in now) (now as any)[s.key] = s.count
    if (snap.result && now.result < snap.result) pushEvent(`${snap.result - now.result} 项医技结果已回报，可复核/结算`, 'ok')
    if (snap.pay && now.pay < snap.pay) pushEvent(`${snap.pay - now.pay} 笔账单已收款，流转至随访`, 'ok')
    if (snap.wait && now.wait > snap.wait) pushEvent(`新增 ${now.wait - snap.wait} 个今日挂号待接诊`, 'warn')
    snap = now
    lastRefresh.value = `${p(new Date().getHours())}:${p(new Date().getMinutes())}:${p(new Date().getSeconds())}`
  } catch (e) {
    loadError.value = true
    pushEvent('数据加载失败，请点「重新加载」重试', 'err')
  }
}

// —— 导航：流内动作优先内联到子路由抽屉，否则整页跳转 ——
const inlineOpen = ref(false)
const inlineTitle = ref('处理')
const inlineMap: Record<string, string> = {
  '/doctorPage/d-appoint': 'appoint',
  '/doctorPage/z-examResult': 'exam',
  '/doctorPage/y-billList': 'pay',
  '/doctorPage/z-follow': 'follow',
}
const onNavigate = (path: string): void => {
  if (!path) return
  const qIndex = path.indexOf('?')
  const base = qIndex >= 0 ? path.slice(0, qIndex) : path
  const qs = qIndex >= 0 ? path.slice(qIndex) : ''
  const child = inlineMap[base]
  if (child) {
    inlineTitle.value = '处理 · ' + (inlineMap[base] === 'appoint' ? '接诊' : inlineMap[base] === 'exam' ? '医技结果' : inlineMap[base] === 'pay' ? '收银' : '随访')
    inlineOpen.value = true
    router.push('/doctorPage/d-flow/' + child + qs)
  } else {
    router.push(path)
  }
}
const closeInline = (done: () => void): void => {
  inlineOpen.value = false
  router.push('/doctorPage/d-flow').then(() => { loadAll(); done() })
}

// 抽屉开合跟随路由：深页内部 router.push 被守卫重写到 d-flow/inline 时，
// 抽屉保持打开、内容无缝切换，患者全程不离开这条河。
const inlineNames: Record<string, string> = {
  appoint: '接诊', exam: '医技结果', pay: '收银', follow: '随访'
}
watch(() => route.path, (p: string): void => {
  if (!p.startsWith('/doctorPage/d-flow/')) {
    inlineOpen.value = false
    return
  }
  inlineOpen.value = true
  const leaf = p.split('/').pop() || ''
  if (leaf === 'inline') {
    const t = String(route.query.__flowTarget || '')
    inlineTitle.value = '处理 · ' + (t.split('/').pop() || '业务页')
  } else {
    inlineTitle.value = '处理 · ' + (inlineNames[leaf] || leaf)
  }
}, {immediate: true})

const doLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '退出登录', {confirmButtonText: '确定退出', cancelButtonText: '取消', type: 'warning'})
    .then(() => { reqLogout().then(res => { clearPwdCheckFlag(); if (res.code === 1) router.push('/login') }) })
    .catch(() => ElMessage.info('已取消'))
}

// 深页完成动作后回到流的事件
const onRefresh = () => loadAll()
let unsub: (() => void) | null = null
let pollTimer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  loadAll()
  pollTimer = setInterval(loadAll, 20000)
  unsub = subscribeFlow((e: any) => {
    if (e?.text) pushEvent(e.text, 'ok')
    loadAll()
  })
  window.addEventListener('mixpet:flow-refresh', onRefresh as EventListener)
})
onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
  unsub?.(); unsub = null
  window.removeEventListener('mixpet:flow-refresh', onRefresh as EventListener)
})
</script>
