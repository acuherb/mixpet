<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <b>医生排班</b>
        <span class="tip">设置医生每周几上/下午是否出诊及可约号源；单时段最多 20 号，挂号按时段校验容量</span>
      </template>
      <div class="sel-row">
        <span>选择医生：</span>
        <el-select v-model="doctorId" filterable placeholder="选择医生" style="width: 220px" @change="loadSchedule">
          <el-option v-for="d in doctors" :key="d.roleId" :label="d.roleName" :value="d.roleId"/>
        </el-select>
        <el-button :disabled="!doctorId" style="margin-left: 12px" type="primary" @click="saveSchedule">保存排班</el-button>
      </div>
      <el-table :data="rows" border size="small" style="margin-top: 12px">
        <el-table-column label="星期" width="90">
          <template #default="s">{{ s.row.label }}</template>
        </el-table-column>
        <el-table-column label="上午出诊" width="110">
          <template #default="s"><el-switch v-model="s.row.amActive"/></template>
        </el-table-column>
        <el-table-column label="上午号源" width="140">
          <template #default="s">
            <el-input-number v-model="s.row.amCount" :disabled="!s.row.amActive" :max="20" :min="0" size="small"/>
          </template>
        </el-table-column>
        <el-table-column label="下午出诊" width="110">
          <template #default="s"><el-switch v-model="s.row.pmActive"/></template>
        </el-table-column>
        <el-table-column label="下午号源" width="140">
          <template #default="s">
            <el-input-number v-model="s.row.pmCount" :disabled="!s.row.pmActive" :max="20" :min="0" size="small"/>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header><b>排班总览</b></template>
      <el-table :data="overview" border size="small">
        <el-table-column label="医生" min-width="120" prop="doctorName"/>
        <el-table-column label="出诊日" min-width="300">
          <template #default="s">
            <el-tag v-for="(d, i) in s.row.days" :key="i" size="small" style="margin-right:4px">{{ d }}</el-tag>
            <span v-if="!s.row.days.length" style="color:#999">未排班</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {reqDoctorName} from "@/request/DoctorApi";
import {reqSaveSchedule, reqScheduleAll, reqScheduleByDoctor} from "@/request/ScheduleApi";
import {weekdays, DoctorSchedule} from "@/model/entiy/DoctorSchedule";
import {NameDo} from "@/model/DO/NameDo";
import {ElMessage} from "element-plus";

const doctors = ref<NameDo[]>([])
const doctorId = ref<number | null>(null)
const rows = ref<{ label: string; weekday: number; amActive: boolean; amCount: number; pmActive: boolean; pmCount: number }[]>([])
const overview = ref<{ doctorId: number; doctorName: string; days: string[] }[]>([])

const initRows = (): void => {
  rows.value = weekdays.map(w => ({
    label: w.label, weekday: w.v, amActive: false, amCount: 10, pmActive: false, pmCount: 10
  }))
}
const loadSchedule = (): void => {
  initRows()
  if (!doctorId.value) return
  reqScheduleByDoctor(doctorId.value).then(list => {
    for (const s of list || []) {
      const row = rows.value.find(r => r.weekday === s.weekday)
      if (!row) continue
      if (s.period === 1) {
        row.amActive = !!s.active
        row.amCount = s.maxCount ?? 10
      } else {
        row.pmActive = !!s.active
        row.pmCount = s.maxCount ?? 10
      }
    }
  })
}
const saveSchedule = (): void => {
  if (!doctorId.value) return
  const list: DoctorSchedule[] = []
  for (const r of rows.value) {
    list.push({doctorId: doctorId.value!, weekday: r.weekday, period: 1, maxCount: r.amCount, active: r.amActive})
    list.push({doctorId: doctorId.value!, weekday: r.weekday, period: 2, maxCount: r.pmCount, active: r.pmActive})
  }
  reqSaveSchedule(list).then(res => {
    if (res.code === 1) {
      ElMessage.success('排班已保存')
      loadOverview()
    } else ElMessage.error(res.msg || '保存失败')
  })
}
const loadOverview = (): void => {
  reqScheduleAll().then(list => {
    const map = new Map<number, { doctorId: number; doctorName: string; days: string[] }>()
    for (const s of list || []) {
      if (!s.active) continue
      let e = map.get(s.doctorId)
      if (!e) {
        e = {doctorId: s.doctorId, doctorName: s.doctorName || ('医生#' + s.doctorId), days: []}
        map.set(s.doctorId, e)
      }
      const w = weekdays.find(x => x.v === s.weekday)?.label || ''
      e.days.push(w + (s.period === 1 ? '上' : '下'))
    }
    overview.value = [...map.values()]
  })
}

onMounted(() => {
  reqDoctorName().then(res => { doctors.value = res || [] })
  initRows()
  loadOverview()
})
</script>

<style scoped>
.tip { margin-left: 12px; font-size: 12px; color: #909399; }
.sel-row { display: flex; align-items: center; }
</style>
