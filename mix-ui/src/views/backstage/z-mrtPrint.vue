<template>
  <div class="mp-page">
    <div class="no-print mp-bar">
      <el-button @click="goBack">返回</el-button>
      <el-select v-model="tpl" style="width: 210px" @change="onTplChange">
        <el-option-group label="体检报告（7 种）">
          <el-option v-for="o in CHECKUP_TPLS" :key="o.v" :label="o.l" :value="o.v"/>
        </el-option-group>
        <el-option-group label="B超 / X光报告单">
          <el-option v-for="o in IMAGE_TPLS" :key="o.v" :label="o.l" :value="o.v"/>
        </el-option-group>
      </el-select>
      <el-button :disabled="!sheet || !!error" type="primary" @click="doPrint">打印</el-button>
      <template v-if="isCheckup">
        <el-input v-model="petIdInput" clearable placeholder="宠物ID（可空，空白表手写）" style="width: 190px"/>
        <el-button @click="loadPet">载入档案</el-button>
      </template>
      <span class="mp-hint" :class="{'mp-err': error}">
        <template v-if="error">{{ error }}</template>
        <template v-else>
          版式移植自老系统 {{ sheet?.name || tpl }} · {{ sizeText }}
          <template v-if="!hasData"> · 未载入数据，打印空白表</template>
        </template>
      </span>
    </div>

    <div v-if="sheet" class="mp-sheet" :style="{width: sheet.w + 'cm', height: sheet.h + 'cm'}">
      <div class="mp-canvas" :style="{
        left: sheet.ml + 'cm', top: sheet.mt + 'cm',
        width: (sheet.w - sheet.ml * 2) + 'cm', height: (sheet.h - sheet.mt * 2) + 'cm'
      }">
        <div v-for="(c, i) in sheet.comps" :key="i" class="mp-comp" :class="'mp-' + c.t" :style="compStyle(c)">
          <template v-if="c.t === 'check'">{{ checked(c.cb) ? '☑' : '☐' }}</template>
          <img v-else-if="c.t === 'image' && c.ib && data[c.ib]" :src="photoUrl(data[c.ib])" alt=""/>
          <span v-else-if="c.t === 'text'" class="mp-text">{{ renderText(c.s) }}</span>
        </div>
      </div>
    </div>
    <div v-else-if="loaded" class="no-print mp-empty">未找到该模板版式：{{ tpl }}</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {reqImageByRecord, reqImageRequisition} from '@/request/ImageApi'
import {reqPetArchive} from '@/request/PetApi'
import {Requisition} from '@/model/entiy/Requisition'
import {ImageOrder} from '@/model/entiy/ImageOrder'
import {getAge} from '@/utils/TimeUtil'
import {useBrand} from '@/utils/UseBrand'

/** 版式 JSON 组件（由 migration/tools/mrt2json.py 生成） */
interface Comp {
  t: 'text' | 'check' | 'image' | 'line' | 'rect' | 'cap'
  x: number; y: number; w: number; h: number
  s?: string; f?: number; b?: boolean; a?: string; v?: string; bd?: number
  bg?: string; fg?: string; ww?: number; cb?: string; ib?: string; ln?: string
}

interface Sheet {
  name: string; w: number; h: number; ml: number; mt: number; comps: Comp[]
}

const CHECKUP_TPLS = [
  {v: 'CheckupTijian', l: '常规体检表'}, {v: 'CheckupPifu', l: '皮肤检查表'},
  {v: 'CheckupYachi', l: '牙齿检查表'}, {v: 'CheckupYankeh', l: '眼科检查表'},
  {v: 'CheckupOral', l: '口腔检查表'}, {v: 'CheckupPatho', l: '病理检查表'},
  {v: 'CheckupHealth', l: '健康检查表（出境）'},
]
const IMAGE_TPLS = [
  {v: 'TypeB', l: 'B超报告单（A5横）'}, {v: 'TypeBFull', l: 'B超报告单（含图 A4）'},
  {v: 'TypeX', l: 'X光报告单（A5横）'}, {v: 'TypeXFull', l: 'X光报告单（含图 A4）'},
]

