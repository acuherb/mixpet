<template>
  <!-- 统一打印入口：先选「电子病历」还是「兽用处方笺（国标）」 -->
  <el-dialog v-model="view" append-to-body title="打印" top="4vh" width="94%" class="print-choice-dlg">
    <!-- 一级：选择打印什么 -->
    <div class="pc-choice">
      <div :class="['pc-card', {active: mode === 'record'}]" @click="mode = 'record'">
        <div class="pc-ico ico-rec"><el-icon :size="24"><Document/></el-icon></div>
        <div class="pc-txt">
          <h4>电子病历</h4>
          <p>内部流程病历：主诉/现病史/既往史/查体/体征/诊断/治疗/医嘱，附处方/检验/影像汇总</p>
        </div>
      </div>
      <div :class="['pc-card', {active: mode === 'rx'}]" @click="pickRx">
        <div class="pc-ico ico-rx"><el-icon :size="24"><Tickets/></el-icon></div>
        <div class="pc-txt">
          <h4>兽用处方笺（国标）</h4>
          <p>734号规范格式：一式三联/编号/执业兽医与发药人签名，适合宠主带走与检查留档</p>
        </div>
      </div>
    </div>

    <!-- 二级：电子病历（模板宫格 + 实时预览） -->
    <div v-if="mode === 'record'" class="pv-body">
      <div class="pv-tpls">
        <div v-for="t in tpls" :key="t.style" :class="['pv-tpl', {active: style === t.style}]" @click="style = t.style">
          <div class="pv-tpl-ico" :style="{background: t.color}">
            <el-icon :size="22"><component :is="t.icon"/></el-icon>
          </div>
          <div class="pv-tpl-name">{{ t.name }}</div>
          <div class="pv-tpl-desc">{{ t.desc }}</div>
        </div>
      </div>
      <div class="pv-preview">
        <iframe v-if="recSrc" :src="recSrc" class="pv-frame" title="病历打印预览"/>
      </div>
    </div>

    <!-- 二级：处方笺（联次 + 补录 + 预览） -->
    <div v-else-if="mode === 'rx'" v-loading="rxLoading" class="pv-body">
      <div class="rx-form">
        <div class="rx-form-row">
          <span class="rx-label">联次</span>
          <el-radio-group v-model="copies">
            <el-radio :label="1">仅第三联（动物主人留存）</el-radio>
            <el-radio :label="3">三联全打</el-radio>
          </el-radio-group>
        </div>
        <div class="rx-form-row">
          <span class="rx-label">毛色</span>
          <el-input v-if="!rx?.hairColor" v-model="fill.hairColor" clearable maxlength="16" placeholder="缺失，可补录（如：黄白）" style="width:200px"/>
          <span v-else class="ro">{{ rx.hairColor }}</span>
        </div>
        <div class="rx-form-row">
          <span class="rx-label">动物种类</span>
          <el-input v-if="!rx?.species" v-model="fill.species" clearable maxlength="16" placeholder="缺失，可补录（如：犬/猫）" style="width:200px"/>
          <span v-else class="ro">{{ rx.species }}</span>
        </div>
        <div class="rx-form-row">
          <span class="rx-label">休药期</span>
          <el-input v-if="!rx?.withdrawalPeriod" v-model="fill.withdrawalPeriod" clearable maxlength="32" placeholder="宠物处方默认填「—」" style="width:200px"/>
          <span v-else class="ro">{{ rx.withdrawalPeriod }}</span>
        </div>
        <div class="rx-form-row">
          <span class="rx-label">发药人</span>
          <el-input v-if="!rx?.dispenser" v-model="fill.dispenser" clearable maxlength="50" placeholder="未发药，可补录姓名或留空手写" style="width:200px"/>
          <span v-else class="ro">{{ rx.dispenser }}</span>
        </div>
        <el-alert :closable="false" show-icon type="info"
                  title="打印后由执业兽医师手签并盖医院章；系统不留电子签名。"/>
      </div>
      <div class="pv-preview">
        <iframe v-if="rxPreviewSrc" :src="rxPreviewSrc" class="pv-frame" title="处方笺预览"/>
      </div>
    </div>

    <template #footer>
      <span class="pv-tip">{{ mode === 'rx' ? '生成编号后打开打印视图（补打复用同号）' : '「导出 PDF 文件」直接下载；打印目标选“另存为 PDF”亦可' }}</span>
      <el-button @click="view = false">关闭</el-button>
      <template v-if="mode === 'record'">
        <el-button @click="framePrint">打印</el-button>
        <el-button type="success" @click="exportPdf">导出 PDF 文件</el-button>
      </template>
      <template v-else>
        <el-button :loading="preparing" type="primary" @click="prepareAndPrint">生成编号并打印</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, reactive, ref} from 'vue'
import {Document, Picture, Printer, Tickets} from "@element-plus/icons-vue";
import {reqRxSheet} from "@/request/RecordApi";
import {reqRxPrintPrepare} from "@/request/RxApi";
import {RxSheet} from "@/model/entiy/RxSheet";
import {ElMessage} from "element-plus";

