<template>
  <div class="pa">
    <!-- 工具栏（不打印） -->
    <div class="no-print pa-bar">
      <el-button @click="router.back()">返回</el-button>
      <span class="pa-bar-tit">宠物就诊档案</span>
      <el-button v-if="a" :loading="loading" size="small" @click="reload">刷新</el-button>
      <el-button v-if="a" size="small" type="primary" @click="doPrint">打印档案</el-button>
      <span v-if="a" class="pa-bar-hint">
        共 {{ allEvents.length }} 条诊疗记录 · 最后更新 {{ nowText }}
      </span>
    </div>

    <div v-if="a" class="pa-grid">
      <!-- 左：档案卡 -->
      <aside class="pa-side">
        <div class="pa-pet">
          <div class="pa-ava">
            <img v-if="a.pet.petPhoto" :src="photoUrl(a.pet.petPhoto)" alt=""/>
            <span v-else>{{ a.pet.petName?.charAt(0) || '宠' }}</span>
          </div>
          <div class="pa-pet-name">{{ a.pet.petName }}</div>
          <div class="pa-pet-meta">
            {{ a.pet.petSex ? '公' : '母' }} · {{ a.pet.petVariety || '未填品种' }}
            <template v-if="ageText"> · {{ ageText }}</template>
          </div>
          <div class="pa-tags">
            <el-tag v-if="a.pet.petSterilization" size="small" type="success" effect="plain">已绝育</el-tag>
            <el-tag v-if="a.stat.weight" size="small" effect="plain">{{ a.stat.weight }}kg</el-tag>
            <el-tag v-if="a.pet.petChip" size="small" type="info" effect="plain">芯片 {{ a.pet.petChip }}</el-tag>
          </div>
        </div>

        <div class="pa-block">
          <div class="pa-block-tit">主人</div>
          <div class="pa-row"><span>姓名</span><b>{{ a.client?.clientName || '—' }}</b></div>
          <div class="pa-row"><span>电话</span><b>{{ a.client?.clientTel || '—' }}</b></div>
        </div>

        <div class="pa-block">
          <div class="pa-block-tit">就诊概览</div>
          <div class="pa-kpis">
            <div class="pa-kpi"><b>{{ a.stat.visitCount }}</b><span>就诊次数</span></div>
            <div class="pa-kpi"><b>{{ a.stat.firstVisit || '—' }}</b><span>首诊</span></div>
            <div class="pa-kpi"><b>{{ a.stat.lastVisit || '—' }}</b><span>末诊</span></div>
            <div class="pa-kpi"><b>{{ a.stat.nextImmune || '—' }}</b><span>下次免疫</span></div>
          </div>
        </div>

        <div class="pa-block">
          <div class="pa-block-tit">病史要点</div>
          <div class="pa-note"><span>过敏史</span>{{ a.pet.petAllergy || '无' }}</div>
          <div class="pa-note"><span>既往病史</span>{{ a.pet.petHistory || '无' }}</div>
          <div class="pa-note"><span>末次诊断</span>{{ a.stat.lastDiagnosis || '—' }}</div>
        </div>
      </aside>

      <!-- 右：时间线 -->
      <section class="pa-main">
        <div class="pa-chips no-print">
          <button v-for="c in chips" :key="c.kind"
                  :class="['pa-chip', {on: filter === c.kind}]" @click="filter = c.kind">
            {{ c.label }}<i v-if="c.n">{{ c.n }}</i>
          </button>
        </div>

        <el-timeline v-if="filtered.length" class="pa-tl">
          <el-timeline-item v-for="ev in filtered" :key="ev.id" :timestamp="ev.time"
                            placement="top" :type="kindType(ev.kind)" :hollow="ev.kind !== 'visit'">
            <div class="pa-ev">
              <!-- 就诊 -->
              <template v-if="ev.kind === 'visit'">
                <div class="pa-ev-h">
                  <span class="pa-ev-kind v">就诊</span>
                  <b class="pa-ev-tit">{{ ev.row.diagnosis || '未填写诊断' }}</b>
                  <el-tag size="small" :type="recordStatusType(ev.row.status)" effect="plain">
                    {{ recordStatusText(ev.row.status) }}
                  </el-tag>
                  <span class="pa-ev-sub">{{ ev.row.doctorName || '—' }}<template v-if="ev.row.weight"> · {{ ev.row.weight }}kg</template></span>
                  <el-button class="no-print" size="small" text type="primary" @click="printRecord(ev.row)">病历</el-button>
                </div>
                <div v-if="ev.row.chiefComplaint" class="pa-ev-line"><span>主诉</span>{{ ev.row.chiefComplaint }}</div>
                <div v-if="ev.row.presentIllness" class="pa-ev-line"><span>现病史</span>{{ ev.row.presentIllness }}</div>
                <div v-if="ev.row.advice" class="pa-ev-line"><span>医嘱</span>{{ ev.row.advice }}</div>
                <div v-if="rxOf(ev.row.recordId).length" class="pa-rx">
                  <div class="pa-rx-tit">处方 {{ rxOf(ev.row.recordId).length }} 项</div>
                  <table class="pa-rx-tb">
                    <thead>
                    <tr><th>药品</th><th>规格</th><th>数量</th><th>单价</th><th>用法</th><th>天数</th></tr>
                    </thead>
                    <tbody>
                    <tr v-for="(p, i) in rxOf(ev.row.recordId)" :key="i">
                      <td>{{ p.drugName }}</td>
                      <td>{{ p.drugSpec || '—' }}</td>
                      <td>{{ p.quantity }}{{ p.drugUnit || '' }}</td>
                      <td>{{ p.drugPrice != null ? '￥' + Number(p.drugPrice).toFixed(2) : '—' }}</td>
                      <td>{{ p.usageInfo || '—' }}</td>
                      <td>{{ p.durationDays ?? '—' }}</td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </template>

              <!-- 免疫 -->
              <template v-else-if="ev.kind === 'immune'">
                <div class="pa-ev-h">
                  <span class="pa-ev-kind i">免疫</span>
                  <b class="pa-ev-tit">{{ ev.row.drugName }}</b>
                  <span class="pa-ev-sub">第 {{ ev.row.doseCnt || 1 }} 针<template v-if="ev.row.nextTime"> · 下次 {{ ev.row.nextTime.slice(0, 10) }}</template></span>
                </div>
                <div v-if="ev.row.note" class="pa-ev-line"><span>说明</span>{{ ev.row.note }}</div>
              </template>

              <!-- 住院 -->
              <template v-else-if="ev.kind === 'hospital'">
                <div class="pa-ev-h">
                  <span class="pa-ev-kind h">住院</span>
                  <b class="pa-ev-tit">{{ ev.row.roomNo ? '床位 ' + ev.row.roomNo : '住院' }}</b>
                  <el-tag size="small" :type="ev.row.status ? 'info' : 'success'" effect="plain">
                    {{ ev.row.status ? '已出院' : '住院中' }}
                  </el-tag>
                  <span class="pa-ev-sub">
                    {{ fmt(ev.row.inTime) }} → {{ ev.row.outTime ? fmt(ev.row.outTime) : '至今' }}
                    <template v-if="ev.row.days"> · {{ ev.row.days }} 天</template>
                  </span>
                  <el-button class="no-print" size="small" text type="primary" @click="printHospital(ev.row)">记录单</el-button>
                </div>
                <div v-if="ev.row.note" class="pa-ev-line"><span>说明</span>{{ ev.row.note }}</div>
              </template>

              <!-- 手术 -->
              <template v-else-if="ev.kind === 'surgery'">
                <div class="pa-ev-h">
                  <span class="pa-ev-kind s">手术</span>
                  <b class="pa-ev-tit">{{ ev.row.surgeryName }}</b>
                  <span class="pa-ev-sub">
                    {{ ev.row.surgeryType || '—' }} · {{ statusText(ev.row.status) }}
                    <template v-if="ev.row.surgeon"> · 主刀 {{ ev.row.surgeon }}</template>
                  </span>
                  <el-button class="no-print" size="small" text type="primary" @click="printSurgery(ev.row)">记录单</el-button>
                </div>
                <div v-if="ev.row.postopNote" class="pa-ev-line"><span>术后</span>{{ ev.row.postopNote }}</div>
              </template>

              <!-- 化验 -->
              <template v-else-if="ev.kind === 'lab'">
                <div class="pa-ev-h">
                  <span class="pa-ev-kind l">化验</span>
                  <b class="pa-ev-tit">化验单 L{{ String(ev.row.orderId).padStart(6, '0') }}</b>
                  <el-tag size="small" :type="ev.row.status ? 'success' : 'warning'" effect="plain">
                    {{ ev.row.status ? '已出结果' : '待检验' }}
                  </el-tag>
                  <span class="pa-ev-sub">{{ ev.row.orderNote || '' }}</span>
                </div>
              </template>

              <!-- 影像 -->
              <template v-else>
                <div class="pa-ev-h">
                  <span class="pa-ev-kind x">影像</span>
                  <b class="pa-ev-tit">{{ ev.row.itemName || '影像检查' }}</b>
                  <span class="pa-ev-sub">{{ ev.row.examPart || '' }}</span>
                </div>
                <div v-if="ev.row.impression" class="pa-ev-line"><span>提示</span>{{ ev.row.impression }}</div>
              </template>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="没有符合条件的记录"/>
      </section>
    </div>

    <el-empty v-else-if="loaded" description="未找到该宠物"/>
    <div v-else v-loading="loading" class="pa-loading"></div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {PetArchive} from '@/model/DO/PetArchive'
