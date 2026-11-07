<template>
  <div class="flow-root">
    <!-- 顶栏：保持与工作台一致，但去掉卡片墙 -->
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand">
          <span class="brand-mark">
            <img v-if="hosp.logo" :src="'/api/common/download?name=' + hosp.logo" alt="logo"/>
            <template v-else>{{ hosp.name ? hosp.name.charAt(0) : 'DR' }}</template>
          </span>
          <span class="brand-name">诊疗流 · {{ hosp.name || 'mixpet' }}</span>
        </div>
        <div class="flow-clock">
          <span class="dot"></span>{{ timeText }}
          <span class="hint">全流程实时联动 · 自动流转</span>
        </div>
        <ul class="nav">
          <li><a class="nav-link" @click="paletteOpen = true"><el-icon><Search/></el-icon> 命令面板 <kbd>⌘K</kbd></a></li>
          <li><router-link to="/doctorPage/d-home"><el-icon><User/></el-icon> 经典视图</router-link></li>
          <li><router-link to="/backstage"><el-icon><User/></el-icon> 个人中心</router-link></li>
          <li><a class="nav-logout" title="退出登录" @click="doLogout"><el-icon><SwitchButton/></el-icon> 退出</a></li>
        </ul>
      </div>
    </header>

    <main class="flow-body">
      <!-- 流水线：五个环节首尾相接，环节之间用箭头连成一条河 -->
      <div class="pipeline">
        <section v-for="(st, i) in stages" :key="st.key" class="lane" :class="{hot: st.items.length}">
          <div class="lane-head">
            <span class="lane-no">{{ i + 1 }}</span>
            <div class="lane-tit">
              <h3>{{ st.title }}</h3>
              <p>{{ st.desc }}</p>
            </div>
            <span class="lane-count" :class="{on: st.items.length}">{{ st.items.length }}</span>
          </div>
          <TransitionGroup name="chip" tag="div" class="lane-list">
            <button v-for="c in st.items" :key="c.key" class="chip" :class="c.cls" @click="openChip(c)">
              <span class="chip-pet">{{ c.petName }}</span>
              <span class="chip-sub">{{ c.sub }}</span>
              <span v-if="c.flag" class="chip-flag">{{ c.flag }}</span>
            </button>
          </TransitionGroup>
          <div v-if="!st.items.length" class="lane-empty">— 空 —</div>
          <div class="lane-foot">
            <router-link :to="st.actionPath" class="lane-act">{{ st.actionLabel }}</router-link>
          </div>
          <div v-if="i < stages.length - 1" class="flow-arrow"><el-icon><Right/></el-icon></div>
        </section>
      </div>

      <!-- 实时事件流：系统自己把"下一步"递到你手边 -->
      <aside class="live-rail">
        <div class="rail-head">
          <span class="pulse"></span> 实时动态
          <span class="rail-time">刷新 {{ lastRefresh }}</span>
        </div>
        <TransitionGroup name="evt" tag="div" class="rail-list">
          <div v-for="e in events" :key="e.id" class="evt" :class="e.cls">
            <span class="evt-ico"><el-icon><component :is="e.icon"/></el-icon></span>
            <div class="evt-main">
              <div class="evt-txt">{{ e.text }}</div>
              <div class="evt-time">{{ e.time }}</div>
            </div>
          </div>
        </TransitionGroup>
        <div v-if="!events.length" class="rail-empty">静默中 · 有变化会即时弹出</div>
      </aside>
    </main>

    <!-- 患者时间线抽屉：点开即见该患者全旅程，并给出下一个动作 -->
    <el-drawer v-model="drawer" :title="active ? active.petName + ' · 诊疗旅程' : ''" size="420px" direction="rtl">
      <div v-if="active" class="drawer">
      <div class="dr-header">
        <div><b>{{ active.petName || '患者' }}</b> <span class="dr-sub">{{ active.clientName || '' }}</span></div>
          <div class="dr-meta">{{ active.sub }}</div>
        </div>
        <div class="dr-stage">
          当前环节：<b>{{ active.stageTitle }}</b>
        </div>
        <p class="dr-next">下一步 · <b>{{ active.nextLabel }}</b></p>
        <router-link :to="active.actionPath" class="dr-go" @click="drawer = false">{{ active.nextLabel }}</router-link>
        <div class="dr-tip">进入对应工作台完成该步骤，完成后本流水线会自动把它推向下一环节。</div>
      </div>
    </el-drawer>

    <!-- 命令面板：灵动入口，键盘直达任意环节/患者 -->
    <el-dialog v-model="paletteOpen" width="560px" top="12vh" :show-close="false" @open="paletteKw = ''">
      <div class="palette">
        <el-input v-model="paletteKw" placeholder="搜索患者 / 环节 / 动作…" size="large" clearable autofocus>
          <template #prefix><el-icon><Search/></el-icon></template>
        </el-input>
        <div class="palette-list">
          <div v-for="p in paletteFiltered" :key="p.key" class="palette-item" @click="runPalette(p)">
            <span class="pi-ico"><el-icon><component :is="p.icon"/></el-icon></span>
            <span class="pi-label">{{ p.label }}</span>
            <span class="pi-path">{{ p.hint }}</span>
          </div>
          <div v-if="!paletteFiltered.length" class="palette-empty">无匹配</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, ref} from "vue"
