<template>
  <div class="console-root">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand">
          <span class="brand-mark">D</span>
          <span class="brand-name">医生操作台 <small>今日工作 · 一步到位</small></span>
        </div>
        <div class="clock">{{ timeText }} <span class="clock-hint">数据 {{ lastRefresh || '—' }} 更新</span></div>
        <ul class="nav">
          <li><a class="nav-link" @click="openPalette"><el-icon><Search/></el-icon> 搜索 <kbd>Ctrl/⌘ K</kbd></a></li>
          <li><router-link to="/doctorPage/d-flow"><el-icon><DataLine/></el-icon> 诊疗流全景</router-link></li>
          <li v-if="isManager"><router-link to="/backstage/m-flow"><el-icon><Setting/></el-icon> 后台管理</router-link></li>
          <li><router-link to="/backstage"><el-icon><User/></el-icon> 个人中心</router-link></li>
          <li><a class="nav-logout" @click="doLogout"><el-icon><SwitchButton/></el-icon> 退出</a></li>
        </ul>
      </div>
    </header>

    <!-- 加载失败：不伪装成「今天没病人」 -->
    <div v-if="loadError" class="load-error">
      <el-result icon="error" title="待办数据暂时无法加载"
                 sub-title="这不代表今天没有患者。请重试，或联系管理员检查服务与数据库。">
        <template #extra><el-button type="primary" @click="loadAll">重新加载</el-button></template>
      </el-result>
    </div>

    <main v-else class="console-body">
      <!-- 左：今日待办（点选即当前患者） -->
      <section class="col col-todo">
        <div class="col-head">
          <h2>今日待办</h2>
          <span class="todo-total">{{ totalPending }} 位</span>
          <el-button size="small" :loading="loading" circle @click="loadAll" title="刷新"><el-icon><Refresh/></el-icon></el-button>
        </div>
        <div class="todo-scroll">
          <div class="todo-collapse-ctrl">
            <a @click="collapseAll">全部收起</a><span class="cc-sep">·</span><a @click="expandAll">全部展开</a>
          </div>
          <div v-for="st in stages" :key="st.key" class="todo-group">
            <div class="todo-group-head" :class="{hot: st.items.length, open: !isCollapsed(st.key)}" @click="toggleGroup(st.key)">
              <el-icon class="chev"><ArrowRight/></el-icon>
              <span class="dot" :class="st.key"></span>{{ st.title }}
              <b>{{ st.items.length }}</b>
            </div>
            <div v-show="!isCollapsed(st.key)" class="todo-cards">
              <button v-for="c in st.items" :key="c.key" class="todo-card"
                      :class="{active: selected?.key === c.key}" @click="select(c, st)">
                <div class="tc-line"><b>{{ c.petName || '—' }}</b><span>{{ c.clientName || '' }}</span></div>
                <div class="tc-sub">{{ c.sub }}</div>
                <div class="tc-next">{{ c.nextLabel || '去处理' }} <el-icon><Right/></el-icon></div>
                <span v-if="c.flag" class="tc-flag">{{ c.flag }}</span>
              </button>
              <div v-if="!st.items.length" class="todo-empty">暂无</div>
            </div>
            <div v-if="isCollapsed(st.key) && st.items.length" class="todo-collapsed">已折叠 {{ st.items.length }} 位 · 点击展开</div>
          </div>
        </div>
      </section>

      <!-- 中：当前患者工作区 -->
      <section class="col col-patient">
        <div v-if="!selected" class="patient-empty">
          <el-icon class="pe-ico"><UserFilled/></el-icon>
          <h3>从左侧选一位患者开始</h3>
          <p>选中后，这里会显示患者信息与本环节该做的操作；<br/>每个按钮都自动带上当前患者，不用再搜索一遍。</p>
          <div class="pe-tip">也可以直接用右侧「常用功能」处理不针对某个患者的工作。</div>
        </div>
        <template v-else>
          <div class="patient-head">
            <div class="ph-name">
              <h2>{{ selected.petName || '—' }}</h2>
              <span>{{ selected.clientName || '' }}</span>
            </div>
            <div class="ph-stage">{{ selectedStage?.title }} · {{ selected.nextLabel || '待处理' }}</div>
          </div>
          <div class="patient-summary">
            <div class="ps-item"><span>当前情况</span><b>{{ selected.sub }}</b></div>
            <div v-if="detail" class="ps-item"><span>诊断</span><b>{{ detail.diagnosis || '未填写' }}</b></div>
            <div v-if="detail" class="ps-item"><span>主诉</span><b>{{ detail.chiefComplaint || '未填写' }}</b></div>
            <div v-if="detail" class="ps-item"><span>接诊医生</span><b>{{ detail.doctorName || '—' }}</b></div>
            <div v-if="idTags.length" class="ps-ids">
              <span v-for="t in idTags" :key="t">{{ t }}</span>
            </div>
          </div>

          <!-- 就诊档案预览：挂号后医生可快速了解该宠物的既往就诊概况 -->
          <div v-if="selected?.petId" class="archive-preview">
            <div class="ap-head" :class="{open: archiveOpen}" @click="archiveOpen = !archiveOpen">
              <el-icon class="chev"><ArrowRight/></el-icon>
              <span class="ap-tit">就诊档案预览</span>
              <span v-if="archive && archive.stat" class="ap-meta">
                累计 {{ archive.stat.visitCount }} 次 · 上次 {{ shortDate(archive.stat.lastVisit) }}
              </span>
              <el-button size="small" link type="primary" style="margin-left:auto" @click.stop="openArchive">完整档案</el-button>
            </div>
            <div v-show="archiveOpen" class="ap-body">
              <el-skeleton v-if="archiveLoading" :rows="3" animated/>
              <template v-else-if="archive">
                <div v-if="!archive.visits.length" class="ap-empty">该宠物暂无历史就诊记录（新患宠）</div>
                <div v-for="v in recentVisits" :key="v.recordId" class="ap-visit" @click="openRecord(v.recordId)">
                  <span class="ap-date">{{ shortDate(v.createTime) }}</span>
                  <span class="ap-diag">{{ v.diagnosis || '（未填诊断）' }}</span>
                  <el-icon class="ap-go"><Right/></el-icon>
                </div>
              </template>
              <div v-else class="ap-empty">档案加载失败</div>
            </div>
          </div>

          <div class="action-head">本环节常用操作 <small>针对当前患者，点击后在右侧抽屉打开</small></div>
          <div class="action-grid">
            <button v-for="a in currentActions" :key="a.path" class="action-btn" @click="openAction(a.path)">
              <el-icon class="ab-ico"><component :is="a.icon"/></el-icon>
              <span>{{ a.label }}</span>
            </button>
            <p v-if="!currentActions.length" class="action-empty">该环节暂无专属操作，请从右侧「常用功能」选择</p>
          </div>
        </template>
      </section>

      <!-- 右：常用功能宫格（固定位置，形成肌肉记忆） -->
      <aside class="col col-func">
        <div class="col-head"><h2>常用功能</h2><span class="func-hint">按岗位分组</span></div>
        <div class="func-scroll">
          <div v-for="g in visibleGroups" :key="g.title" class="func-group">
            <div class="fg-title">{{ g.title }}</div>
            <div class="fg-grid">
              <button v-for="f in g.items" :key="f.path" class="func-btn" @click="openAction(f.path)">
                <el-icon><component :is="f.icon"/></el-icon>
                <span>{{ f.label }}</span>
              </button>
            </div>
          </div>
        </div>
      </aside>
    </main>

    <!-- 流内抽屉：与 d-flow 同机制，按 __flowTarget 就地渲染业务页 -->
    <el-drawer v-model="inlineOpen" :title="inlineTitle" size="76%" direction="rtl" :before-close="closeInline">
      <router-view v-if="inlineOpen"/>
    </el-drawer>

    <!-- 命令面板降级为「熟练用户加速器」，不再是唯一入口 -->
    <el-dialog v-model="paletteOpen" width="560px" top="12vh" :show-close="false" @open="paletteKw = ''">
      <el-input v-model="paletteKw" placeholder="搜索患者 / 功能…" size="large" clearable autofocus>
        <template #prefix><el-icon><Search/></el-icon></template>
      </el-input>
      <div class="palette-list">
        <div v-for="p in paletteResults" :key="p.key" class="palette-item" @click="runPalette(p)">
          <span class="pi-label">{{ p.label }}</span><span class="pi-hint">{{ p.hint }}</span>
        </div>
        <div v-if="!paletteResults.length" class="palette-empty">无匹配</div>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {
  ArrowRight, Bell, Calendar, ChatDotRound, Collection, DataLine, Document, DocumentChecked, EditPen, Files,
  FirstAidKit, FolderOpened, House, Money, Phone, Picture, Printer, Refresh, Right, Search, Setting,
  SwitchButton, Tickets, User, UserFilled,
} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {reqDoctorPipeline, type FlowChip, type FlowStage} from '@/request/FlowApi'
import {reqRecordOne} from '@/request/RecordApi'
import {reqPetArchive} from '@/request/PetApi'
import {reqLogout} from '@/request/PowerApi'
import {reqGetLoginD} from '@/request/DoctorApi'
import {subscribeFlow} from '@/utils/chatSocket'
import {clearPwdCheckFlag} from '@/router'
import {canOpenAction} from '@/utils/PermissionUtil'

