<template>
  <div class="reg-picker">
    <!-- 一框通查：宠物名 / 主人姓名 / 手机号 / 宠物卡号（老系统「宠物卡号」录入习惯） -->
    <el-select
        :model-value="petId ?? undefined"
        clearable filterable remote
        :remote-method="doSearch"
        :loading="searching"
        placeholder="输入 宠物名 / 主人姓名 / 手机号 / 卡号"
        style="width: 100%"
        @change="onPick"
        @clear="onClear"
        @focus="onFocus"
    >
      <el-option v-for="p in options" :key="p.petId" :value="p.petId!"
                 :label="optLabel(p)">
        <div class="opt-row">
          <span class="opt-pet">{{ p.petName }}</span>
          <span class="opt-meta">{{ p.petVariety || '—' }} · {{ p.clientName || '无主' }}</span>
          <span class="opt-tel">{{ p.clientTel || '' }}</span>
          <span class="opt-no">卡号 {{ p.petId }}</span>
        </div>
      </el-option>
    </el-select>

    <!-- dense：医生端挂号弹窗，只保留一行档案摘要 -->
    <div v-if="archive && dense" class="reg-brief">
      <span class="rb-item"><b>{{ archive.pet.petName }}</b></span>
      <span class="rb-item">{{ archive.pet.petVariety || '—' }} · {{ archive.pet.petSex == null ? '—' : (archive.pet.petSex ? '公' : '母') }}</span>
      <span class="rb-item">{{ getAge(archive.pet.petBirthday) }}</span>
      <span class="rb-item">主人 {{ archive.client?.clientName || '—' }}</span>
      <span class="rb-item">{{ archive.client?.clientTel || '—' }}</span>
      <span class="rb-item">余额 ￥{{ (archive.client?.clientBalance ?? 0).toFixed(2) }}</span>
    </div>

    <!-- 完整版：左侧档案带出（只读） + 右侧三张固定信息卡 -->
    <div v-if="archive && !dense" class="reg-body">
      <div class="reg-archive">
        <div class="ra-tit">档案带出（只读）</div>
        <div class="ra-grid">
          <div class="ra-field"><span class="k">宠物</span><span class="v">{{ archive.pet.petName }}</span></div>
          <div class="ra-field"><span class="k">卡号</span><span class="v">{{ archive.pet.petId }}</span></div>
          <div class="ra-field"><span class="k">品种</span><span class="v">{{ archive.pet.petVariety || '—' }}</span></div>
          <div class="ra-field"><span class="k">性别</span><span class="v">{{ archive.pet.petSex == null ? '—' : (archive.pet.petSex ? '公' : '母') }}</span></div>
          <div class="ra-field"><span class="k">年龄</span><span class="v">{{ getAge(archive.pet.petBirthday) }}</span></div>
          <div class="ra-field"><span class="k">体重</span><span class="v">{{ archive.stat.weight != null ? archive.stat.weight + ' kg' : '—' }}</span></div>
          <div class="ra-field"><span class="k">主人</span><span class="v">{{ archive.client?.clientName || '—' }}</span></div>
          <div class="ra-field"><span class="k">电话</span><span class="v">{{ archive.client?.clientTel || '—' }}</span></div>
          <div class="ra-field ra-wide"><span class="k">过敏/病史</span><span class="v">{{ archive.pet.petAllergy || archive.pet.petHistory || '无' }}</span></div>
        </div>
      </div>
      <div class="reg-cards">
        <div class="rc-card">
          <div class="rc-tit">今日医生</div>
          <template v-if="todayDoctors.length">
            <div v-for="d in todayDoctors" :key="d.doctorId + '-' + d.period" class="rc-line">
              <span class="rc-doc">{{ d.doctorName }}</span>
              <span class="rc-meta">{{ d.period === 1 ? '上午' : '下午' }}</span>
              <span class="rc-meta">余 {{ d.maxCount }}</span>
            </div>
          </template>
          <div v-else class="rc-empty">{{ scheduleFailed ? '排班加载失败' : '今日无排班' }}</div>
        </div>
        <div class="rc-card">
          <div class="rc-tit">上次就诊</div>
          <template v-if="archive.stat.lastVisit">
            <div class="rc-line"><span class="rc-doc">{{ shortT(archive.stat.lastVisit) }}</span></div>
            <div class="rc-line rc-diag">{{ archive.stat.lastDiagnosis || '—' }}</div>
            <div class="rc-meta">累计就诊 {{ archive.stat.visitCount }} 次</div>
          </template>
          <div v-else class="rc-empty">首次就诊</div>
        </div>
        <div class="rc-card">
          <div class="rc-tit">预付余额</div>
          <template v-if="archive.client">
            <div class="rc-balance">￥{{ (archive.client.clientBalance ?? 0).toFixed(2) }}</div>
            <div class="rc-meta">{{ (archive.client.clientBalance ?? 0) > 0 ? '可抵扣诊疗费用' : '余额不足，可现场充值' }}</div>
          </template>
          <div v-else class="rc-empty">无档案</div>
        </div>
      </div>
    </div>
    <div v-else-if="!dense" class="reg-hint">输入关键词检索宠物，选中后自动带出档案与今日医生</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import {reqPetArchive, reqPetSearch} from '@/request/PetApi'