import {Right, Search, SwitchButton, User} from "@element-plus/icons-vue"
import {useRouter} from "vue-router"
import {ElMessage, ElMessageBox} from "element-plus"
import {reqDoctorAppoint} from "@/request/DoctorApi"
import {reqRecordPage} from "@/request/RecordApi"
import {reqLabOrderPage} from "@/request/LabApi"
import {reqImageOrderPage} from "@/request/ImageApi"
import {reqBillPage} from "@/request/BillApi"
import {reqFollowPage} from "@/request/FollowApi"
import {reqDoctorMsgGroup} from "@/request/MsgApi"
import {reqLogout} from "@/request/PowerApi"
import {hospital} from "@/utils/hospital"
import {subscribe} from "@/utils/chatSocket"
import {clearPwdCheckFlag} from "@/router"
import type {AppointDto2} from "@/model/DO/AppointDto2"
import type {RecordDto} from "@/model/DO/RecordDto"
import type {LabOrderDto} from "@/model/DO/LabOrderDto"
import type {ImageOrder} from "@/model/entiy/ImageOrder"
import type {BillDto} from "@/model/DO/BillDto"
import type {FollowUpDto} from "@/model/entiy/FollowUp"

const router = useRouter()
const hosp = hospital

// ---------- 时间 ----------
const timeText = ref('--:--:--')
const pad = (n: number) => n.toString().padStart(2, '0')
const tick = () => { const t = new Date(); timeText.value = `${pad(t.getHours())}:${pad(t.getMinutes())}:${pad(t.getSeconds())}` }
let clockTimer: ReturnType<typeof setInterval> | undefined
let pollTimer: ReturnType<typeof setInterval> | undefined

// ---------- 流水线数据 ----------
interface Chip {
  key: string; petName: string | null | undefined; sub: string; flag?: string
  cls?: string; stageTitle: string; nextLabel: string; actionPath: string; clientName?: string | null
}
const stages = ref([
  {key: 'wait', title: '待诊', desc: '今日已挂号·未接诊', actionPath: '/doctorPage/d-appoint', actionLabel: '去接诊', items: [] as Chip[]},
  {key: 'treat', title: '接诊中', desc: '已开诊·未完结', actionPath: '/doctorPage/d-appoint', actionLabel: '继续接诊', items: [] as Chip[]},
  {key: 'result', title: '待结果', desc: '已开单·待回报', actionPath: '/doctorPage/z-examResult', actionLabel: '去录入', items: [] as Chip[]},
  {key: 'pay', title: '待结算', desc: '已完结·未收款', actionPath: '/doctorPage/y-billList?payStatus=0', actionLabel: '去收银', items: [] as Chip[]},
  {key: 'follow', title: '待随访', desc: '已结算·待回访', actionPath: '/doctorPage/z-follow', actionLabel: '去随访', items: [] as Chip[]},
])