const route = useRoute()
const router = useRouter()
const {hosp, load: loadBrand} = useBrand()
const tpl = ref(String(route.query.tpl || 'CheckupTijian'))
const sheet = ref<Sheet | null>(null)
const loaded = ref(false)
const data = ref<Record<string, string>>({})
/** 数据/版式加载失败：禁止打印，避免输出串单或缺患者信息的空白文书 */
const error = ref('')
const petIdInput = ref(String(route.query.petId || ''))
const isCheckup = computed(() => tpl.value.startsWith('Checkup'))
const hasData = computed(() => Object.keys(data.value).length > 0)
const sizeText = computed(() => sheet.value ? `${sheet.value.w}×${sheet.value.h}cm` : '')

let pageStyle: HTMLStyleElement | null = null
const mountPageStyle = (s: Sheet): void => {
  pageStyle?.remove()
  pageStyle = document.createElement('style')
  pageStyle.textContent = `@page { size: ${s.w}cm ${s.h}cm; margin: 0; }`
  document.head.appendChild(pageStyle)
}

const loadSheet = (): void => {
  loaded.value = false
  sheet.value = null
  pageStyle?.remove()   // 切模板失败时不能沿用上一模板的纸张尺寸
  fetch(import.meta.env.BASE_URL + 'mrt/' + tpl.value + '.json')
    .then(r => r.ok ? r.json() : Promise.reject(new Error('missing')))
    .then((s: Sheet) => {
      if (!s || !Array.isArray(s.comps) || !s.w || !s.h) throw new Error('bad layout')
      sheet.value = s
      mountPageStyle(s)
      loaded.value = true
    })
    .catch(() => {
      loaded.value = true
      error.value = `版式 ${tpl.value} 加载失败，请刷新页面重试`
    })
}

/* ---------------- 数据注入 ---------------- */
const fmt = (t: string | null | undefined): string => (t || '').replace('T', ' ').slice(0, 16) || '—'
const nowText = (): string => new Date().toLocaleString('zh-CN')
// 报告附件走 /api/common/downReport（与 UpImg.vue 的 upReportFile 配对），/img/ 并不存在
const photoUrl = (p: string): string => p.startsWith('http') || p.startsWith('/')
  ? p : '/api/common/downReport?name=' + encodeURIComponent(p)

const baseFromReq = (r: Requisition): Record<string, string> => {
  const d: Record<string, string> = {
    COMPANY_NAME: hosp.value.name || '动物诊疗机构',
    M_FULLNAME: r.clientName || '', M_MEMBER_NAME: r.clientName || '', MEMBER_NAME: r.clientName || '',
    M_PET_NAME: r.petName || '', chongwuName: r.petName || '',
    M_BREED: r.petVariety || '', pinzhong: r.petVariety || '', pinming: r.petVariety || '',
    M_AGE: getAge(r.petBirthday), nianling: getAge(r.petBirthday),
    M_SEX: r.petSex == null ? '' : (r.petSex ? '公' : '母'),
    xingbie: r.petSex == null ? '' : (r.petSex ? '公' : '母'),
    M_WEIGHT: r.petWeight != null ? String(r.petWeight) + 'kg' : '',
    M_MEMBER_ID: String(r.orderNo || ''), M_CASE_ID: String(r.orderId || ''),
    M_CREATE_DATE: fmt(r.createTime), riqi: fmt(r.createTime), CREATE_DATE: fmt(r.createTime),
    M_DOCT_FULLNAME: r.doctorName || '', M_TYPEB_DOCT: r.doctorName || '',
    M_TYPEX_DOCT: r.doctorName || '', yishi: r.doctorName || '',
    M_DESC_SYMPTOM: r.diagnosis || '', zhengzhuang: r.diagnosis || '',
    M_MEMO: r.remark || '', CHECK_PART: r.examPart || '', CHECK_PART_POSI: r.examPart || '',
    M_PET_ID: r.petChip || '',
  }
  return d
}

