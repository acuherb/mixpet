<template>
  <div class="cf-wrap">
    <div class="cf-head">
      <h2>我的寄养</h2>
      <span>查看爱宠寄养期间的状态、每日照护记录与照片，以及接回后的结算明细</span>
    </div>

    <el-empty v-if="loaded && !list.length" description="暂无寄养记录"/>

    <div v-else class="cf-body">
      <aside class="cf-list">
        <div v-for="f in list" :key="f.fosterId" class="cf-card"
             :class="{active: f.fosterId === activeId}" @click="select(f)">
          <div class="cf-card-tit">
            <b>{{ f.petName }}</b>
            <el-tag size="small" :type="f.status === 2 ? 'success' : f.status === 1 ? 'warning' : 'info'">
              {{ fosterStatusText(f.status) }}
            </el-tag>
          </div>
          <div class="cf-card-sub">{{ f.inDate || '—' }} → {{ f.backDate || f.fosterTerm || '—' }}</div>
          <div class="cf-card-sub">订单 {{ f.fosterCode }}</div>
        </div>
      </aside>

      <section v-if="cur" class="cf-detail">
        <div class="cf-sec">
          <div class="cf-sec-tit">寄养信息</div>
          <div class="cf-grid">
            <div><span>状态</span><b>{{ fosterStatusText(cur.status) }}</b></div>
            <div><span>入托日期</span><b>{{ cur.inDate || '—' }}</b></div>
            <div><span>预计接回</span><b>{{ cur.fosterTerm || '—' }}</b></div>
            <div><span>实际接回</span><b>{{ cur.backDate || '—' }}</b></div>
            <div><span>实际天数</span><b>{{ cur.dayCount != null ? cur.dayCount + ' 天' : '—' }}</b></div>
            <div><span>费用/天</span><b>￥{{ perDay(cur).toFixed(2) }}</b></div>
          </div>
        </div>

        <div class="cf-sec">
          <div class="cf-sec-tit">费用与结算</div>
          <div class="cf-grid">
            <div><span>登记押金</span><b>￥{{ n2(cur.fosterMoney) }}</b></div>
            <div><span>系统预估</span><b>￥{{ (perDay(cur) * (fosterDays(cur.inDate, cur.fosterTerm) || 0)).toFixed(2) }}</b></div>
            <div><span>最终结算</span><b>￥{{ cur.settleMoney != null ? n2(cur.settleMoney) : '未结算' }}</b></div>
            <div><span>押金冲抵</span><b>￥{{ n2(cur.depositUsed) }}</b></div>
            <div><span>应退</span><b>￥{{ n2(cur.refundMoney) }}</b></div>
            <div><span>需补缴</span><b>￥{{ n2(cur.chargeMoney) }}</b></div>
          </div>
          <div v-if="cur.adjustNote" class="cf-note">改价说明：{{ cur.adjustNote }}</div>
          <div class="cf-actions">
            <el-button v-if="cur.status === 2" size="small" type="primary" plain @click="printOne(cur)">查看 / 下载结算单</el-button>
            <span v-if="cur.chargeMoney && cur.chargeMoney > 0" class="cf-tip">待缴费用可在「账单明细」里查看</span>
          </div>
        </div>

        <div class="cf-sec">
          <div class="cf-sec-tit">每日照护记录（{{ logs.length }}）</div>
          <div v-if="!logs.length" class="cf-none">还没有照护记录，寄养开始后医院会每日更新</div>
          <div v-for="l in logs" :key="l.logId" class="cf-log">
            <div class="cf-log-date">{{ l.logDate }} <span>{{ l.recorder || '' }}</span></div>
            <div class="cf-log-fields">
              精神 {{ l.mentalState || '—' }} · 食欲 {{ l.appetite || '—' }} · 排便 {{ l.defecation || '—' }}
              <template v-if="l.weight != null"> · 体重 {{ l.weight }}kg</template>
            </div>
            <div v-if="l.note" class="cf-log-note">{{ l.note }}</div>
            <div v-if="photosOf(l.logDate).length" class="cf-log-photos">
              <a v-for="p in photosOf(l.logDate)" :key="p.photoId" :href="fosterPhotoUrl(p.photoId)" target="_blank">
                <img :src="fosterPhotoUrl(p.photoId)" alt="寄养照片" loading="lazy"/>
              </a>
            </div>
          </div>
        </div>
      </section>
      <section v-else class="cf-detail cf-none">从左侧选择一笔寄养查看详情</section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {Foster, fosterDays, fosterStatusText} from '@/model/entiy/Foster'
