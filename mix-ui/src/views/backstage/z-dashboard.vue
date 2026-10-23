<template>
  <div class="dash-wrap">
    <!--核心指标卡（第一行：营收）-->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="s-ico bg-red"><el-icon><Money/></el-icon></div>
        <div class="s-body">
          <p class="s-label">今日实收</p>
          <p class="s-value">￥{{ money(stat.todayRevenue) }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="s-ico bg-orange"><el-icon><Wallet/></el-icon></div>
        <div class="s-body">
          <p class="s-label">本月实收</p>
          <p class="s-value">￥{{ money(stat.monthRevenue) }}</p>
        </div>
      </div>
      <div class="stat-card" @click="go('/backstage/y-appointmentList')">
        <div class="s-ico bg-blue"><el-icon><Tickets/></el-icon></div>
        <div class="s-body">
          <p class="s-label">今日挂号</p>
          <p class="s-value">{{ stat.todayAppoint }} <span class="s-unit">单</span></p>
        </div>
      </div>
      <div class="stat-card" @click="go('/backstage/y-appointmentList')">
        <div class="s-ico bg-green"><el-icon><FirstAidKit/></el-icon></div>
        <div class="s-body">
          <p class="s-label">今日接诊</p>
          <p class="s-value">{{ stat.todayRecord }} <span class="s-unit">单</span></p>
        </div>
      </div>
    </div>

    <!--核心指标卡（第二行：运营）-->
    <div class="stat-grid">
      <div class="stat-card" @click="go('/backstage/y-billList')">
        <div class="s-ico bg-orange"><el-icon><Money/></el-icon></div>
        <div class="s-body">
          <p class="s-label">待收费账单</p>
          <p class="s-value">{{ stat.pendingBills }} <span class="s-unit">笔</span></p>
        </div>
      </div>
      <div class="stat-card" @click="go('/backstage/y-billList')">
        <div class="s-ico bg-teal"><el-icon><Box/></el-icon></div>
        <div class="s-body">
          <p class="s-label">待发药</p>
          <p class="s-value">{{ stat.pendingDispense }} <span class="s-unit">笔</span></p>
        </div>
      </div>
      <div class="stat-card" @click="go('/backstage/z-hospital')">
        <div class="s-ico bg-blue"><el-icon><HomeFilled/></el-icon></div>
        <div class="s-body">
          <p class="s-label">在院宠物</p>
          <p class="s-value">{{ stat.inHospital }} <span class="s-unit">只</span></p>
        </div>
      </div>
      <div class="stat-card" @click="go('/backstage/y-clientList')">
        <div class="s-ico bg-purple"><el-icon><User/></el-icon></div>
        <div class="s-body">
          <p class="s-label">客户总数</p>
          <p class="s-value">{{ stat.clientTotal }} <span class="s-unit">位</span></p>
        </div>
      </div>
    </div>

    <!--待办 + 库存预警-->
    <div class="todo-grid">
      <div class="todo-card">
        <div class="t-head">
          <div class="t-ico bg-blue"><el-icon><DataLine/></el-icon></div>
          <div>
            <p class="t-label">待检验化验单</p>
            <p class="t-value">{{ stat.pendingLab }}</p>
          </div>
        </div>
        <el-link :underline="false" type="primary" @click="go('/backstage/z-examResult?tab=lab')">去处理 ›</el-link>
      </div>
      <div class="todo-card">
        <div class="t-head">
          <div class="t-ico bg-purple"><el-icon><Picture/></el-icon></div>
          <div>
            <p class="t-label">待检查影像单</p>
            <p class="t-value">{{ stat.pendingImage }}</p>
          </div>
        </div>
        <el-link :underline="false" type="primary" @click="go('/backstage/z-examResult?tab=image')">去处理 ›</el-link>
      </div>
      <div class="todo-card">
        <div class="t-head">
          <div class="t-ico bg-green"><el-icon><CirclePlusFilled/></el-icon></div>
          <div>
            <p class="t-label">近30天应接种</p>
            <p class="t-value">{{ stat.dueVaccine }}</p>
          </div>
        </div>
        <el-link :underline="false" type="primary" @click="go('/backstage/z-immune')">查看档案 ›</el-link>
      </div>
      <div class="todo-card" @click="go('/backstage/z-recordList')">
        <div class="t-head">
          <div class="t-ico bg-red"><el-icon><Tickets/></el-icon></div>
          <div>
            <p class="t-label">接诊中病历（未完结）</p>
            <p class="t-value">{{ todo.treating }}</p>
          </div>
        </div>
        <el-link :underline="false" type="danger" @click="go('/backstage/z-recordList')">去跟进 ›</el-link>
      </div>
    </div>

    <div class="todo-grid two">
      <!-- 待审核顾客：自助注册待放行，点击直达客户管理自动筛选 -->
      <div class="todo-card warn-card" @click="go('/backstage/y-clientList?audit=0')">
        <div class="t-head">
          <div class="t-ico bg-orange"><el-icon><UserFilled/></el-icon></div>
          <div>
            <p class="t-label">待审核顾客（自助注册）</p>
            <p class="t-value">{{ todo.clientPending }}</p>
          </div>
        </div>
        <el-link :underline="false" type="warning" @click="go('/backstage/y-clientList?audit=0')">去审核 ›</el-link>
      </div>
      <div class="todo-card warn-card" @click="go('/backstage/y-drugList')">
        <div class="t-head">
          <div class="t-ico bg-orange"><el-icon><WarningFilled/></el-icon></div>
          <div>
            <p class="t-label">低库存药品</p>
            <p class="t-value">{{ stat.lowStock }}</p>
          </div>
        </div>
        <el-link :underline="false" type="warning" @click="go('/backstage/y-drugList')">去补货 ›</el-link>
      </div>
      <div class="todo-card danger-card" @click="go('/backstage/y-drugList')">
        <div class="t-head">
          <div class="t-ico bg-red"><el-icon><Calendar/></el-icon></div>
          <div>
            <p class="t-label">近效期药品（90天）</p>
            <p class="t-value">{{ stat.expiringDrug }}</p>
          </div>
        </div>
        <el-link :underline="false" type="danger" @click="go('/backstage/y-drugList')">去处理 ›</el-link>
      </div>
    </div>

    <!--趋势 + 医生接诊top-->
    <div class="chart-row">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="c-header">
            <el-icon class="c-ico bg-green"><TrendCharts/></el-icon>
            <b>近7日实收趋势</b>
          </div>
        </template>
        <MiniBar :items="stat.revenueTrend.map(d => ({name: d.date.slice(5), value: d.amount, label: '￥' + money(d.amount)}))"
                 empty-text="近7日暂无已收费账单"/>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="c-header">
            <el-icon class="c-ico bg-orange"><Trophy/></el-icon>
            <b>医生接诊数 Top</b>
          </div>
        </template>
        <div v-if="stat.doctorTop.length" class="doctor-rank">
          <div v-for="(d, i) in stat.doctorTop" :key="d.name" class="rank-row">
            <span class="rank-no" :class="'no-' + (i + 1)">{{ i + 1 }}</span>
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
import {useRouter} from 'vue-router'
import {
  Box, Calendar, CirclePlusFilled, DataLine, FirstAidKit, HomeFilled, Money, Picture,
  Tickets, TrendCharts, Trophy, User, UserFilled, Wallet, WarningFilled
} from "@element-plus/icons-vue";
import {StatOverview} from "@/model/entiy/StatOverview";
import {reqStatOverview} from "@/request/StatisticsApi";
import {reqRecordTodo} from "@/request/RecordApi";
import {RecordTodo} from "@/model/VO/RecordFull";
import MiniBar from "@/components/chart/MiniBar.vue";

const router = useRouter()
const go = (path: string): void => { router.push(path) }

const stat = ref<StatOverview>({
  todayRevenue: 0, monthRevenue: 0, pendingBills: 0, inHospital: 0,
  pendingLab: 0, pendingImage: 0, dueVaccine: 0,
  todayAppoint: 0, todayRecord: 0, clientTotal: 0, pendingDispense: 0, lowStock: 0, expiringDrug: 0,
  revenueTrend: [], doctorTop: []
})

const money = (n: number | null | undefined): string =>
    (n ?? 0).toFixed(2)

const rankPct = (v: number): number =>
    Math.min(100, Math.round((v / Math.max(1, ...stat.value.doctorTop.map(d => d.count))) * 100))

// 诊疗待办（卡壳就诊：接诊中/待出结果/待手术/待随访）
const todo = ref<RecordTodo>({treating: 0, labPending: 0, imagePending: 0, surgeryPending: 0, followPending: 0, clientPending: 0})

onMounted(() => {
  reqStatOverview().then(res => {
    stat.value = res || stat.value
  }).catch(() => {
    // 静默，保留默认空数据
  })
  reqRecordTodo().then(res => { if (res) todo.value = res }).catch(() => { /* 静默 */ })
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
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: transform 0.18s, box-shadow 0.18s;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}
.s-ico {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  color: #fff;
  font-size: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.bg-red { background: linear-gradient(135deg, #f78ca0, #f56c6c); }
.bg-orange { background: linear-gradient(135deg, #ffb74d, #f57c00); }
.bg-blue { background: linear-gradient(135deg, #64b5f6, #1976d2); }
.bg-green { background: linear-gradient(135deg, #81c784, #43a047); }
.bg-teal { background: linear-gradient(135deg, #4db6ac, #00897b); }
.bg-purple { background: linear-gradient(135deg, #ba68c8, #8e24aa); }
.s-body { min-width: 0; }
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
  white-space: nowrap;
}
.s-unit { font-size: 13px; font-weight: 400; color: #909399; }
.todo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.todo-grid.two { grid-template-columns: repeat(2, 1fr); }
.todo-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: transform 0.18s, box-shadow 0.18s;
}
.todo-card:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0, 0, 0, 0.09); }
.warn-card { border-left: 4px solid #e6a23c; }
.danger-card { border-left: 4px solid #f56c6c; }
.t-head { display: flex; align-items: center; gap: 12px; }
.t-ico {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  color: #fff;
  font-size: 21px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.t-label {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.t-value {
  margin: 2px 0 0;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.chart-row {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
}
.c-header { display: flex; align-items: center; gap: 10px; }
.c-ico {
  width: 30px; height: 30px; border-radius: 8px; color: #fff; font-size: 16px;
  display: flex; align-items: center; justify-content: center;
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
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #c0c4cc;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.rank-no.no-1 { background: linear-gradient(135deg, #ffd54f, #ff9800); }
.rank-no.no-2 { background: linear-gradient(135deg, #b0bec5, #78909c); }
.rank-no.no-3 { background: linear-gradient(135deg, #d7a86e, #a1887f); }
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