import {reqPetArchive} from '@/request/PetApi'

type Kind = 'visit' | 'immune' | 'hospital' | 'surgery' | 'lab' | 'image'

interface Ev {
  id: string
  kind: Kind
  time: string
  row: any
}

const route = useRoute()
const router = useRouter()
const a = ref<PetArchive | null>(null)
const loading = ref(false)
const loaded = ref(false)
const filter = ref<'all' | Kind>('all')
const nowText = new Date().toLocaleString('zh-CN')

const petId = computed<number>(() => Number(route.query.petId || route.params.petId))

const load = (): void => {
  if (!petId.value) { loaded.value = true; return }
  loading.value = true
  reqPetArchive(petId.value).then(res => {
    a.value = res || null
    loaded.value = true
  }).catch(() => { loaded.value = true }).finally(() => { loading.value = false })
}
const reload = (): void => load()
onMounted(load)

const fmt = (t?: string | null): string => (t || '').replace('T', ' ').slice(0, 16) || '—'
const photoUrl = (p: string): string => (p.startsWith('http') || p.startsWith('/') ? p : '/img/' + p)
const ageText = computed<string>(() => {
  const b = a.value?.pet?.petBirthday
  if (!b) return ''
  const y = (Date.now() - new Date(b).getTime()) / (365.25 * 864e5)
  return y >= 1 ? `${Math.floor(y)} 岁` : `${Math.max(1, Math.round(y * 12))} 个月`
})
const recordStatusText = (s: number): string =>
  ({0: '接诊中', 2: '已作废'} as Record<number, string>)[s] ?? '已完结'
