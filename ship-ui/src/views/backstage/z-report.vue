<template>
  <div class="report-wrap">
    <!--筛选栏-->
    <el-card shadow="never">
      <div class="filter-row">
        <el-icon class="f-ico bg-blue"><DataBoard/></el-icon>
        <el-date-picker
            v-model="range"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            range-separator="至"
            size="large"
            start-placeholder="开始日期"
            type="daterange"
            value-format="YYYY-MM-DD"
        />
        <el-button-group style="margin-left: 12px">
          <el-button size="large" @click="quick('week')">近7天</el-button>
          <el-button size="large" @click="quick('month')">本月</el-button>
          <el-button size="large" @click="quick('lastMonth')">上月</el-button>
        </el-button-group>
        <el-button style="margin-left: 12px" size="large" type="primary" @click="load">
          <el-icon><Search/></el-icon>
          查询
        </el-button>
      </div>
      <p class="tip">按"接诊日期"统计已收款、未退款账单（折后实收）；本区间会员优惠合计
        <b style="color:#e6a23c">￥{{ money(r.discountMoney) }}</b>。药品成本按当前进货价估算。</p>
    </el-card>

    <!--核心指标-->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="s-ico bg-red"><el-icon><Money/></el-icon></div>
        <div class="s-body">
          <p class="s-label">总收入</p>
          <p class="s-value">￥{{ money(r.totalRevenue) }}</p>
          <el-tag v-if="deltaText()" :type="deltaType()" size="small" style="margin-top:4px">
            环比 {{ deltaText() }}
          </el-tag>
        </div>
      </div>
      <div class="stat-card">
        <div class="s-ico bg-blue"><el-icon><Tickets/></el-icon></div>
        <div class="s-body">
          <p class="s-label">订单数</p>
          <p class="s-value">{{ r.orderCount }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="s-ico bg-green"><el-icon><Wallet/></el-icon></div>
        <div class="s-body">
          <p class="s-label">客单价</p>
          <p class="s-value">￥{{ money(r.avgOrder) }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="s-ico bg-orange"><el-icon><RefreshLeft/></el-icon></div>
        <div class="s-body">
          <p class="s-label">退款</p>
          <p class="s-value refund">￥{{ money(r.refundAmount) }}（{{ r.refundCount }} 单）</p>
        </div>
      </div>
    </div>

    <div class="mid-row">
      <!--收入构成-->
      <el-card shadow="never" class="panel">
        <template #header>
          <div class="c-header">
            <el-icon class="c-ico bg-green"><PieChart/></el-icon>
            <b>收入构成（按收费项目）</b>
          </div>
        </template>
        <div class="compose">
          <div v-for="c in compose" :key="c.label" class="compose-row">
            <span class="c-label">{{ c.label }}</span>
            <div class="c-track"><div class="c-fill" :style="{width: pct(c.amount) + '%'}"></div></div>
            <span class="c-amt">￥{{ money(c.amount) }}</span>
          </div>
        </div>
      </el-card>

      <!--药品毛利-->
      <el-card shadow="never" class="panel">
        <template #header>
          <div class="c-header">
            <el-icon class="c-ico bg-teal"><Goods/></el-icon>
            <b>药品毛利</b>
          </div>
        </template>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="药费收入">￥{{ money(r.feeDrug) }}</el-descriptions-item>
          <el-descriptions-item label="药品成本">￥{{ money(r.drugCost) }}</el-descriptions-item>
          <el-descriptions-item label="毛利">
            <b :style="{color: r.drugProfit >= 0 ? '#67c23a' : '#f56c6c'}">￥{{ money(r.drugProfit) }}</b>
          </el-descriptions-item>
        </el-descriptions>
        <p class="tip">成本按当前进货价估算，未快照历史进货价；服务类(检验/影像/手术/住院/套餐)暂不计成本。</p>
      </el-card>
    </div>

    <!--收入趋势-->
    <el-card shadow="never">
      <template #header>
        <div class="c-header">
          <el-icon class="c-ico bg-blue"><TrendCharts/></el-icon>
          <b>每日收入趋势</b>
        </div>
      </template>
      <MiniBar :items="r.trend.map(d => ({name: d.date.slice(5), value: d.amount, label: '￥' + money(d.amount)}))"
               empty-text="该区间暂无已收款账单"/>
    </el-card>

    <div class="mid-row">
      <!--按科室-->
      <el-card shadow="never" class="panel">
        <template #header>
          <div class="c-header">
            <el-icon class="c-ico bg-purple"><OfficeBuilding/></el-icon>
            <b>按科室</b>
          </div>
        </template>
        <el-table :data="r.byDepartment" border size="small">
          <el-table-column label="科室" min-width="120" prop="name"/>
          <el-table-column label="收入(元)" min-width="120">
            <template #default="s">￥{{ money(s.row.amount) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!r.byDepartment.length" :image-size="40" description="暂无数据"/>
      </el-card>
      <!--按医生-->
      <el-card shadow="never" class="panel">
        <template #header>
          <div class="c-header">
            <el-icon class="c-ico bg-orange"><User/></el-icon>
            <b>按医生</b>
          </div>
        </template>
        <el-table :data="r.byDoctor" border size="small">
          <el-table-column label="医生" min-width="120" prop="name"/>
          <el-table-column label="收入(元)" min-width="120">
            <template #default="s">￥{{ money(s.row.amount) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!r.byDoctor.length" :image-size="40" description="暂无数据"/>
      </el-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref, watch} from 'vue'
import MiniBar from "@/components/chart/MiniBar.vue";
import {
  DataBoard, Goods, Money, OfficeBuilding, PieChart, RefreshLeft, Search, Tickets,
  TrendCharts, User, Wallet
} from "@element-plus/icons-vue";
import {FinanceReport} from "@/model/entiy/FinanceReport";
import {reqFinanceReport} from "@/request/StatisticsApi";

const empty = (): FinanceReport => ({
  startDate: null, endDate: null, totalRevenue: 0, discountMoney: 0, orderCount: 0, avgOrder: 0,
  refundCount: 0, refundAmount: 0, feeRegister: 0, feeTreat: 0, feeDrug: 0, feeLab: 0,
  feeImage: 0, feePackage: 0, feeSurgery: 0, feeHospital: 0, drugCost: 0, drugProfit: 0,
  trend: [], byDepartment: [], byDoctor: []
})
const r = ref<FinanceReport>(empty())
const range = ref<[string, string]>([monthFirst(), today()])
const prevRevenue = ref<number | null>(null)

function today(): string { const d = new Date(); return iso(d) }
function monthFirst(): string { const d = new Date(); return iso(new Date(d.getFullYear(), d.getMonth(), 1)) }
function iso(d: Date): string {
  const p = (x: number) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const money = (n: number | null | undefined): string => (n ?? 0).toFixed(2)

const load = (): void => {
  reqFinanceReport(range.value[0], range.value[1]).then(res => {
    r.value = res || empty()
    // 环比：取等长且紧邻当前区间之前的周期
    const s = new Date(range.value[0] + 'T00:00:00')
    const e = new Date(range.value[1] + 'T00:00:00')
    const days = Math.round((e.getTime() - s.getTime()) / 86400000) + 1
    const pe = new Date(s.getTime() - 86400000)
    const ps = new Date(pe.getTime() - (days - 1) * 86400000)
    reqFinanceReport(iso(ps), iso(pe)).then(p => { prevRevenue.value = p?.totalRevenue ?? null })
  })
}
const quick = (k: 'week' | 'month' | 'lastMonth'): void => {
  if (k === 'week') { const d = new Date(); d.setDate(d.getDate() - 6); range.value = [iso(d), today()] }
  else if (k === 'month') { range.value = [monthFirst(), today()] }
  else {
    const d = new Date()
    const first = new Date(d.getFullYear(), d.getMonth() - 1, 1)
    const last = new Date(d.getFullYear(), d.getMonth(), 0)
    range.value = [iso(first), iso(last)]
  }
  load()
}
const deltaText = (): string => {
  if (prevRevenue.value == null || prevRevenue.value === 0) return ''
  const d = (r.value.totalRevenue - prevRevenue.value) / prevRevenue.value * 100
  return `${d >= 0 ? '+' : ''}${d.toFixed(1)}%`
}
const deltaType = (): 'success' | 'danger' | 'info' => {
  if (prevRevenue.value == null || prevRevenue.value === 0) return 'info'
  return r.value.totalRevenue >= prevRevenue.value ? 'success' : 'danger'
}

const compose = ref<{ label: string; amount: number }[]>([])
const buildCompose = (): void => {
  compose.value = [
    {label: '挂号费', amount: r.value.feeRegister},
    {label: '诊疗费', amount: r.value.feeTreat},
    {label: '药费', amount: r.value.feeDrug},
    {label: '化验费', amount: r.value.feeLab},
    {label: '影像费', amount: r.value.feeImage},
    {label: '套餐费', amount: r.value.feePackage},
    {label: '手术费', amount: r.value.feeSurgery},
    {label: '住院费', amount: r.value.feeHospital},
  ]
}
const maxCompose = (): number => Math.max(1, ...compose.value.map(c => c.amount))
const pct = (v: number): number => Math.round((v / maxCompose()) * 100)

onMounted(() => {
  load()
})
watch(r, buildCompose, {deep: true})
</script>

<style scoped>
.report-wrap { display: flex; flex-direction: column; gap: 14px; }
.filter-row { display: flex; align-items: center; flex-wrap: wrap; }
.f-ico {
  width: 34px; height: 34px; border-radius: 9px; color: #fff; font-size: 18px;
  display: flex; align-items: center; justify-content: center; margin-right: 10px;
}
.tip { margin: 8px 0 0; font-size: 12px; color: #909399; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat-card {
  background: #fff; border-radius: 12px; padding: 18px 20px;
  display: flex; align-items: center; gap: 14px; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.s-ico {
  width: 50px; height: 50px; border-radius: 13px; color: #fff; font-size: 24px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.bg-red { background: linear-gradient(135deg, #f78ca0, #f56c6c); }
.bg-orange { background: linear-gradient(135deg, #ffb74d, #f57c00); }
.bg-blue { background: linear-gradient(135deg, #64b5f6, #1976d2); }
.bg-green { background: linear-gradient(135deg, #81c784, #43a047); }
.bg-teal { background: linear-gradient(135deg, #4db6ac, #00897b); }
.bg-purple { background: linear-gradient(135deg, #ba68c8, #8e24aa); }
.s-body { min-width: 0; }
.s-label { margin: 0; font-size: 13px; color: #909399; }
.s-value { margin: 6px 0 0; font-size: 24px; font-weight: 700; color: #303133; white-space: nowrap; }
.s-value.refund { color: #e6a23c; font-size: 20px; }
.mid-row { display: grid; grid-template-columns: 1.6fr 1fr; gap: 14px; }
.panel { min-width: 0; }
.c-header { display: flex; align-items: center; gap: 10px; }
.c-ico {
  width: 28px; height: 28px; border-radius: 8px; color: #fff; font-size: 15px;
  display: flex; align-items: center; justify-content: center;
}
.compose { display: flex; flex-direction: column; gap: 10px; }
.compose-row { display: flex; align-items: center; gap: 10px; }
.c-label { width: 56px; font-size: 13px; color: #606266; flex-shrink: 0; }
.c-track { flex: 1; height: 16px; background: #f2f3f5; border-radius: 4px; overflow: hidden; }
.c-fill { height: 100%; background: linear-gradient(90deg, #409eff, #67c23a); }
.c-amt { width: 90px; text-align: right; font-size: 13px; color: #303133; flex-shrink: 0; }
</style>
