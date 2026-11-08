<template>
  <div class="flow-root">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand">
          <span class="brand-mark">
            <img v-if="hosp.logo" :src="'/api/common/download?name=' + hosp.logo" alt="logo"/>
            <template v-else>{{ brandName ? brandName.charAt(0) : 'P' }}</template>
          </span>
          <span class="brand-name">{{ brandName }} · {{ title }}</span>
        </div>
        <div class="flow-clock">
          <span class="dot"></span>{{ timeText }}
          <span class="hint">全流程实时联动 · 自动流转</span>
        </div>
        <ul class="nav">
          <li><a class="nav-link" @click="paletteOpen = true"><el-icon><Search/></el-icon> 命令面板 <kbd>Ctrl/⌘ K</kbd></a></li>
          <slot name="nav-right"/>
          <li><router-link to="/backstage"><el-icon><User/></el-icon> 个人中心</router-link></li>
          <li><a class="nav-logout" title="退出登录" @click="$emit('logout')"><el-icon><SwitchButton/></el-icon> 退出</a></li>
        </ul>
      </div>
    </header>

    <main class="flow-body">
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
              <span class="chip-pet">{{ c.petName || '—' }}</span>
              <span class="chip-sub">{{ c.sub }}</span>
              <span v-if="c.flag" class="chip-flag">{{ c.flag }}</span>
            </button>
          </TransitionGroup>
          <div v-if="!st.items.length" class="lane-empty">— 空 —</div>
          <div class="lane-foot">
            <a class="lane-act" @click="$emit('navigate', st.actionPath)">{{ st.actionLabel }}</a>
          </div>
          <div v-if="i < stages.length - 1" class="flow-arrow"><el-icon><Right/></el-icon></div>
        </section>
      </div>

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

    <el-drawer v-model="drawer" :title="active ? (active.petName || '患者') + ' · ' + (active.stageTitle || '') : ''" size="420px" direction="rtl">
      <div v-if="active" class="drawer">
        <div class="dr-header">
          <div><b>{{ active.petName || '患者' }}</b> <span class="dr-sub">{{ active.clientName || '' }}</span></div>
          <div class="dr-meta">{{ active.sub }}</div>
        </div>
        <div class="dr-stage">当前环节：<b>{{ active.stageTitle }}</b></div>
        <p class="dr-next">下一步 · <b>{{ active.nextLabel }}</b></p>
        <a class="dr-go" @click="$emit('navigate', active.actionPath || '')">{{ active.nextLabel }}</a>
        <div class="dr-tip">在流内直接处理，完成后本流水线会自动把它推向下一环节。</div>
      </div>
    </el-drawer>

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
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {Right, Search, SwitchButton, User} from '@element-plus/icons-vue'
import {hospital} from '@/utils/hospital'
import type {FlowChip, FlowStage} from '@/request/FlowApi'

const props = defineProps<{
  title: string
  brandName?: string
  stages: FlowStage[]
  events: {id: number; text: string; time: string; cls: string; icon: any}[]
  lastRefresh: string
  commands: {key: string; label: string; hint: string; icon: any; path: string}[]
}>()
const emit = defineEmits<{
  (e: 'navigate', path: string): void
  (e: 'logout'): void
}>()
const hosp = hospital

const timeText = ref('--:--:--')
const pad = (n: number) => n.toString().padStart(2, '0')
const tick = () => { const t = new Date(); timeText.value = `${pad(t.getHours())}:${pad(t.getMinutes())}:${pad(t.getSeconds())}` }
let clockTimer: ReturnType<typeof setInterval> | undefined
onMounted(() => { tick(); clockTimer = setInterval(tick, 1000) })
onBeforeUnmount(() => { if (clockTimer) clearInterval(clockTimer) })

const drawer = ref(false)
const active = ref<FlowChip | null>(null)
const openChip = (c: FlowChip) => { active.value = c; drawer.value = true }

const paletteOpen = ref(false)
const onPaletteHotkey = (e: KeyboardEvent): void => {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    paletteOpen.value = !paletteOpen.value
  }
}
onMounted(() => window.addEventListener('keydown', onPaletteHotkey))
onBeforeUnmount(() => window.removeEventListener('keydown', onPaletteHotkey))
const paletteKw = ref('')
const paletteFiltered = computed(() => {
  const kw = paletteKw.value.trim().toLowerCase()
  let list = props.commands
  if (kw) list = list.filter(c => (c.label + c.hint).toLowerCase().includes(kw))
  if (kw) {
    const matched: typeof props.commands = []
    for (const st of props.stages) for (const c of st.items) {
      if ((c.petName + (c.clientName || '')).toLowerCase().includes(kw))
        matched.push({key: 'c' + c.key, label: (c.petName || '') + ' · ' + (c.clientName || ''), hint: (c.stageTitle || '') + ' → ' + (c.nextLabel || ''), icon: Right, path: c.actionPath || ''})
    }
    list = [...matched, ...list]
  }
  return list.slice(0, 20)
})
const runPalette = (p: {path: string}) => { paletteOpen.value = false; if (p.path) emit('navigate', p.path) }
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
.lane-act { font-size: 12px; color: var(--brand); cursor: pointer; }
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
  .dr-go { display: block; text-align: center; background: var(--brand); color: #fff; border-radius: 10px; padding: 11px; margin: 10px 0; cursor: pointer; font-weight: 600; }
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
.chip-enter-from, .chip-leave-to { opacity: 0; transform: translateY(-6px) scale(.98); }
.chip-leave-active { position: absolute; }
.evt-enter-from { opacity: 0; transform: translateX(10px); }
</style>