const props = defineProps<{ recordId: number; modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()
// 双向显示控制（兼容写法：不用 defineModel，本项目 vue 3.2 运行时无 useModel 导出）
const view = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

/** 打印类型：record 电子病历 / rx 兽用处方笺 */
const mode = ref<'record' | 'rx'>('record')

/*===== 电子病历：模板宫格（国标病历/黑白简化/彩色详情版） =====*/
const style = ref<'std' | 'mono' | 'color'>('std')
const tpls = [
  {style: 'std' as const, name: '国标病历', desc: '黑白规范 · 纸质归档', color: '#1F2D3D', icon: Document},
  {style: 'mono' as const, name: '黑白简化', desc: '单色紧凑 · 针打友好', color: '#7f8c98', icon: Printer},
  {style: 'color' as const, name: '彩色详情版', desc: '完整内容 · 发宠主', color: '#1E6FA8', icon: Picture}
]
const recSrc = computed(() =>
    '/#/doctorPage/d-recordPrint?recordId=' + props.recordId + '&style=' + style.value)
const framePrint = (): void => {
  const f = document.querySelector('.pv-frame') as HTMLIFrameElement | null
  f?.contentWindow?.print()
}
const exportPdf = (): void => {
  window.open(recSrc.value + '&autopdf=1', '_blank')
}

/*===== 兽用处方笺：联次 + 补录 + 生成编号打印 =====*/
const rx = ref<RxSheet | null>(null)
const rxLoading = ref(false)
const copies = ref(1)
const fill = reactive({hairColor: '', species: '', withdrawalPeriod: '', dispenser: ''})
const rxPreviewSrc = computed(() => '/#/doctorPage/d-rx?recordId=' + props.recordId)
/** 选到处方笺：拉一次当前处方数据（补录项只读/可录展示） */
const pickRx = (): void => {
  mode.value = 'rx'
  if (rx.value || rxLoading.value) return
  rxLoading.value = true
  reqRxSheet(props.recordId).then(res => {
    rx.value = res
    rxLoading.value = false
  }).catch(() => { rxLoading.value = false })
}
/** 生成编号并打印：prepare 落编号/补录 → 打开打印视图自动打印 */
const preparing = ref(false)
const prepareAndPrint = (): void => {
  preparing.value = true
  reqRxPrintPrepare({
    recordId: props.recordId,
    copies: copies.value,
    // 仅传缺失项（已有值后端已存）
    hairColor: rx.value?.hairColor ? undefined : (fill.hairColor || undefined),
    species: rx.value?.species ? undefined : (fill.species || undefined),
    withdrawalPeriod: rx.value?.withdrawalPeriod ? undefined : (fill.withdrawalPeriod || undefined),
    dispenser: rx.value?.dispenser ? undefined : (fill.dispenser || undefined)
  }).then(res => {
    preparing.value = false
    if (res.code === 1) {
      view.value = false
      window.open('/#/doctorPage/d-rx?recordId=' + props.recordId + '&auto=1', '_blank')
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  }).catch(() => { preparing.value = false })
}
</script>

<style scoped>
/* 一级选择卡片 */
.pc-choice {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 14px;
}
.pc-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  border: 2px solid #ebeef5;
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  transition: border-color .2s, background .2s;
  &:hover { border-color: #c6e2ff; }
  &.active { border-color: #1E6FA8; background: #f0f7fc; }
  .pc-ico {
    width: 44px; height: 44px; flex-shrink: 0;
    border-radius: 10px; color: #fff;
    display: flex; align-items: center; justify-content: center;
    &.ico-rec { background: #1F2D3D; }
    &.ico-rx { background: #2BB6A8; }
  }
  .pc-txt {
    h4 { margin: 0 0 4px; font-size: 15px; font-weight: 700; color: #1F2D3D; }
    p { margin: 0; font-size: 12px; color: #909399; line-height: 1.6; }
  }
}
@media (max-width: 768px) {
  .pc-choice { grid-template-columns: 1fr; }
}

/* 二级：模板宫格 + 预览 */
.pv-body {
  display: flex;
  gap: 16px;
  height: 72vh;
}
.pv-tpls {
  width: 190px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
}
.pv-tpl {
  border: 2px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 10px;
  text-align: center;
  cursor: pointer;
  transition: all .2s;
  &:hover { border-color: #c6e2ff; }
  &.active { border-color: #1E6FA8; background: #f0f7fc; }
  .pv-tpl-ico {
    width: 40px; height: 40px; margin: 0 auto 8px;
    border-radius: 10px; color: #fff;
    display: flex; align-items: center; justify-content: center;
  }
  .pv-tpl-name { font-size: 13px; font-weight: 700; color: #1F2D3D; }
  .pv-tpl-desc { font-size: 11px; color: #909399; margin-top: 2px; }
}
.pv-preview {
  flex: 1; min-width: 0;
  border: 1px solid #ebeef5; border-radius: 8px;
  overflow: hidden; background: #f5f6fa;
}
.pv-frame { width: 100%; height: 100%; border: none; }
.pv-tip { float: left; font-size: 12px; color: #909399; line-height: 32px; }

/* 处方笺：联次 + 补录表单 */
.rx-form {
  width: 300px;
  flex-shrink: 0;
  overflow-y: auto;
  padding-right: 4px;
}
.rx-form-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  .rx-label {
    width: 64px; flex-shrink: 0;
    font-size: 13px; color: #606266;
    text-align: right;
  }
  .ro { font-size: 13px; color: #303133; }
}
@media (max-width: 768px) {
  .pv-body { flex-direction: column; height: auto; }
  .pv-tpls { width: 100%; flex-direction: row; }
  .rx-form { width: 100%; }
  .pv-preview { min-height: 50vh; }
}
</style>