const recordStatusType = (s: number): string => (s === 0 ? 'warning' : s === 2 ? 'danger' : 'success')
const statusText = (s: number): string =>
  ({0: '待手术', 1: '手术中', 2: '已完成', 3: '已取消'} as Record<number, string>)[s] ?? '—'

// ---- 事件合并 ----
const allEvents = computed<Ev[]>(() => {
  if (!a.value) return []
  const list: Ev[] = []
  a.value.visits?.forEach(v => list.push({
    id: 'v' + v.recordId, kind: 'visit', time: fmt(v.createTime), row: v
  }))
  a.value.immune?.forEach(v => list.push({
    id: 'i' + v.immuneId, kind: 'immune', time: fmt(v.vaccinateTime), row: v
  }))
  a.value.hospital?.forEach(v => list.push({
    id: 'h' + v.hospitalId, kind: 'hospital', time: fmt(v.inTime), row: v
  }))
  a.value.surgery?.forEach(v => list.push({
    id: 's' + v.surgeryId, kind: 'surgery', time: fmt(v.startTime || v.scheduledTime), row: v
  }))
  a.value.lab?.forEach(v => list.push({
    id: 'l' + v.orderId, kind: 'lab', time: fmt((v as any).createTime), row: v
  }))
  a.value.image?.forEach(v => list.push({
    id: 'x' + v.orderId, kind: 'image', time: fmt((v as any).createTime), row: v
  }))
  return list.sort((p, q) => (q.time || '').localeCompare(p.time || ''))
})

const countOf = (k: Kind): number => allEvents.value.filter(e => e.kind === k).length
const chips = computed(() => [
  {kind: 'all' as const, label: '全部', n: allEvents.value.length},
  {kind: 'visit' as const, label: '就诊', n: countOf('visit')},
  {kind: 'immune' as const, label: '免疫', n: countOf('immune')},
  {kind: 'hospital' as const, label: '住院', n: countOf('hospital')},
  {kind: 'surgery' as const, label: '手术', n: countOf('surgery')},
  {kind: 'lab' as const, label: '化验', n: countOf('lab')},
  {kind: 'image' as const, label: '影像', n: countOf('image')},
])
const filtered = computed<Ev[]>(() =>
    filter.value === 'all' ? allEvents.value : allEvents.value.filter(e => e.kind === filter.value))

const kindType = (k: Kind): string =>
  ({visit: 'primary', immune: 'success', hospital: 'warning', surgery: 'danger', lab: 'info', image: 'info'} as Record<Kind, string>)[k]

const rxOf = (recordId: number): any[] => a.value?.prescriptions?.[recordId] || []

// ---- 打印 / 跳转 ----
const doPrint = (): void => window.print()
const openWin = (path: string): void => { window.open(path, '_blank') }
// 病历打印页路由名是 d-recordPrint（z-surgeryPrint / z-hospitalPrint 同级，别混用）
const printRecord = (row: any): void => openWin('/#/doctorPage/d-recordPrint?recordId=' + row.recordId)
const printSurgery = (row: any): void => openWin('/#/doctorPage/z-surgeryPrint?surgeryId=' + row.surgeryId)
const printHospital = (row: any): void =>
    openWin('/#/doctorPage/z-hospitalPrint?hospitalId=' + row.hospitalId + '&recordId=' + row.recordId)