type Action = {label: string; path: string; icon: any}

const router = useRouter()
const route = useRoute()

// 管理者（MANAGER=6 / ADMIN=8）才在顶部看到「后台管理」互切入口；取不到权限时按普通医生处理
const isManager = ref(false)
const authLv = ref(0)
reqGetLoginD().then(d => {
  authLv.value = Number(d?.authLv ?? 0)
  isManager.value = authLv.value >= 6
}).catch(() => {})

// ── 数据 ──────────────────────────────────────────────────────────────
const loadError = ref(false)
const loading = ref(false)
const stages = ref<FlowStage[]>([])
const lastRefresh = ref('')
// 左列分组折叠：空分组默认折叠，减少视觉噪音；非空分组默认展开
const collapsed = ref<Set<string>>(new Set())
const isCollapsed = (key: string): boolean => collapsed.value.has(key)
const toggleGroup = (key: string): void => {
  const s = new Set(collapsed.value)
  s.has(key) ? s.delete(key) : s.add(key)
  collapsed.value = s
}
const collapseAll = (): void => { collapsed.value = new Set(stages.value.map(s => s.key)) }
const expandAll = (): void => { collapsed.value = new Set() }
const selected = ref<FlowChip | null>(null)
const selectedStage = ref<FlowStage | null>(null)
const detail = ref<any>(null)