const loadImageData = (): void => {
  const orderId = Number(route.query.orderId)
  const recordId = Number(route.query.recordId)
  if (!orderId) { data.value = {}; return }
  reqImageRequisition(orderId).then(base => {
    const d = baseFromReq(base)
    const isB = tpl.value.startsWith('TypeB')
    d[isB ? 'M_TYPEB_ID' : 'M_TYPEX_ID'] = base.orderNo || String(orderId)
    d.DESCRIPT = base.items?.map(i => i.name).join('、') || ''
    d.SUMCOST = String(base.fee ?? 0)
    data.value = d
    if (!recordId) return
    reqImageByRecord(recordId).then(list => {
      const hit: ImageOrder | undefined = (list || []).find((o: ImageOrder) => o.orderId === orderId)
      if (!hit) return
      const dx: Record<string, string> = {...data.value}
      dx.M_DESC_CHAO_PRINT = hit.findings || hit.result || ''
      dx.M_DESC_ABNORMAL = hit.findings ? (hit.result || '') : ''
      dx.M_DESC_ALERT = hit.impression || ''
      dx.CHECK_RESULT = hit.impression || hit.result || ''
      dx.METHOD = hit.itemName || ''
      dx.CHECK_NEED = hit.note || ''
      dx.CHECK_PART = hit.examPart || dx.CHECK_PART
      dx.CHECK_PART_POSI = hit.examPart || ''
      dx.DESCRIPT = hit.itemName || dx.DESCRIPT
      dx.M_CREATE_DATE = fmt(hit.reportTime || hit.createTime)
      dx.M_MEMO = hit.note || dx.M_MEMO
      if (hit.imageFiles) {
        try {
          const arr = JSON.parse(hit.imageFiles)
          if (Array.isArray(arr)) { dx.M_IMAGE1 = arr[0] || ''; dx.M_IMAGE2 = arr[1] || '' }
        } catch { /* 非 JSON 忽略 */ }
      }
      data.value = dx
    }).catch(() => { error.value = '影像报告明细加载失败，请刷新重试' })
  }).catch(() => { data.value = {}; error.value = '影像申请单加载失败，请检查单号或权限' })
}

const loadPetData = (petId: number): void => {
  if (!petId) { data.value = {}; return }
  reqPetArchive(petId).then(a => {
    if (!a || !a.pet) { data.value = {}; error.value = `未找到宠物 ${petId} 的档案`; return }
    const c: any = a.client, p: any = a.pet, st: any = a.stat
    const d: Record<string, string> = {
      COMPANY_NAME: hosp.value.name || '动物诊疗机构',
      M_MEMBER_NAME: c?.clientName || '', MEMBER_NAME: c?.clientName || '', M_FULLNAME: c?.clientName || '',
      MEMBER_MOBILE: c?.clientTel || '', MEMBER_ADDRESS: c?.clientAddress || '',
      M_MEMBER_ID: String(c?.clientId || ''), M_PET_ID: String(p.petId || ''),
      M_PET_NAME: p.petName || '', chongwuName: p.petName || '',
      M_BREED: p.petVariety || '', pinzhong: p.petVariety || '', pinming: p.petVariety || '',
      M_AGE: getAge(p.petBirthday), nianling: getAge(p.petBirthday),
      M_SEX: p.petSex == null ? '' : (p.petSex ? '公' : '母'),
      xingbie: p.petSex == null ? '' : (p.petSex ? '公' : '母'),
      M_WEIGHT: st?.weight != null ? String(st.weight) + 'kg' : '',
      M_CASE_ID: a.visits?.[0] ? String((a.visits[0] as any).recordId || '') : '',
      M_CREATE_DATE: nowText(), CREATE_DATE: nowText(), riqi: nowText(),
      M_DOCT_FULLNAME: '', yishi: '', DESCRIPTION: '', jianyi: '',
    }
    data.value = d
  }).catch(() => { data.value = {}; error.value = '宠物档案加载失败，请检查宠物ID或权限' })
}

const loadPet = (): void => {
  const id = Number(petIdInput.value)
  // 只改 query，由 watch 统一重取（避免与 loadAll 重复请求）
  router.replace({query: {...route.query, petId: id ? String(id) : undefined}})
}

const loadAll = (): void => {
  error.value = ''
  data.value = {}
  loadSheet()
  if (isCheckup.value) loadPetData(Number(route.query.petId || petIdInput.value))
  else loadImageData()
}