const lastRefresh = ref('')
const events = ref<{id: number; text: string; time: string; cls: string; icon: any}[]>([])
let evtId = 0
const pushEvent = (text: string, cls = 'info', icon: any = Right) => {
  const t = new Date()
  events.value.unshift({id: ++evtId, text, time: `${pad(t.getHours())}:${pad(t.getMinutes())}:${pad(t.getSeconds())}`, cls, icon})
  if (events.value.length > 30) events.value.pop()
}

// 快照，用于 diff 出"刚发生的变化"→ 灵动提示
let snap = {wait: 0, treat: 0, result: 0, pay: 0, follow: 0, msgs: 0}

const isToday = (d: string | null): boolean => {
  if (!d) return false
  const t = new Date()
  const today = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`
  return d.slice(0, 10) === today
}

const loadAll = async (): Promise<void> => {
  try {
    const [apps, recs, labs, imgs, bills, follows, groups] = await Promise.all([
      reqDoctorAppoint(),
      reqRecordPage({status: 0, numPage: 1, pageSize: 50, petName: '', clientName: '', doctorName: '', diagnosis: '', startTime: '', endTime: ''}),
      reqLabOrderPage({numPage: 1, pageSize: 50, status: 0}),
      reqImageOrderPage({numPage: 1, pageSize: 50, status: false}),
      reqBillPage({numPage: 1, pageSize: 50, payStatus: 0}),
      reqFollowPage({numPage: 1, pageSize: 50, status: false}),
      reqDoctorMsgGroup(0),
    ])
    const a = (apps || []) as AppointDto2[]
    const r = (recs?.records || []) as RecordDto[]
    const l = (labs?.records || []) as LabOrderDto[]
    const im = (imgs?.records || []) as ImageOrder[]
    const b = (bills?.records || []) as BillDto[]
    const f = (follows?.records || []) as FollowUpDto[]
    const g = (groups || []) as any[]

    stages.value[0].items = a.filter(x => isToday(x.appointmentDate) && !x.isSuc && !x.miss)
      .map(x => ({key: 'a' + x.appointmentId, petName: x.petName, sub: x.clientName + ' · ' + (x.appointmentDate || '').slice(11, 16),
        stageTitle: '待诊', nextLabel: '接诊', actionPath: `/doctorPage/d-appoint?appointmentId=${x.appointmentId}`, clientName: x.clientName}))
    stages.value[1].items = r.map(x => ({key: 'r' + x.recordId, petName: x.petName, sub: x.clientName + ' · ' + (x.diagnosis || '接诊中'),
      stageTitle: '接诊中', nextLabel: '继续接诊', actionPath: `/doctorPage/d-appoint?appointmentId=${x.appointmentId ?? ''}`, clientName: x.clientName}))
    const labChips: Chip[] = l.map(x => ({key: 'l' + x.orderId, petName: x.petName || '化验', sub: '化验 · 待回报', flag: x.criticalFlag ? '危急' : '', cls: x.criticalFlag ? 'danger' : '',
      stageTitle: '待结果', nextLabel: '录入化验结果', actionPath: '/doctorPage/z-examResult?tab=lab', clientName: x.clientName}))
    const imgChips: Chip[] = im.map(x => ({key: 'i' + x.orderId, petName: x.petName || x.itemName, sub: x.itemName + ' · 待报告',
      stageTitle: '待结果', nextLabel: '录入影像报告', actionPath: '/doctorPage/z-examResult?tab=image', clientName: x.clientName}))
    stages.value[2].items = [...labChips, ...imgChips]
    stages.value[3].items = b.map(x => ({key: 'b' + x.billId, petName: x.petName, sub: x.clientName + ' · ￥' + (x.realMoney ?? x.totalMoney).toFixed(2),
      stageTitle: '待结算', nextLabel: '收款结算', actionPath: '/doctorPage/y-billList?payStatus=0', clientName: x.clientName}))
    stages.value[4].items = f.map(x => ({key: 'f' + x.followId, petName: x.petName || '', sub: x.clientName + ' · 约' + (x.planTime || '').slice(0, 10),
      stageTitle: '待随访', nextLabel: '回访', actionPath: '/doctorPage/z-follow', clientName: x.clientName}))

    // diff：刚进入/离开某环节，给出灵动提示
    const now = {wait: stages.value[0].items.length, treat: stages.value[1].items.length, result: stages.value[2].items.length,
      pay: stages.value[3].items.length, follow: stages.value[4].items.length, msgs: g.reduce((s: number, x: any) => s + (x.unread || 0), 0)}
    if (snap.result && now.result < snap.result) pushEvent(`${snap.result - now.result} 项医技结果已回报，可复核/结算`, 'ok', Right)
    if (snap.pay && now.pay < snap.pay) pushEvent(`${snap.pay - now.pay} 笔账单已收款，流转至随访`, 'ok', Right)
    if (snap.wait && now.wait > snap.wait) pushEvent(`新增 ${now.wait - snap.wait} 个今日挂号待接诊`, 'warn', Right)
    snap = now
    lastRefresh.value = `${pad(new Date().getHours())}:${pad(new Date().getMinutes())}:${pad(new Date().getSeconds())}`
  } catch (e) {
    pushEvent('数据加载失败，稍后自动重试', 'err', Right)
  }
}

// ---------- 抽屉 ----------
const drawer = ref(false)
const active = ref<Chip | null>(null)
const openChip = (c: Chip) => { active.value = c; drawer.value = true }

// ---------- 命令面板 ----------
const paletteOpen = ref(false)
const paletteKw = ref('')
interface Cmd { key: string; label: string; hint: string; icon: any; path?: string; run?: () => void }
const paletteBase: Cmd[] = [
  {key: 'p-wait', label: '接诊队列', hint: '待诊 → 去接诊', icon: User, path: '/doctorPage/d-appoint'},
  {key: 'p-result', label: '医技结果录入', hint: '化验 / 影像报告', icon: Right, path: '/doctorPage/z-examResult'},
  {key: 'p-pay', label: '收银台', hint: '待结算账单', icon: Right, path: '/doctorPage/y-billList?payStatus=0'},
  {key: 'p-follow', label: '随访提醒', hint: '待回访名单', icon: Right, path: '/doctorPage/z-follow'},
  {key: 'p-rx', label: '开处方', hint: '进入接诊开单', icon: Right, path: '/doctorPage/d-appoint'},
]
const paletteFiltered = computed<Cmd[]>(() => {
  const kw = paletteKw.value.trim().toLowerCase()
  let list = paletteBase
  if (kw) list = list.filter(c => (c.label + c.hint).toLowerCase().includes(kw))
  // 患者搜索：从当前流水线里模糊匹配
  if (kw) {
    const matched: Cmd[] = []
    for (const st of stages.value) for (const c of st.items) {
      if ((c.petName + (c.clientName || '')).toLowerCase().includes(kw))
        matched.push({key: 'c' + c.key, label: c.petName + ' · ' + (c.clientName || ''), hint: c.stageTitle + ' → ' + c.nextLabel, icon: Right, path: c.actionPath})
    }
    list = [...matched, ...list]
  }
  return list.slice(0, 20)
})
const runPalette = (p: Cmd) => { paletteOpen.value = false; if (p.path) router.push(p.path) }

// ---------- 退出 ----------
const doLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '退出登录', {confirmButtonText: '确定退出', cancelButtonText: '取消', type: 'warning'})
    .then(() => { reqLogout().then(res => { clearPwdCheckFlag(); if (res.code === 204) router.push('/login') }) })
    .catch(() => ElMessage.info('已取消'))
}

// ---------- 实时：WS 消息即时刷新 ----------
let unsub: (() => void) | null = null
const onKey = (e: KeyboardEvent) => {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); paletteOpen.value = !paletteOpen.value }
}

onMounted(() => {
  tick(); clockTimer = setInterval(tick, 1000)
  loadAll()
  pollTimer = setInterval(loadAll, 20000)
  unsub = subscribe(() => { loadAll() })
  window.addEventListener('keydown', onKey)
  // 深度页完成动作后回写：监听全局刷新事件
  window.addEventListener('mixpet:flow-refresh', loadAll as EventListener)
})
onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (pollTimer) clearInterval(pollTimer)
  unsub?.(); unsub = null
  window.removeEventListener('keydown', onKey)
  window.removeEventListener('mixpet:flow-refresh', loadAll as EventListener)
})
</script>

<style lang="scss" scoped>
.flow-root { min-height: 100vh; background: linear-gradient(180deg, #f3f7fb 0%, #eef3f8 100%); user-select: none; }
.topbar { background: #fff; border-bottom: 1px solid #e4e7ed; position: sticky; top: 0; z-index: 20; }
.topbar-inner { max-width: 1280px; margin: 0 auto; height: 60px; padding: 0 24px; display: flex; align-items: center; justify-content: space-between; box-sizing: border-box; gap: 16px; }
.brand { display: flex; align-items: center; gap: 10px; font-weight: 700; color: var(--brand); font-size: 16px;
  .brand-mark { display: inline-flex; align-items: center; justify-content: center; width: 30px; height: 30px; border-radius: 6px; background: var(--brand); color: #fff; font-size: 11px; overflow: hidden; img { width: 100%; height: 100%; object-fit: cover; } }
}
.flow-clock { font-variant-numeric: tabular-nums; color: #606266; font-size: 13px; display: flex; align-items: center; gap: 8px;
  .dot { width: 7px; height: 7px; border-radius: 50%; background: #0F8A7E; box-shadow: 0 0 0 3px rgba(15,138,126,.18); }
  .hint { color: #b0b6c4; font-size: 12px; }
}
.nav { display: flex; align-items: center; gap: 4px; list-style: none; margin: 0; padding: 0;
  li a, li :deep(a) { display: inline-flex; align-items: center; gap: 5px; padding: 8px 12px; border-radius: 6px; font-size: 14px; color: #1F2D3D; cursor: pointer; text-decoration: none; }
  li a:hover { background: #EAF3FA; color: var(--brand); }
  .nav-link kbd { background: #eef1f5; border: 1px solid #d8dce5; border-radius: 4px; padding: 0 5px; font-size: 11px; color: #909399; }
  .nav-logout:hover { background: #fef0f0; color: #f56c6c; }
}

.flow-body { max-width: 1280px; margin: 0 auto; padding: 24px; display: grid; grid-template-columns: 1fr 300px; gap: 20px; align-items: start; }
@media (max-width: 1080px) { .flow-body { grid-template-columns: 1fr; } }

.pipeline { display: flex; gap: 0; overflow-x: auto; padding-bottom: 8px; }
.lane { flex: 1 1 0; min-width: 210px; background: #fff; border: 1px solid #e4e7ed; border-radius: 14px; padding: 14px; margin-right: 26px; position: relative; box-shadow: 0 2px 8px rgba(31,45,61,.05); }
.lane.hot { border-color: #c6e2ff; box-shadow: 0 6px 18px rgba(64,158,255,.12); }
.lane-head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.lane-no { width: 24px; height: 24px; border-radius: 8px; background: var(--brand); color: #fff; font-size: 13px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.lane-tit { flex: 1; h3 { margin: 0; font-size: 15px; color: #1F2D3D; } p { margin: 2px 0 0; font-size: 11px; color: #909399; } }
.lane-count { min-width: 26px; height: 26px; padding: 0 6px; border-radius: 13px; background: #eef1f5; color: #909399; font-weight: 700; font-size: 14px; display: flex; align-items: center; justify-content: center; }
.lane-count.on { background: var(--brand); color: #fff; }
.lane-list { display: flex; flex-direction: column; gap: 8px; min-height: 60px; max-height: 52vh; overflow-y: auto; }
.chip { position: relative; text-align: left; border: 1px solid #edf0f3; background: #f7f9fb; border-radius: 10px; padding: 9px 11px; cursor: pointer; transition: border-color .2s, box-shadow .2s, transform .15s; display: flex; flex-direction: column; gap: 2px; }
.chip:hover { border-color: #409eff; box-shadow: 0 4px 12px rgba(64,158,255,.14); transform: translateY(-1px); }
.chip.danger { border-color: #fbc4c4; background: #fff5f5; }
.chip-pet { font-size: 14px; font-weight: 600; color: #1F2D3D; }
.chip-sub { font-size: 11px; color: #909399; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chip-flag { position: absolute; top: 8px; right: 8px; font-size: 10px; color: #f56c6c; font-weight: 700; }
.lane-empty { color: #c0c4cc; font-size: 12px; text-align: center; padding: 18px 0; }
.lane-foot { margin-top: 10px; }
.lane-act { display: inline-block; font-size: 12px; color: var(--brand); text-decoration: none; }
.flow-arrow { position: absolute; right: -22px; top: 50%; transform: translateY(-50%); color: #c0c4cc; z-index: 2; }

.live-rail { background: #fff; border: 1px solid #e4e7ed; border-radius: 14px; padding: 14px; box-shadow: 0 2px 8px rgba(31,45,61,.05); position: sticky; top: 84px; }
.rail-head { display: flex; align-items: center; gap: 8px; font-weight: 600; color: #1F2D3D; font-size: 14px; margin-bottom: 10px;
  .pulse { width: 8px; height: 8px; border-radius: 50%; background: #0F8A7E; animation: pulse 1.4s infinite; }
  .rail-time { margin-left: auto; font-size: 11px; color: #b0b6c4; font-weight: 400; }
}
@keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(15,138,126,.5);} 70% { box-shadow: 0 0 0 7px rgba(15,138,126,0);} 100% { box-shadow: 0 0 0 0 rgba(15,138,126,0);} }
.rail-list { display: flex; flex-direction: column; gap: 8px; max-height: 60vh; overflow-y: auto; }
.evt { display: flex; gap: 10px; padding: 8px 10px; background: #f7f9fb; border-radius: 10px; border-left: 3px solid #c0c4cc; }
.evt.ok { border-left-color: #0F8A7E; }
.evt.warn { border-left-color: #e6a23c; }
.evt.err { border-left-color: #f56c6c; }
.evt-ico { color: #909399; }
.evt-main { flex: 1; min-width: 0; }
.evt-txt { font-size: 13px; color: #303133; line-height: 1.4; }
.evt-time { font-size: 11px; color: #b0b6c4; margin-top: 2px; }
.rail-empty { color: #c0c4cc; font-size: 12px; text-align: center; padding: 16px 0; }

.drawer { .dr-header { b { font-size: 18px; } .dr-sub { color: #909399; margin-left: 8px; font-size: 13px; } .dr-meta { color: #606266; font-size: 12px; margin-top: 4px; } }
  .dr-stage { margin: 14px 0 6px; color: #606266; }
  .dr-next { color: #909399; font-size: 13px; b { color: var(--brand); } }
  .dr-go { display: block; text-align: center; background: var(--brand); color: #fff; border-radius: 10px; padding: 11px; margin: 10px 0; text-decoration: none; font-weight: 600; }
  .dr-tip { color: #b0b6c4; font-size: 12px; line-height: 1.5; }
}

.palette { .palette-list { margin-top: 12px; max-height: 50vh; overflow-y: auto; }
  .palette-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px; cursor: pointer; transition: background .15s; }
  .palette-item:hover { background: #eaf3fa; }
  .pi-ico { color: var(--brand); }
  .pi-label { font-size: 14px; color: #1F2D3D; font-weight: 600; }
  .pi-path { margin-left: auto; font-size: 12px; color: #b0b6c4; }
  .palette-empty { color: #c0c4cc; text-align: center; padding: 16px; }
}

/* 芯片在环节间流动时的过渡 */
.chip-enter-from, .chip-leave-to { opacity: 0; transform: translateY(-6px) scale(.98); }
.chip-leave-active { position: absolute; }
.evt-enter-from { opacity: 0; transform: translateX(10px); }
</style>
