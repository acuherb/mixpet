<template>
  <el-dialog :model-value="modelValue" title="宠物健康档案" top="5vh" width="780px"
             @update:modelValue="(v: boolean) => emit('update:modelValue', v)">
    <div v-if="h && h.pet">
      <el-descriptions :column="3" border size="small" title="基本信息">
        <el-descriptions-item label="宠物">{{ h.pet.petName }}</el-descriptions-item>
        <el-descriptions-item label="品种">{{ h.pet.petVariety || '—' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ h.pet.petSex ? '公' : '母' }}</el-descriptions-item>
        <el-descriptions-item label="生日">{{ h.pet.petBirthday ? h.pet.petBirthday.slice(0, 10) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="绝育">{{ h.pet.petSterilization ? '已绝育' : '未绝育' }}</el-descriptions-item>
        <el-descriptions-item label="芯片">{{ h.pet.petChip || '—' }}</el-descriptions-item>
        <el-descriptions-item label="过敏史" :span="3">{{ h.pet.petAllergy || '无' }}</el-descriptions-item>
        <el-descriptions-item label="既往病史" :span="3">{{ h.pet.petHistory || '无' }}</el-descriptions-item>
      </el-descriptions>

      <div class="summary-row">
        <span>就诊 <b>{{ h.visitCount }}</b> 次</span>
        <span>最近就诊：{{ h.lastVisitDate || '—' }}</span>
        <span>最近诊断：{{ h.lastDiagnosis || '—' }}</span>
        <span>下次免疫：<b v-if="h.nextVaccineDate" style="color:#e6a23c">{{ h.nextVaccineDate }}</b><span v-else>—</span></span>
      </div>

      <el-divider content-position="left">体重曲线</el-divider>
      <div v-if="h.weightTrend.length" class="wt-chart">
        <div v-for="(w, i) in h.weightTrend" :key="i" class="wt-col">
          <div class="wt-track"><div class="wt-fill" :style="{height: wtH(w.weight) + '%'}"></div></div>
          <span class="wt-val">{{ w.weight }}</span>
          <span class="wt-date">{{ (w.date || '').slice(5) }}</span>
        </div>
      </div>
      <el-empty v-else :image-size="40" description="暂无体重记录"/>

      <el-divider content-position="left">就诊史</el-divider>
      <el-table :data="h.visits" border max-height="220" size="small">
        <el-table-column label="日期" width="110">
          <template #default="s">{{ (s.row.createTime || '').replace('T', ' ').slice(0, 10) }}</template>
        </el-table-column>
        <el-table-column label="诊断" min-width="160" prop="diagnosis" show-overflow-tooltip/>
        <el-table-column label="医生" min-width="90" prop="doctorName" show-overflow-tooltip/>
        <el-table-column label="复诊日" min-width="100" prop="revisitDate"/>
      </el-table>
      <el-empty v-if="!h.visits.length" :image-size="40" description="暂无就诊记录"/>

      <el-divider content-position="left">免疫记录</el-divider>
      <el-table :data="h.immune" border max-height="200" size="small">
        <el-table-column label="疫苗" min-width="140" prop="drugName" show-overflow-tooltip/>
        <el-table-column label="针次" prop="doseCnt" width="60"/>
        <el-table-column label="接种时间" width="150">
          <template #default="s">{{ fmt(s.row.vaccinateTime) }}</template>
        </el-table-column>
        <el-table-column label="下一针" width="110">
          <template #default="s">{{ s.row.nextTime ? s.row.nextTime.slice(0, 10) : '—' }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!h.immune.length" :image-size="40" description="暂无免疫记录"/>

      <el-divider content-position="left">住院记录</el-divider>
      <el-table :data="h.hospital" border max-height="200" size="small">
        <el-table-column label="床位" prop="roomNo" width="90"/>
        <el-table-column label="入院" width="150">
          <template #default="s">{{ fmt(s.row.inTime) }}</template>
        </el-table-column>
        <el-table-column label="出院" width="150">
          <template #default="s">{{ fmt(s.row.outTime) }}</template>
        </el-table-column>
        <el-table-column label="天数" prop="days" width="60"/>
        <el-table-column label="状态" width="90">
          <template #default="s">
            <el-tag :type="s.row.status ? 'success' : 'warning'" size="small">{{ s.row.status ? '已出院' : '住院中' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!h.hospital.length" :image-size="40" description="暂无住院记录"/>
    </div>
    <el-empty v-else description="暂无档案或无权查看"/>
  </el-dialog>
</template>

<script lang="ts" setup>
import {ref, watch} from 'vue'
import {PetHealth} from "@/model/entiy/PetHealth";
import {reqPetHealth} from "@/request/PetApi";

const props = defineProps<{ modelValue: boolean; petId: number | null }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const h = ref<PetHealth | null>(null)
const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)
const maxW = (): number => Math.max(1, ...(h.value?.weightTrend.map(w => w.weight) || [1]))
const wtH = (v: number): number => Math.round((v / maxW()) * 100)

watch(() => [props.modelValue, props.petId], (nv) => {
  const vis = nv[0]
  const id = nv[1] as number | null
  if (vis && id) reqPetHealth(id).then(res => { h.value = res || null })
})
</script>

<style scoped>
.summary-row { display: flex; flex-wrap: wrap; gap: 18px; margin: 12px 0; font-size: 13px; color: #606266; }
.wt-chart { display: flex; align-items: flex-end; gap: 10px; height: 150px; }
.wt-col { flex: 1; min-width: 30px; display: flex; flex-direction: column; align-items: center; gap: 3px; height: 100%; }
.wt-track { width: 100%; max-width: 34px; flex: 1; display: flex; align-items: flex-end; background: #f2f3f5; border-radius: 5px 5px 0 0; }
.wt-fill { width: 100%; background: linear-gradient(180deg, #409eff, #67c23a); border-radius: 5px 5px 0 0; min-height: 3px; }
.wt-val { font-size: 10px; color: #909399; }
.wt-date { font-size: 10px; color: #606266; }
</style>