/* ---------------- 渲染 ---------------- */
const renderText = (s?: string): string => {
  if (!s) return ''
  return s.replace(/\{(?:Date\.)?temp\.([A-Za-z0-9_]+)\}/g, (_m, k) => data.value[k] || '')
    .replace(/\{Time\}/g, nowText())
}
const checked = (key?: string): boolean => {
  if (!key) return false
  const v = data.value[key]
  return !!v && !['0', 'false', 'n', 'no', '否'].includes(String(v).toLowerCase())
}

const compStyle = (c: Comp): Record<string, string> => {
  const st: Record<string, string> = {
    left: c.x + 'cm', top: c.y + 'cm', width: c.w + 'cm', height: c.h + 'cm',
  }
  if (c.t === 'text') {
    st.fontSize = (c.f || 9.5) + 'pt'
    st.fontWeight = c.b ? '700' : '400'
    st.justifyContent = c.a === 'center' ? 'center' : (c.a === 'right' ? 'flex-end' : 'flex-start')
    st.alignItems = c.v === 'center' ? 'center' : (c.v === 'bottom' ? 'flex-end' : 'flex-start')
    if (c.bg) st.background = c.bg
    if (c.fg) st.color = c.fg
    // 所见/印象等长文本必须换行；Stimulsoft 未显式写 WordWrap 的短标签同样安全
    st.whiteSpace = 'pre-wrap'
  }
  if (c.t === 'check') { st.justifyContent = 'center'; st.alignItems = 'center'; st.fontSize = '13pt' }
  if (c.t === 'line') {
    st.background = '#333'
    if (c.ln === 'h') st.height = '1px'
    else st.width = '1px'
  }
  if (c.bd) st.border = '1px solid #333'
  return st
}

const onTplChange = (): void => {
  router.replace({query: {...route.query, tpl: tpl.value}})
}
const doPrint = (): void => window.print()
const goBack = (): void => {
  if (window.history.length > 1) router.back()
  else window.close()
}

// 品牌名异步到达（晚于 immediate watch 的数据注入）时补进版式
watch(() => hosp.value.name, n => {
  if (n) data.value = {...data.value, COMPANY_NAME: n}
})

// 同一路由换单据（orderId/recordId/petId）Vue 会复用组件：必须全部监听，
// 否则版式/患者信息停留在上一份，打印会串单（参照 z-examReport 的同类处理）
watch(() => [route.query.tpl, route.query.petId, route.query.orderId, route.query.recordId], () => {
  tpl.value = String(route.query.tpl || 'CheckupTijian')
  petIdInput.value = String(route.query.petId || '')
  loadAll()
}, {immediate: true})
onMounted(() => {
  loadBrand()
  if (route.query.auto === '1') {
    // 等版式与数据都就绪再打印，最多等 6 秒；失败则提示而不是打半张纸
    const tick = (n: number): void => {
      if (error.value) { ElMessage.error(error.value); return }
      if (sheet.value && loaded.value) { window.print(); return }
      if (n > 0) setTimeout(() => tick(n - 1), 300)
      else ElMessage.error('报告数据加载超时，未执行打印')
    }
    tick(20)
  }
})
onBeforeUnmount(() => pageStyle?.remove())
</script>

<style lang="scss" scoped>
.mp-page { padding: 16px; background: #f3f6fb; min-height: 100vh; }
.mp-bar { max-width: 230mm; margin: 0 auto 12px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.mp-hint { color: #8a93a6; font-size: 12px; }
.mp-err { color: #d03050; font-weight: 600; }
.mp-sheet {
  margin: 0 auto; background: #fff; position: relative;
  box-shadow: 0 2px 16px rgba(30, 111, 168, .10); color: #000;
  font-family: "宋体", SimSun, "Noto Serif CJK SC", serif;
}
.mp-canvas { position: absolute; }
.mp-comp { position: absolute; box-sizing: border-box; display: flex; overflow: hidden; }
.mp-text { white-space: pre-wrap; width: 100%; line-height: 1.25; }
.mp-image { img { width: 100%; height: 100%; object-fit: contain; } }
.mp-rect, .mp-cap { pointer-events: none; }
.mp-empty { text-align: center; color: #8a93a6; margin: 40px auto; }
@media print {
  .mp-page { padding: 0; background: #fff; }
  .mp-sheet { box-shadow: none; }
}
</style>
