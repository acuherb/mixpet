<template>
  <div class="dash-wrap">
    <!--核心指标卡-->
    <div class="stat-grid">
      <div class="stat-card money">
        <div class="s-ico"><el-icon><Money/></el-icon></div>
        <div class="s-body">
          <p class="s-label">今日实收</p>
          <p class="s-value">￥{{ money(stat.todayRevenue) }}</p>
        </div>
      </div>
      <div class="stat-card money">
        <div class="s-ico"><el-icon><Wallet/></el-icon></div>
        <div class="s-body">
          <p class="s-label">本月实收</p>
          <p class="s-value">￥{{ money(stat.monthRevenue) }}</p>
        </div>
      </div>
      <div class="stat-card warn" @click="$router.push('/backstage/y-billList')">
        <div class="s-ico"><el-icon><Tickets/></el-icon></div>
        <div class="s-body">
          <p class="s-label">待收费账单</p>
          <p class="s-value">{{ stat.pendingBills }}</p>
        </div>
      </div>
      <div class="stat-card info" @click="$router.push('/backstage/z-hospital')">
        <div class="s-ico"><el-icon><HomeFilled/></el-icon></div>
        <div class="s-body">
          <p class="s-label">在院宠物</p>
          <p class="s-value">{{ stat.inHospital }}</p>
        </div>
      </div>
    </div>

    <!--待办小卡-->
    <div class="todo-grid">
      <div class="todo-card">
        <p class="t-label">待检验化验单</p>
        <p class="t-value">{{ stat.pendingLab }}</p>
        <el-link :underline="false" type="primary" @click="$router.push('/backstage/z-labResult')">去处理</el-link>
      </div>
      <div class="todo-card">
        <p class="t-label">待检查影像单</p>
        <p class="t-value">{{ stat.pendingImage }}</p>
        <el-link :underline="false" type="primary" @click="$router.push('/backstage/z-imageResult')">去处理</el-link>
      </div>
      <div class="todo-card">
        <p class="t-label">近30天应接种</p>
        <p class="t-value">{{ stat.dueVaccine }}</p>
        <el-link :underline="false" type="primary" @click="$router.push('/backstage/z-immune')">查看档案</el-link>
      </div>
    </div>

    <!--趋势 + 医生接诊top-->
    <div class="chart-row">
      <el-card shadow="never" class="chart-card">
        <template #header><b>近7日实收趋势</b></template>
        <div v-if="stat.revenueTrend.length" class="bar-chart">
          <div v-for="d in stat.revenueTrend" :key="d.date" class="bar-col">
            <div class="bar-track">
              <div class="bar-fill" :style="{height: barHeight(d.amount) + '%'}"></div>
            </div>
            <span class="bar-val">￥{{ money(d.amount) }}</span>
            <span class="bar-date">{{ d.date.slice(5) }}</span>
          </div>
        </div>
        <el-empty v-else description="近7日暂无已收费账单"/>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header><b>医生接诊数 Top</b></template>
        <div v-if="stat.doctorTop.length" class="doctor-rank">
          <div v-for="(d, i) in stat.doctorTop" :key="d.name" class="rank-row">
            <span class="rank-no">{{ i + 1 }}</span>
            <span class="rank-name">{{ d.name || '未知医生' }}</span>
            <el-progress :percentage="rankPct(d.count)" :stroke-width="10" color="#67c23a" class="rank-bar"/>
            <span class="rank-count">{{ d.count }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无接诊数据"/>
      </el-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {HomeFilled, Money, Tickets, Wallet} from "@element-plus/icons-vue";
import {StatOverview} from "@/model/entiy/StatOverview";
import {reqStatOverview} from "@/request/StatisticsApi";

const stat = ref<StatOverview>({
  todayRevenue: 0, monthRevenue: 0, pendingBills: 0, inHospital: 0,
  pendingLab: 0, pendingImage: 0, dueVaccine: 0, revenueTrend: [], doctorTop: []
})

const money = (n: number | null | undefined): string =>
    (n ?? 0).toFixed(2)

// 柱状高度：相对最大值归一化，至少给一点点
const maxAmt = (): number =>
    Math.max(1, ...stat.value.revenueTrend.map(d => d.amount))
const barHeight = (v: number): number =>
    Math.round((v / maxAmt()) * 100)

const rankPct = (v: number): number =>
    Math.min(100, Math.round((v / Math.max(1, ...stat.value.doctorTop.map(d => d.count))) * 100))

onMounted(() => {
  reqStatOverview().then(res => {
    stat.value = res || stat.value
  }).catch(() => {
    // 静默，保留默认空数据
  })
})
</script>

<style scoped>
.dash-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: default;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.stat-card.money .s-ico { background: #f56c6c; }
.stat-card.warn .s-ico { background: #e6a23c; }
.stat-card.info .s-ico { background: #409eff; }
.stat-card.warn, .stat-card.info { cursor: pointer; }
.s-ico {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  color: #fff;
  font-size: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.s-label {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.s-value {
  margin: 4px 0 0;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.todo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.todo-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.t-label {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.t-value {
  margin: 6px 0 4px;
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}
.chart-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
}
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  height: 220px;
  padding-top: 10px;
}
.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  height: 100%;
}
.bar-track {
  width: 100%;
  max-width: 46px;
  flex: 1;
  display: flex;
  align-items: flex-end;
  background: #f2f3f5;
  border-radius: 6px 6px 0 0;
}
.bar-fill {
  width: 100%;
  background: linear-gradient(180deg, #67c23a, #409eff);
  border-radius: 6px 6px 0 0;
  min-height: 4px;
}
.bar-val {
  font-size: 11px;
  color: #909399;
}
.bar-date {
  font-size: 12px;
  color: #606266;
}
.doctor-rank {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rank-no {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.rank-name {
  width: 72px;
  font-size: 13px;
  color: #303133;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rank-bar { flex: 1; }
.rank-count {
  font-size: 13px;
  color: #606266;
  width: 28px;
  text-align: right;
}
</style>