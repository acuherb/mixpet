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
      :commands="visibleCommands"
      @navigate="onNavigate"
      @logout="doLogout"
    >
      <template #nav-right>
        <li v-if="isManager"><router-link to="/backstage/m-flow"><el-icon><Setting/></el-icon> 后台管理</router-link></li>
        <li><router-link to="/doctorPage/d-console"><el-icon><HomeFilled/></el-icon> 医生操作台</router-link></li>
      </template>
    </FlowPipeline>

    <!-- 内联抽屉：深页在流内直接处理，不离开这条河（行云流水） -->
    <el-drawer v-model="inlineOpen" :title="inlineTitle" size="76%" direction="rtl" :before-close="closeInline">
      <router-view v-if="inlineOpen"/>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {
  Bell, Calendar, Collection, Document, DocumentChecked, Files, FolderOpened, HomeFilled,
  House, Money, Phone, Picture, Printer, Right, Setting, User,
} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import FlowPipeline from '@/views/doctorPage/FlowPipeline.vue'
import {reqDoctorPipeline, type FlowStage} from '@/request/FlowApi'
import {reqLogout} from '@/request/PowerApi'
import {reqGetLoginD} from '@/request/DoctorApi'
import {subscribeFlow} from '@/utils/chatSocket'
import {clearPwdCheckFlag} from '@/router'
import {canOpenAction} from '@/utils/PermissionUtil'

const router = useRouter()
const route = useRoute()

// 管理者（MANAGER=6 / ADMIN=8）才在顶部看到「后台管理」互切入口；取不到权限时按普通医生处理
const isManager = ref(false)
const authLv = ref(0)
reqGetLoginD().then(d => {
  authLv.value = Number(d?.authLv ?? 0)
  isManager.value = authLv.value >= 6
}).catch(() => {})

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

// 命令面板降级为「熟练用户加速器」：完整入口按岗位分组，主界面另有医生操作台可点
const commands = [
  {key: 'c-appoint', label: '接诊 / 写病历', hint: '接诊与开单', group: '接诊与开单', icon: User, path: '/doctorPage/d-appoint'},
  {key: 'c-queue', label: '挂号 / 接诊队列', hint: '接诊与开单', group: '接诊与开单', icon: Calendar, path: '/doctorPage/y-appointmentList'},
  {key: 'c-lab', label: '开化验单', hint: '接诊与开单', group: '接诊与开单', icon: Document, path: '/doctorPage/d-lab'},
  {key: 'c-image', label: '开影像单', hint: '接诊与开单', group: '接诊与开单', icon: Picture, path: '/doctorPage/d-image'},
  {key: 'c-rx', label: '开处方', hint: '接诊与开单', group: '接诊与开单', icon: Files, path: '/doctorPage/d-rx'},
  {key: 'c-record', label: '病例中心', hint: '档案与打印', group: '档案与打印', icon: FolderOpened, path: '/doctorPage/d-record'},
  {key: 'c-print', label: '病历打印', hint: '档案与打印', group: '档案与打印', icon: Printer, path: '/doctorPage/d-recordPrint'},
  {key: 'c-result', label: '医技结果录入', hint: '结果与报告', group: '结果与报告', icon: DocumentChecked, path: '/doctorPage/z-examResult'},
  {key: 'c-report', label: '报告单打印', hint: '结果与报告', group: '结果与报告', icon: Printer, path: '/doctorPage/z-examReport'},
  {key: 'c-bill', label: '收银台', hint: '收费与随访', group: '收费与随访', icon: Money, path: '/doctorPage/y-billList?payStatus=0'},
  {key: 'c-follow', label: '随访记录', hint: '收费与随访', group: '收费与随访', icon: Phone, path: '/doctorPage/z-follow'},
  {key: 'c-remind', label: '复诊/疫苗提醒', hint: '收费与随访', group: '收费与随访', icon: Bell, path: '/doctorPage/z-remind'},
  {key: 'c-hospital', label: '住院登记', hint: '住院与配置', group: '住院与配置', icon: House, path: '/doctorPage/z-hospital'},
  {key: 'c-diagnosis', label: '诊断字典', hint: '住院与配置', group: '住院与配置', icon: Collection, path: '/doctorPage/z-diagnosis'},
  {key: 'c-console', label: '切到医生操作台', hint: '视图', group: '视图', icon: Right, path: '/doctorPage/d-console'},
]

// 管理账号只做管理：命令面板同样不提供临床作业入口
const visibleCommands = computed(() => commands.filter(c => canOpenAction(c.path || '', authLv.value)))

let snap = {wait: 0, future: 0, treat: 0, result: 0, pay: 0, follow: 0}
const loadAll = async (): Promise<void> => {
  try {
    const vo = await reqDoctorPipeline()
    loadError.value = false
    stages.value = vo.stages || []
    const now = {wait: 0, future: 0, treat: 0, result: 0, pay: 0, follow: 0}
    for (const s of stages.value) if (s.key in now) (now as any)[s.key] = s.count
    if (snap.result && now.result < snap.result) pushEvent(`${snap.result - now.result} 项医技结果已回报，可复核/结算`, 'ok')
    if (snap.pay && now.pay < snap.pay) pushEvent(`${snap.pay - now.pay} 笔账单已收款，流转至随访`, 'ok')
    if (snap.wait && now.wait > snap.wait) pushEvent(`新增 ${now.wait - snap.wait} 个今日挂号待接诊`, 'warn')
    if (now.future > snap.future && snap.future) pushEvent(`新增 ${now.future - snap.future} 个未来预约，可提前排班`, 'info')
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