const totalPending = computed(() => stages.value.reduce((n, s) => n + s.items.length, 0))
const pad = (n: number) => n.toString().padStart(2, '0')
const timeText = ref('--:--:--')
const tick = () => { const t = new Date(); timeText.value = `${pad(t.getHours())}:${pad(t.getMinutes())}:${pad(t.getSeconds())}` }
let clockTimer: ReturnType<typeof setInterval> | undefined

const loadAll = async (): Promise<void> => {
  loading.value = true
  try {
    const vo = await reqDoctorPipeline()
    loadError.value = false
    stages.value = vo.stages || []
    // 空分组默认折叠（不清掉用户手动展开的非空分组）
    const next = new Set<string>()
    for (const s of stages.value) if (!s.items.length) next.add(s.key)
    for (const k of collapsed.value) if (stages.value.some(s => s.key === k)) next.add(k)
    collapsed.value = next
    lastRefresh.value = `${pad(new Date().getHours())}:${pad(new Date().getMinutes())}:${pad(new Date().getSeconds())}`
    // 待办刷新后保持当前患者选中；若该患者已流转走则自动取消选中
    if (selected.value) {
      const still = stages.value.flatMap(s => s.items).find(c => c.key === selected.value!.key)
      if (!still) { selected.value = null; selectedStage.value = null; detail.value = null }
    }
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

// ── 患者上下文 ────────────────────────────────────────────────────────
/** 诊疗流卡片把患者名与单号放在 actionPath 的 query 里，这里解析出来供所有动作复用 */
const currentQuery = computed<Record<string, string>>(() => {
  if (!selected.value?.actionPath) return {}
  const p = selected.value.actionPath
  const i = p.indexOf('?')
  const out: Record<string, string> = {}
  if (i >= 0) new URLSearchParams(p.slice(i + 1)).forEach((v, k) => { out[k] = v })
  return out
})
const idTags = computed<string[]>(() => {
  const pairs: [string, string][] = [
    ['appointmentId', '挂号'], ['recordId', '病历'], ['orderId', '申请单'],
    ['billId', '账单'], ['followId', '随访'],
  ]
  const q = currentQuery.value
  return pairs.filter(([k]) => q[k]).map(([k, l]) => `${l} #${q[k]}`)
})

const select = (c: FlowChip, st: FlowStage): void => {
  selected.value = c
  selectedStage.value = st
  detail.value = null
  archive.value = null
  const rid = Number(currentQuery.value.recordId)
  if (rid) reqRecordOne(rid).then(vo => { detail.value = vo?.record || null }).catch(() => { detail.value = null })
  // 拉取该宠物既往就诊档案（挂号后也能看到历史概况）
  if (c.petId) loadArchive(c.petId)
  else archive.value = null
}

// ── 就诊档案预览 ─────────────────────────────────────────────────────
const archive = ref<any>(null)
const archiveLoading = ref(false)
const archiveOpen = ref(true)
const recentVisits = computed(() => (archive.value?.visits || []).slice(0, 5))
const loadArchive = (petId: number): void => {
  archiveLoading.value = true
  reqPetArchive(petId).then(res => { archive.value = res || null })
    .catch(() => { archive.value = null })
    .finally(() => { archiveLoading.value = false })
}
const shortDate = (s: string | null): string => {
  if (!s) return '—'
  return s.length >= 10 ? s.slice(0, 10) : s
}
// 完整档案在右侧抽屉打开（与 d-flow 同机制）
const openArchive = (): void => {
  if (selected.value?.petId) openAction('/backstage/z-petArchive?petId=' + selected.value.petId)
}
// 点击某次就诊：跳转到病历详情页（与就诊档案里「病历」按钮同一个 d-recordPrint），在抽屉内打开
const openRecord = (recordId: number): void => {
  openAction('/doctorPage/d-recordPrint?recordId=' + recordId)
}

// 从个人中心「今日概览」点环节卡进来：自动选中该环节第一位患者，少点一次
let stageQueryApplied = false
const applyStageQuery = (): void => {
  if (stageQueryApplied || selected.value) return
  const key = String(route.query.stage || '')
  if (!key) return
  stageQueryApplied = true
  const st = stages.value.find(s => s.key === key)
  if (st?.items.length) select(st.items[0], st)
}

// ── 动作定义（每个都自动带当前患者 query）────────────────────────────
const stageActions: Record<string, Action[]> = {
  wait: [
    {label: '开始接诊', path: '/doctorPage/d-appoint', icon: EditPen},
  ],
  treat: [
    {label: '继续接诊 / 写病历', path: '/doctorPage/d-appoint', icon: EditPen},
    {label: '开化验单', path: '/doctorPage/d-lab', icon: Document},
    {label: '开影像单', path: '/doctorPage/d-image', icon: Picture},
    {label: '开处方', path: '/doctorPage/d-rx', icon: Files},
    {label: '病历打印', path: '/doctorPage/d-recordPrint', icon: Printer},
  ],
  result: [
    {label: '录入医技结果', path: '/doctorPage/z-examResult', icon: DocumentChecked},
  ],
  pay: [
    {label: '去收银结算', path: '/doctorPage/y-billList', icon: Money},
  ],
  follow: [
    {label: '写随访记录', path: '/doctorPage/z-follow', icon: Phone},
  ],
}
const commonActions: Action[] = [
  {label: '病例中心', path: '/doctorPage/d-record', icon: FolderOpened},
  {label: '医技结果', path: '/doctorPage/z-examResult', icon: DocumentChecked},
  {label: '收银台', path: '/doctorPage/y-billList', icon: Money},
  {label: '挂号列表', path: '/doctorPage/y-appointmentList', icon: Calendar},
]
const currentActions = computed<Action[]>(() => {
  const key = selectedStage.value?.key || ''
  return (stageActions[key] || []).filter(a => canOpenAction(a.path, authLv.value))
})

const funcGroups: {title: string; items: Action[]}[] = [
  {title: '接诊与开单', items: [
    {label: '挂号 / 接诊队列', path: '/doctorPage/y-appointmentList', icon: Calendar},
    {label: '接诊工作台', path: '/doctorPage/d-appoint', icon: EditPen},
    {label: '病例中心', path: '/doctorPage/d-record', icon: FolderOpened},
    {label: '在线问诊', path: '/doctorPage/d-chat', icon: ChatDotRound},
  ]},
  {title: '结果与报告', items: [
    {label: '医技结果录入', path: '/doctorPage/z-examResult', icon: DocumentChecked},
    {label: '报告单打印', path: '/doctorPage/z-examReport', icon: Printer},
    {label: '申请单打印', path: '/doctorPage/z-requisition', icon: Tickets},
  ]},
  {title: '收费与随访', items: [
    {label: '收银台', path: '/doctorPage/y-billList', icon: Money},
    {label: '随访记录', path: '/doctorPage/z-follow', icon: Phone},
    {label: '复诊/疫苗提醒', path: '/doctorPage/z-remind', icon: Bell},
    {label: '免疫接种', path: '/doctorPage/z-immune', icon: FirstAidKit},
  ]},
  {title: '住院与配置', items: [
    {label: '住院登记', path: '/doctorPage/z-hospital', icon: House},
    {label: '手术记录', path: '/doctorPage/z-surgery', icon: Document},
    {label: '诊断字典', path: '/doctorPage/z-diagnosis', icon: Collection},
    {label: '参考区间', path: '/doctorPage/z-refRange', icon: DataLine},
  ]},
]

// 管理账号(>=6)只做管理：隐藏临床作业入口；医护(<4)隐藏需 DOCTOR 资格的开单/开方
const visibleGroups = computed(() => funcGroups
  .map(g => ({...g, items: g.items.filter(i => canOpenAction(i.path, authLv.value))}))
  .filter(g => g.items.length > 0))

// ── 抽屉（与 d-flow 同机制，守卫会把深页跳转重写回 d-console/inline）──
const inlineOpen = ref(false)
const inlineTitle = ref('处理')
const actionTitles: Record<string, string> = {
  '/doctorPage/d-appoint': '接诊 / 写病历', '/doctorPage/d-lab': '检验开单',
  '/doctorPage/d-image': '影像开单', '/doctorPage/d-rx': '处方',
  '/doctorPage/d-recordPrint': '病历打印', '/doctorPage/d-record': '病例中心',
  '/doctorPage/d-chat': '在线问诊', '/doctorPage/z-examResult': '医技结果',
  '/doctorPage/y-billList': '收银', '/doctorPage/y-appointmentList': '挂号列表',
  '/doctorPage/z-follow': '随访', '/doctorPage/z-remind': '复诊/疫苗提醒',
  '/doctorPage/z-immune': '免疫接种', '/doctorPage/z-hospital': '住院登记',
  '/doctorPage/z-surgery': '手术记录', '/doctorPage/z-examReport': '报告单打印',
  '/doctorPage/z-requisition': '申请单打印',
  '/doctorPage/z-diagnosis': '诊断字典', '/doctorPage/z-refRange': '参考区间',
  '/backstage/z-petArchive': '就诊档案',
}
const titleOf = (p: string): string => {
  const base = p.split('?')[0]
  return actionTitles[base] || base.split('/').pop() || '业务页'
}
const openAction = (path: string): void => {
  if (!path) return
  // 把路径自带的 query 拆出来：__flowTarget 只留裸路径（否则 FlowInline 按整串查白名单会失配），
  // 参数平铺为路由 query，被渲染的深页用 useRoute().query 正常读取（如 petId / recordId）
  const qi = path.indexOf('?')
  const base = qi >= 0 ? path.slice(0, qi) : path
  const extra: Record<string, any> = {}
  if (qi >= 0) new URLSearchParams(path.slice(qi + 1)).forEach((v, k) => { extra[k] = v })
  const q: Record<string, any> = {...currentQuery.value, ...extra, __flowTarget: base}
  inlineTitle.value = '处理 · ' + titleOf(base)
  inlineOpen.value = true
  router.push({path: '/doctorPage/d-console/inline', query: q})
}
const closeInline = (done: () => void): void => {
  inlineOpen.value = false
  router.push('/doctorPage/d-console').then(() => { loadAll(); done() })
}
watch(() => route.path, (p: string): void => {
  if (!p.startsWith('/doctorPage/d-console/')) { inlineOpen.value = false; return }
  inlineOpen.value = true
  const t = String(route.query.__flowTarget || '')
  inlineTitle.value = '处理 · ' + (t ? titleOf(t) : '业务页')
}, {immediate: true})

// ── 命令面板：仅作为加速器 ───────────────────────────────────────────
const paletteOpen = ref(false)
const paletteKw = ref('')
const openPalette = () => { paletteOpen.value = true }
type PaletteHit = {key: string; label: string; hint: string; path?: string; chip?: FlowChip; stage?: FlowStage}
const allCommands = computed<PaletteHit[]>(() => {
  const list: PaletteHit[] = []
  for (const g of visibleGroups.value) for (const f of g.items)
    list.push({key: 'f' + f.path, label: f.label, hint: g.title, path: f.path})
  for (const a of commonActions) if (canOpenAction(a.path, authLv.value))
    list.push({key: 'c' + a.path, label: a.label, hint: '通用操作', path: a.path})
  return list
})
const paletteResults = computed<PaletteHit[]>(() => {
  const kw = paletteKw.value.trim().toLowerCase()
  const patients: PaletteHit[] = stages.value.flatMap(st => st.items.map(c => ({
    key: 'p' + c.key, label: `${c.petName || ''} · ${c.clientName || ''}`,
    hint: `${st.title} → ${c.nextLabel || '去处理'}`, chip: c, stage: st,
  })))
  const hits = [...patients, ...allCommands.value]
  const filtered = kw ? hits.filter(h => (h.label + h.hint).toLowerCase().includes(kw)) : hits.slice(0, 20)
  return filtered.slice(0, 20)
})
const runPalette = (h: PaletteHit): void => {
  paletteOpen.value = false
  if (h.chip && h.stage) { select(h.chip, h.stage); return }
  if (h.path) openAction(h.path)
}
const onHotkey = (e: KeyboardEvent): void => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); paletteOpen.value = !paletteOpen.value }
}

