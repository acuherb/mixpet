<template>
  <!-- 今日工作流：待接诊挂号 + 接诊中文案按时间混排成一条线 -->
  <div v-if="items.length" class="tl-list">
    <div v-for="it in items" :key="it.key" :class="['tl-item', it.state]">
      <div class="tl-rail">
        <span class="tl-dot"/>
      </div>
      <div class="tl-card">
        <div class="tl-head">
          <span class="tl-time">{{ moveT(it.time) }}</span>
          <el-tag v-if="it.state === 'pending'" size="small" round type="warning" effect="light">待接诊</el-tag>
          <el-tag v-else-if="it.state === 'treating'" size="small" round type="danger" effect="light">接诊中</el-tag>
          <el-tag v-else size="small" round type="info" effect="light">已处理</el-tag>
        </div>
        <div class="tl-body">
          <span class="tl-pet">{{ it.petName }}</span>
          <span class="tl-owner">{{ it.clientName }}</span>
          <span class="tl-info" :title="it.info || ''">{{ it.info || '—' }}</span>
        </div>
        <el-button v-if="it.state !== 'done'" size="small" round :type="it.state === 'pending' ? 'primary' : 'warning'"
                   @click.stop="goTreat(it.appointmentId)">
          {{ it.state === 'pending' ? '去接诊' : '继续开单' }}
        </el-button>
      </div>
    </div>
  </div>
  <p v-else class="todo-empty">
    <el-icon><CircleCheck/></el-icon>
    今日暂无就诊安排
  </p>
</template>

<script lang="ts" setup>
import {computed} from 'vue'
import {AppointDto2} from "@/model/DO/AppointDto2";
import {RecordDto} from "@/model/DO/RecordDto";
import {moveT} from "@/utils/TimeUtil";

const props = defineProps<{
  /** 挂号单（含已处理；已处理的若对应接诊中文案则以病历条目呈现，避免重复） */
  appoints: AppointDto2[]
  /** 接诊中的病历（未完结） */
  records: RecordDto[]
  /** 直达接诊：带挂号单 id */
  goTreat: (appointmentId: number) => void
}>()

interface TlItem {
  key: string
  time: string
  state: 'pending' | 'treating' | 'done'
  petName: string
  clientName: string
  info: string
  appointmentId: number
}

/** 合并排序：未处理挂号(按预约时间) + 接诊中文案(按接诊时间)，已处理挂号跳过 */
const items = computed<TlItem[]>(() => {
  const list: TlItem[] = []
  props.appoints.filter(a => !a.isSuc).forEach(a => {
    list.push({
      key: 'a' + a.appointmentId,
      time: a.appointmentDate || '',
      state: 'pending',
      petName: a.petName || '',
      clientName: a.clientName || '',
      info: a.appointmentInfo || '',
      appointmentId: a.appointmentId
    })
  })
  props.records.forEach(r => {
    list.push({
      key: 'r' + r.recordId,
      time: r.createTime || '',
      state: 'treating',
      petName: r.petName || '',
      clientName: r.clientName || '',
      info: r.diagnosis || '（未写诊断）',
      appointmentId: (r as any).appointmentId ?? 0
    })
  })
  return list.sort((x, y) => (x.time || '').localeCompare(y.time || ''))
})
</script>

<style lang="scss" scoped>
.tl-list {
  position: relative;
  padding-left: 4px;
}
.tl-item {
  display: flex;
  gap: 10px;
  padding-bottom: 12px;

  /* 时间轴：圆点 + 竖线（最后一项不垂线） */
  .tl-rail {
    width: 14px;
    flex-shrink: 0;
    display: flex;
    justify-content: center;
    position: relative;

    &::before {
      content: "";
      position: absolute;
      top: 14px;
      bottom: -12px;
      width: 2px;
      background: #ebeef5;
    }
  }
  &:last-child .tl-rail::before { display: none; }

  .tl-dot {
    width: 10px;
    height: 10px;
    margin-top: 6px;
    border-radius: 50%;
    background: #c0c4cc;
    box-shadow: 0 0 0 3px #f2f3f5;
  }
  &.pending .tl-dot { background: #E6A23C; box-shadow: 0 0 0 3px #fdf3e3; }
  &.treating .tl-dot { background: #F56C6C; box-shadow: 0 0 0 3px #fde8e8; }

  .tl-card {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    background: #fafbfc;
    border: 1px solid #ebeef5;
    border-radius: 10px;
    padding: 8px 12px;
    transition: border-color .15s;

    &:hover { border-color: #c6e2ff; }
  }
  &.pending .tl-card { border-left: 3px solid #E6A23C; }
  &.treating .tl-card { border-left: 3px solid #F56C6C; }

  .tl-head {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .tl-time {
    font-size: 12px;
    color: #606266;
    font-variant-numeric: tabular-nums;
  }
  .tl-pet { font-size: 13px; font-weight: 700; color: #1F2D3D; }
  .tl-owner { font-size: 12px; color: #909399; }
  .tl-info {
    flex: 1;
    min-width: 80px;
    font-size: 12px;
    color: #606266;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .el-button { margin-left: auto; }
}
</style>
