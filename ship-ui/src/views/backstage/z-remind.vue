<template>
  <div>
    <PageHead>随访提醒</PageHead>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <b>应复诊</b>
            <span style="color:#909399; font-size:12px; margin-left:8px">复诊日期在30天内（含逾期），共 {{ remind.revisits.length }} 条</span>
          </template>
          <el-table :data="remind.revisits" max-height="calc(100vh - 260px)" stripe size="small">
            <el-table-column label="宠物" min-width="90" prop="petName" show-overflow-tooltip/>
            <el-table-column label="客户" min-width="90" prop="clientName" show-overflow-tooltip/>
            <el-table-column label="诊断" min-width="120" prop="diagnosis" show-overflow-tooltip/>
            <el-table-column label="复诊日期" width="120" prop="revisitDate"/>
            <el-table-column label="状态" width="90">
              <template #default="scope">
                <el-tag v-if="isRevisitOverdue(scope.row.revisitDate)" size="small" type="danger">已逾期</el-tag>
                <el-tag v-else size="small" type="warning">临近</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!remind.revisits.length" description="近期无应复诊"/>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <b>应接种</b>
            <span style="color:#909399; font-size:12px; margin-left:8px">下一针在30天内，共 {{ remind.vaccines.length }} 条</span>
          </template>
          <el-table :data="remind.vaccines" max-height="calc(100vh - 260px)" stripe size="small">
            <el-table-column label="宠物" min-width="90" prop="petName" show-overflow-tooltip/>
            <el-table-column label="客户" min-width="90" prop="clientName" show-overflow-tooltip/>
            <el-table-column label="疫苗" min-width="110" prop="drugName" show-overflow-tooltip/>
            <el-table-column label="针次" width="55" prop="doseCnt"/>
            <el-table-column label="下一针" width="150">
              <template #default="scope">{{ fmt(scope.row.nextTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="scope">
                <el-tag v-if="isVaccineOverdue(scope.row.nextTime)" size="small" type="danger">已逾期</el-tag>
                <el-tag v-else size="small" type="warning">临近</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!remind.vaccines.length" description="近期无应接种"/>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive} from 'vue'
import PageHead from "@/components/row/PageHead.vue";
import {Remind} from "@/model/entiy/Remind";
import {reqRemind} from "@/request/RemindApi";

const remind = reactive<Remind>({revisits: [], vaccines: []})

onMounted(() => {
  reqRemind().then(res => {
    remind.revisits = res.revisits || []
    remind.vaccines = res.vaccines || []
  })
})

const todayMidnight = (): Date => {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
}
const isRevisitOverdue = (d: string | null): boolean => !!d && new Date(d) < todayMidnight()
const isVaccineOverdue = (t: string | null): boolean => !!t && new Date(t) < new Date()
const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)
</script>