// ── 登出 / 实时联动 ──────────────────────────────────────────────────
const doLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '退出登录', {confirmButtonText: '确定退出', cancelButtonText: '取消', type: 'warning'})
    .then(() => { reqLogout().then(res => { clearPwdCheckFlag(); if (res.code === 1) router.push('/login') }) })
    .catch(() => ElMessage.info('已取消'))
}
const onRefresh = () => loadAll()
let unsub: (() => void) | null = null
let pollTimer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  tick(); clockTimer = setInterval(tick, 1000)
  loadAll().then(applyStageQuery)
  pollTimer = setInterval(loadAll, 20000)
  unsub = subscribeFlow(() => loadAll())
  window.addEventListener('mixpet:flow-refresh', onRefresh as EventListener)
  window.addEventListener('keydown', onHotkey)
})
onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (pollTimer) clearInterval(pollTimer)
  unsub?.(); unsub = null
  window.removeEventListener('mixpet:flow-refresh', onRefresh as EventListener)
  window.removeEventListener('keydown', onHotkey)
})
</script>

<style lang="scss" scoped>
.console-root { min-height: 100vh; background: #f3f6fb; color: #1F2D3D; }
.topbar { background: #1f2d3d; color: #fff; }
.topbar-inner { max-width: 1680px; margin: 0 auto; height: 58px; display: flex; align-items: center; gap: 20px; padding: 0 20px; }
.brand { display: flex; align-items: center; gap: 10px; min-width: 260px; }
.brand-mark { width: 32px; height: 32px; border-radius: 8px; background: #ffd04b; color: #1f2d3d; font-weight: 800; display: flex; align-items: center; justify-content: center; }
.brand-name { font-weight: 700; font-size: 16px; }
.brand-name small { display: block; font-weight: 400; font-size: 11px; opacity: .65; }
.clock { flex: 1; font-variant-numeric: tabular-nums; font-size: 14px; }
.clock-hint { font-size: 12px; opacity: .6; margin-left: 10px; }
.nav { display: flex; align-items: center; gap: 6px; list-style: none; margin: 0; padding: 0; }
.nav a, .nav :deep(a) { color: #dbe4f0; text-decoration: none; font-size: 13px; padding: 6px 10px; border-radius: 6px; cursor: pointer; display: flex; align-items: center; gap: 5px; white-space: nowrap; }
.nav a:hover, .nav :deep(a:hover) { background: rgba(255,255,255,.12); color: #ffd04b; }
.nav .nav-logout:hover { color: #ff9b9b; }
kbd { font-size: 10px; border: 1px solid rgba(255,255,255,.35); border-radius: 4px; padding: 1px 4px; opacity: .8; }
.load-error { max-width: 900px; margin: 40px auto; background: #fff; border-radius: 10px; }
.console-body { max-width: 1680px; margin: 0 auto; padding: 16px 20px; display: grid; grid-template-columns: 300px minmax(360px, 1fr) 320px; gap: 14px; align-items: start; }
.col { background: #fff; border: 1px solid #e4e9f2; border-radius: 10px; max-height: calc(100vh - 92px); display: flex; flex-direction: column; overflow: hidden; }
.col-head { display: flex; align-items: center; gap: 8px; padding: 14px 16px; border-bottom: 1px solid #eef1f6; }
.col-head h2 { font-size: 15px; margin: 0; flex: 1; }
.todo-total { font-size: 12px; color: #fff; background: #409eff; border-radius: 10px; padding: 2px 9px; }
.func-hint { font-size: 12px; color: #99a3b3; }
.todo-scroll, .func-scroll { overflow: auto; padding: 8px 12px 14px; }
.todo-group { margin-bottom: 12px; }
.todo-group-head { display: flex; align-items: center; gap: 7px; font-size: 12px; color: #8a94a6; padding: 6px 4px; }
.todo-group-head.hot { color: #1F2D3D; font-weight: 600; }
.todo-group-head b { margin-left: auto; background: #eef2f8; border-radius: 9px; padding: 1px 8px; font-size: 11px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #c8d2e2; }
.dot.wait { background: #409eff; } .dot.treat { background: #e6a23c; } .dot.result { background: #909399; }
.dot.pay { background: #67c23a; } .dot.follow { background: #b37feb; }
.todo-card { width: 100%; text-align: left; border: 1px solid #e8edf5; border-radius: 8px; background: #fff; padding: 9px 11px; margin-bottom: 7px; cursor: pointer; position: relative; transition: .15s; }
.todo-card:hover { border-color: #9ec5ff; box-shadow: 0 2px 8px rgba(64,158,255,.12); }
.todo-card.active { border-color: #409eff; background: #f2f8ff; box-shadow: 0 0 0 2px rgba(64,158,255,.15); }
.tc-line { display: flex; justify-content: space-between; align-items: baseline; gap: 8px; }
.tc-line b { font-size: 14px; }
.tc-line span { font-size: 12px; color: #7d8899; }
.tc-sub { font-size: 12px; color: #6b778c; margin-top: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tc-next { font-size: 12px; color: #409eff; margin-top: 5px; display: flex; align-items: center; gap: 3px; }
.tc-flag { position: absolute; right: 9px; top: 9px; font-size: 10px; color: #fff; background: #f56c6c; border-radius: 8px; padding: 1px 6px; }
.todo-empty { font-size: 12px; color: #b3bccb; padding: 2px 6px 6px; }
.todo-collapse-ctrl { display: flex; align-items: center; gap: 6px; padding: 2px 4px 8px; font-size: 12px; }
.todo-collapse-ctrl a { color: #6f9be0; cursor: pointer; }
.todo-collapse-ctrl a:hover { color: #409eff; text-decoration: underline; }
.todo-collapse-ctrl .cc-sep { color: #c8d2e2; }
.todo-group-head { cursor: pointer; user-select: none; }
.todo-group-head .chev { transition: transform .15s; font-size: 12px; color: #b3bccb; }
.todo-group-head.open .chev { transform: rotate(90deg); color: #8a94a6; }
.todo-collapsed { font-size: 12px; color: #aab3c2; padding: 4px 8px 8px; cursor: pointer; }
.archive-preview { margin: 0 20px 10px; border: 1px solid #e4ecf7; border-radius: 10px; background: #fbfdff; overflow: hidden; }
.ap-head { display: flex; align-items: center; gap: 7px; padding: 11px 14px; cursor: pointer; user-select: none; background: #f3f8ff; border-bottom: 1px solid #eaf1fb; }
.ap-head .chev { transition: transform .15s; font-size: 12px; color: #8aa3c8; }
.ap-head .chev { transform: rotate(90deg); }
.ap-head:not(.open) .chev { transform: rotate(0deg); }
.ap-tit { font-size: 13px; font-weight: 600; color: #3f4d63; }
.ap-meta { font-size: 12px; color: #7d8899; }
.ap-body { padding: 10px 14px 14px; }
.ap-empty { font-size: 12px; color: #aab3c2; padding: 4px 0; }
.ap-visit { display: flex; align-items: center; gap: 10px; padding: 7px 8px; border-radius: 7px; cursor: pointer; }
.ap-visit:hover { background: #eef6ff; }
.ap-date { font-size: 12px; color: #6b778c; width: 92px; flex: none; font-variant-numeric: tabular-nums; }
.ap-diag { flex: 1; font-size: 13px; color: #2f3d54; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ap-go { color: #c0c8d6; font-size: 13px; }
.action-empty { font-size: 12px; color: #aab3c2; padding: 4px 2px 14px; }
.patient-empty { margin: auto; text-align: center; padding: 60px 24px; color: #8a94a6; }
.pe-ico { font-size: 48px; color: #c9d6ea; }
.patient-empty h3 { color: #4a5670; font-size: 16px; margin: 14px 0 8px; }
.patient-empty p { font-size: 13px; line-height: 1.8; margin: 0; }
.pe-tip { margin-top: 18px; font-size: 12px; background: #f6f9ff; border: 1px dashed #cfe0ff; border-radius: 8px; padding: 10px; }
.col-patient { padding: 0; overflow: auto; }
.patient-head { padding: 18px 20px 14px; border-bottom: 1px solid #eef1f6; background: linear-gradient(180deg,#f7fbff,#fff); }
.ph-name { display: flex; align-items: baseline; gap: 12px; }
.ph-name h2 { margin: 0; font-size: 22px; }
.ph-name span { color: #6b778c; font-size: 14px; }
.ph-stage { display: inline-block; margin-top: 8px; font-size: 12px; color: #2f5597; background: #e8f0ff; border-radius: 10px; padding: 3px 10px; }
.patient-summary { padding: 14px 20px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px 18px; border-bottom: 1px solid #eef1f6; }
.ps-item { min-width: 0; }
.ps-item span { display: block; font-size: 11px; color: #9aa5b5; margin-bottom: 2px; }
.ps-item b { font-size: 13px; font-weight: 500; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ps-ids { grid-column: 1 / -1; display: flex; gap: 6px; flex-wrap: wrap; }
.ps-ids span { font-size: 11px; background: #eef4ff; color: #2f5597; border-radius: 10px; padding: 2px 9px; }
.action-head { padding: 14px 20px 8px; font-size: 13px; font-weight: 600; color: #3f4d63; }
.action-head small { font-weight: 400; color: #9aa5b5; margin-left: 8px; font-size: 11px; }
.action-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; padding: 0 20px 8px; }
.action-grid.compact { padding-bottom: 20px; }
.action-btn { border: 1px solid #dce6f5; background: #fbfdff; border-radius: 9px; min-height: 74px; cursor: pointer; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7px; font-size: 13px; color: #2f3d54; transition: .15s; }
.action-btn:hover { border-color: #409eff; background: #eef6ff; color: #1f5fbf; transform: translateY(-1px); }
.ab-ico { font-size: 22px; color: #409eff; }
.func-group { margin-bottom: 16px; }
.fg-title { font-size: 12px; color: #8a94a6; padding: 6px 4px 8px; border-bottom: 1px solid #eef1f6; margin-bottom: 9px; }
.fg-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.func-btn { border: 1px solid #e8edf5; background: #fff; border-radius: 8px; min-height: 56px; cursor: pointer; display: flex; align-items: center; gap: 8px; padding: 8px 10px; font-size: 12px; color: #3f4d63; text-align: left; }
.func-btn:hover { border-color: #409eff; background: #f2f8ff; color: #1f5fbf; }
.func-btn .el-icon { font-size: 17px; color: #6f9be0; flex: none; }
.palette-list { max-height: 420px; overflow: auto; margin-top: 10px; }
.palette-item { display: flex; justify-content: space-between; align-items: center; padding: 9px 12px; border-radius: 7px; cursor: pointer; font-size: 13px; }
.palette-item:hover { background: #f2f8ff; }
.pi-hint { font-size: 11px; color: #9aa5b5; }
.palette-empty { text-align: center; color: #b3bccb; padding: 24px; font-size: 13px; }
@media (max-width: 1280px) {
  .console-body { grid-template-columns: 260px minmax(320px, 1fr); }
  .col-func { display: none; }
}
@media (max-width: 900px) {
  .console-body { grid-template-columns: 1fr; }
  .col { max-height: none; }
  .todo-scroll { max-height: 280px; }
  .action-grid { grid-template-columns: repeat(2, 1fr); }
  .nav a kbd { display: none; }
}
</style>