import {FosterDto} from '@/model/DO/FosterDto'
import {FosterLog, FosterPhoto} from '@/model/VO/FosterCare'
import {reqFosterLogs, reqFosterPhotos, reqFosterSelf, fosterPhotoUrl} from '@/request/FosterApi'

const list = ref<FosterDto[]>([])
const cur = ref<FosterDto | null>(null)
const activeId = ref<number | null>(null)
const logs = ref<FosterLog[]>([])
const photos = ref<FosterPhoto[]>([])
const loaded = ref(false)

const n = (v: number | null | undefined): number => v == null ? 0 : Number(v)
const n2 = (v: number | null | undefined): string => n(v).toFixed(2)
const perDay = (f: Foster): number => n(f.dailyFee) + n(f.cleanFee) + n(f.mealFee)
const photosOf = (date: string): FosterPhoto[] => photos.value.filter(p => p.logDate === date)
const printOne = (f: FosterDto): void => {
  window.open('/#/reception/clientFosterPrint?id=' + f.fosterId, '_blank')
}

const select = (f: FosterDto): void => {
  cur.value = f
  activeId.value = f.fosterId
  logs.value = []
  photos.value = []
  reqFosterLogs(f.fosterId).then(res => { logs.value = res || [] })
  reqFosterPhotos(f.fosterId).then(res => { photos.value = res || [] })
}

onMounted(() => {
  reqFosterSelf().then(res => {
    list.value = res || []
    if (list.value.length) select(list.value[0])
  }).finally(() => { loaded.value = true })
})
</script>

<style scoped>
.cf-wrap { max-width: 1080px; margin: 0 auto; padding: 16px; }
.cf-head h2 { margin: 0 0 4px; font-size: 20px; }
.cf-head span { font-size: 12px; color: #909399; }
.cf-body { display: flex; gap: 16px; margin-top: 16px; align-items: flex-start; }
.cf-list { width: 260px; flex: none; }
.cf-card { background: #fff; border: 1px solid #e8edf5; border-radius: 10px; padding: 12px; margin-bottom: 10px; cursor: pointer; }
.cf-card:hover, .cf-card.active { border-color: #409eff; background: #f2f8ff; }
.cf-card-tit { display: flex; justify-content: space-between; align-items: center; font-size: 14px; }
.cf-card-sub { font-size: 12px; color: #909399; margin-top: 4px; }
.cf-detail { flex: 1; min-width: 0; }
.cf-sec { background: #fff; border: 1px solid #e8edf5; border-radius: 10px; padding: 14px; margin-bottom: 12px; }
.cf-sec-tit { font-size: 14px; font-weight: 700; margin-bottom: 10px; border-left: 3px solid #409eff; padding-left: 8px; }
.cf-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 8px; }
.cf-grid div { font-size: 12.5px; }
.cf-grid span { color: #909399; margin-right: 6px; }
.cf-grid b { color: #1F2D3D; }
.cf-note { margin-top: 8px; font-size: 12px; color: #b88230; }
.cf-actions { margin-top: 12px; display: flex; align-items: center; gap: 10px; }
.cf-tip { font-size: 12px; color: #e6a23c; }
.cf-none { color: #b3bccb; font-size: 13px; padding: 20px 0; text-align: center; }
.cf-log { border-top: 1px dashed #ebeef5; padding: 10px 0; }
.cf-log-date { font-weight: 700; font-size: 13px; }
.cf-log-date span { font-weight: 400; font-size: 12px; color: #909399; margin-left: 8px; }
.cf-log-fields { font-size: 12px; color: #606266; margin-top: 4px; }
.cf-log-note { font-size: 12.5px; margin-top: 4px; white-space: pre-wrap; }
.cf-log-photos { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
.cf-log-photos img { width: 92px; height: 92px; object-fit: cover; border-radius: 6px; border: 1px solid #ebeef5; }
@media (max-width: 768px) {
  .cf-body { flex-direction: column; }
  .cf-list { width: 100%; }
}
</style>