import {reqScheduleAll} from '@/request/ScheduleApi'
import {PetDto} from '@/model/DO/PetDto'
import {PetArchive} from '@/model/DO/PetArchive'
import {DoctorSchedule} from '@/model/entiy/DoctorSchedule'
import {getAge} from '@/utils/TimeUtil'

/**
 * 挂号宠物选择器：
 * 老系统好用的核心是「卡号输入→带出全档」，本组件用新架构复刻该录入路径。
 * 一框通查（宠物名/主人姓名/手机号/卡号）→ 选中后左侧只读档案、右侧今日医生/上次就诊/预付余额三卡。
 */
const props = withDefaults(defineProps<{
  petId: number | null
  clientId: number | null
  /** 医生端窄弹窗：仅一行摘要，不展示三卡 */
  dense?: boolean
}>(), {dense: false})

const emit = defineEmits<{
  (e: 'update:petId', v: number | null): void
  (e: 'update:clientId', v: number | null): void
}>()

const searching = ref(false)
const options = ref<PetDto[]>([])
const archive = ref<PetArchive | null>(null)

const optLabel = (p: PetDto): string =>
    `${p.petName}（${p.clientName || '无主'}${p.clientTel ? ' ' + p.clientTel : ''}）`

// 供父组件（挂号凭证卡）读取已选宠物/主人档案
defineExpose({archive})

const doSearch = (kw: string): void => {
  // 空关键词后端返回最近 20 条有主宠物，用于下拉首次展开（不再出现空面板）
  const key = (kw || '').trim()
  searching.value = true
  reqPetSearch(key).then(res => {
    options.value = res || []
  }).catch(() => {
    options.value = []
    ElMessage.error('宠物检索失败，请检查网络或权限')
  }).finally(() => { searching.value = false })
}
// 点开下拉时展示一屏最近的候选，避免空面板
const onFocus = (): void => {
  if (!options.value.length) doSearch('')
}

const loadArchive = (petId: number): void => {
  reqPetArchive(petId).then(res => {
    archive.value = res
    // 修改挂号回显：确保下拉能显示已选宠物而非裸数字
    if (res?.pet && !options.value.some(o => o.petId === res.pet.petId)) {
      options.value = [{
        petId: res.pet.petId!,
        petName: res.pet.petName!,
        petVariety: res.pet.petVariety ?? null,
        petSex: res.pet.petSex ?? null,
        clientId: res.client?.clientId ?? null,
        clientName: res.client?.clientName ?? null,
        clientTel: res.client?.clientTel ?? null
      } as PetDto, ...options.value]
    }
  }).catch(() => {
    archive.value = null
    // 档案接口失败也补一个占位 option，避免下拉回显成裸 ID
    if (!options.value.some(o => o.petId === petId)) {
      options.value = [{
        petId, petName: '宠物 ' + petId, petVariety: null, petSex: null,
        clientId: props.clientId ?? null, clientName: null, clientTel: null
      } as unknown as PetDto, ...options.value]
    }
  })
}