</script>

<style lang="scss" scoped>
.pa { padding: 16px; background: #f4f7fb; min-height: 100vh; }
.pa-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.pa-bar-tit { font-size: 16px; font-weight: 700; color: #1f2733; }
.pa-bar-hint { margin-left: auto; font-size: 12px; color: #8a93a6; }

.pa-grid { display: grid; grid-template-columns: 292px minmax(0, 1fr); gap: 14px; align-items: start; }
@media (max-width: 900px) { .pa-grid { grid-template-columns: 1fr; } }

/* 左栏 */
.pa-side {
  background: #fff; border-radius: 12px; padding: 16px;
  box-shadow: 0 1px 3px rgba(30, 111, 168, .06); position: sticky; top: 12px;
}
.pa-pet { text-align: center; padding-bottom: 12px; border-bottom: 1px dashed #e3eaf3; }
.pa-ava {
  width: 72px; height: 72px; margin: 0 auto 8px; border-radius: 50%; overflow: hidden;
  background: linear-gradient(135deg, #1E6FA8, #49a3dd); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 26px; font-weight: 700;
  img { width: 100%; height: 100%; object-fit: cover; }
}
.pa-pet-name { font-size: 17px; font-weight: 700; color: #1f2733; }
.pa-pet-meta { font-size: 12px; color: #6b7686; margin-top: 2px; }
.pa-tags { display: flex; flex-wrap: wrap; gap: 4px; justify-content: center; margin-top: 8px; }

.pa-block { padding: 12px 0; border-bottom: 1px dashed #e3eaf3; &:last-child { border-bottom: 0; } }
.pa-block-tit { font-size: 12px; font-weight: 700; color: #8a93a6; letter-spacing: 1px; margin-bottom: 6px; }
.pa-row { display: flex; justify-content: space-between; font-size: 12.5px; padding: 2px 0;
  span { color: #8a93a6; } b { color: #1f2733; font-weight: 600; } }
.pa-kpis { display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  .pa-kpi { background: #f7fafd; border-radius: 8px; padding: 7px 8px; display: flex; flex-direction: column; gap: 1px;
    b { font-size: 13px; color: #1E6FA8; } span { font-size: 11px; color: #8a93a6; } } }
.pa-note { font-size: 12.5px; color: #3c4657; line-height: 1.6;
  span { display: inline-block; color: #8a93a6; margin-right: 6px; } }

/* 右栏 */
.pa-main { min-width: 0; }
.pa-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.pa-chip {
  border: 1px solid #d8e3ee; background: #fff; color: #52606f; border-radius: 16px;
  padding: 4px 12px; font-size: 12.5px; cursor: pointer; transition: all .15s;
  i { font-style: normal; margin-left: 4px; color: #9aa6b6; font-size: 11px; }
  &.on { background: #1E6FA8; border-color: #1E6FA8; color: #fff; i { color: rgba(255, 255, 255, .75); } }
}
.pa-tl { padding-left: 2px; }
.pa-ev { background: #fff; border-radius: 10px; padding: 10px 12px; box-shadow: 0 1px 3px rgba(30, 111, 168, .06); }
.pa-ev-h { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.pa-ev-kind {
  font-size: 11px; padding: 1px 6px; border-radius: 4px; color: #fff;
  &.v { background: #1E6FA8; } &.i { background: #43a86b; } &.h { background: #d99427; }
  &.s { background: #d0554f; } &.l, &.x { background: #6f7ea0; }
}
.pa-ev-tit { font-size: 13.5px; color: #1f2733; }
.pa-ev-sub { font-size: 12px; color: #8a93a6; }
.pa-ev-line { font-size: 12.5px; color: #3c4657; margin-top: 5px; line-height: 1.6;
  span { color: #8a93a6; margin-right: 6px; } }
.pa-rx { margin-top: 8px; border-top: 1px dashed #e3eaf3; padding-top: 6px; }
.pa-rx-tit { font-size: 11.5px; color: #8a93a6; margin-bottom: 4px; }
.pa-rx-tb {
  width: 100%; border-collapse: collapse;
  th, td { border: 1px solid #e6edf5; padding: 3px 6px; font-size: 11.5px; text-align: left; }
  th { background: #f6f9fd; color: #5c6878; font-weight: 600; }
}
.pa-loading { height: 60vh; }

@media print {
  .pa { padding: 0; background: #fff; }
  .pa-grid { grid-template-columns: 220px 1fr; }
  .pa-side { position: static; box-shadow: none; }
  .pa-ev { box-shadow: none; border: 1px solid #e6edf5; }
}
</style>