const onPick = (val: number | undefined): void => {
  if (val == null) { onClear(); return }
  const p = options.value.find(o => o.petId === val)
  emit('update:petId', val)
  // 后端 AppointmentController 要求 clientId 非空；档案未带出时用外部已选值兜底
  emit('update:clientId', p?.clientId ?? archive.value?.client?.clientId ?? props.clientId ?? null)
  loadArchive(val)
}
const onClear = (): void => {
  archive.value = null
  emit('update:petId', null)
  emit('update:clientId', null)
}

// 修改挂号回显：外部带入 petId 时加载档案
watch(() => props.petId, (v) => {
  if (v != null && archive.value?.pet.petId !== v) loadArchive(v)
  if (v == null) archive.value = null
}, {immediate: true})

// 今日医生：排班总览按「今天周几」过滤（weekday 1周一…7周日；period 1上午 2下午）
const schedules = ref<DoctorSchedule[]>([])
const scheduleFailed = ref(false)
reqScheduleAll().then(res => { schedules.value = res || [] })
  .catch(() => { scheduleFailed.value = true })
const todayDoctors = computed<DoctorSchedule[]>(() => {
  const weekday = new Date().getDay() || 7
  return schedules.value
      .filter(s => s.active !== false && s.weekday === weekday)
      .sort((a, b) => a.period - b.period)
})

const shortT = (t: string): string => (t || '').replace('T', ' ').slice(0, 16)
</script>

<style lang="scss" scoped>
.reg-picker {
  width: 100%;
}
.opt-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;

  .opt-pet { font-weight: 600; }
  .opt-meta { color: #909399; flex: 1; }
  .opt-tel { color: #606266; }
  .opt-no { color: #c0c4cc; font-size: 12px; }
}
.reg-brief {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;

  .rb-item b { color: #303133; }
}
.reg-body {
  display: flex;
  gap: 12px;
  margin-top: 10px;
  align-items: stretch;

  .reg-archive {
    flex: 1.1;
    padding: 10px 14px;
    background: #f5f7fa;
    border: 1px solid #ebeef5;
    border-radius: 8px;

    .ra-tit {
      font-size: 12px;
      color: #909399;
      margin-bottom: 8px;
      letter-spacing: 1px;
    }
    .ra-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 6px 12px;
    }
    .ra-field {
      display: flex;
      gap: 6px;
      font-size: 13px;
      min-width: 0;

      .k { color: #909399; flex-shrink: 0; }
      .v { color: #303133; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      &.ra-wide .v { font-weight: 400; white-space: normal; }
    }
  }

  .reg-cards {
    flex: 1;
    display: grid;
    grid-template-rows: auto auto auto;
    gap: 8px;

    .rc-card {
      padding: 8px 12px;
      border: 1px solid #ebeef5;
      border-radius: 8px;
      background: #fff;

      .rc-tit {
        font-size: 12px;
        color: #909399;
        margin-bottom: 4px;
        letter-spacing: 1px;
      }
      .rc-line {
        display: flex;
        gap: 8px;
        align-items: baseline;
        font-size: 13px;

        .rc-doc { font-weight: 600; color: #303133; }
        .rc-meta { color: #909399; font-size: 12px; }
        &.rc-diag { font-weight: 400; color: #606266; }
      }
      .rc-balance {
        font-size: 20px;
        font-weight: 700;
        color: #e6a23c;
      }
      .rc-meta { color: #909399; font-size: 12px; }
      .rc-empty { color: #c0c4cc; font-size: 13px; }
    }
  }
}
.reg-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
