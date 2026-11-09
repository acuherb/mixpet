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
          <h4>兽用处方笺</h4>
          <p>现代 A4 报告：药品用法用量、开方兽医与发药人留痕，适合宠主带走</p>
        </div>
      </div>
    </div>

    <!-- 二级：电子病历（现代报告实时预览，全宽） -->
    <div v-if="mode === 'record'" class="pv-body">
      <div class="pv-preview">
        <iframe v-if="recSrc" :src="recSrc" class="pv-frame" title="病历打印预览"/>
      </div>
    </div>

    <!-- 二级：处方笺（补录条 + 全宽 A4 预览） -->
    <div v-else-if="mode === 'rx'" class="pv-body rx-body">
      <div class="rx-bar">
        <el-button size="small" plain @click="showRxForm = !showRxForm">
          {{ showRxForm ? '收起' : '展开' }}补录与联次
        </el-button>
        <span class="rx-bar-info">
          联次：{{ copies === 1 ? '单页报告' : '三联全打' }}
          <template v-if="missingCount"> · 可补录 {{ missingCount }} 项</template>
        </span>
        <el-button size="small" @click="openPrintPage(rxPreviewSrc)">全屏预览</el-button>
      </div>

      <div v-if="showRxForm" class="rx-form">
        <div class="rx-form-row">
          <span class="rx-label">联次</span>
          <el-radio-group v-model="copies">
            <el-radio :label="1">单页报告（推荐）</el-radio>
            <el-radio :label="3">三联全打（归档用）</el-radio>
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
                  title="开方兽医与发药人信息由系统留痕；如需纸质签名可在报告签名区手签。电子病历完结时由接诊医师签名，可在病历页查看版本与审核状态。"/>
      </div>

      <div class="pv-preview">
        <div v-if="rxLoading" class="pv-loading">正在加载处方数据…</div>
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
import {computed, reactive, ref, watch} from 'vue'
import {Document, Tickets} from "@element-plus/icons-vue";
import {reqRxSheet} from "@/request/RecordApi";
import {reqRxPrintPrepare} from "@/request/RxApi";
import {RxSheet} from "@/model/entiy/RxSheet";
import {ElMessage} from "element-plus";

const props = defineProps<{
  recordId: number
  modelValue: boolean
  /** 预设模式：rx=直接进处方笺（如接诊第3步保存处方后快捷打印），缺省电子病历 */
  preset?: 'record' | 'rx'
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()
// 双向显示控制（兼容写法：不用 defineModel，本项目 vue 3.2 运行时无 useModel 导出）
const view = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

/** 打印类型：record 电子病历 / rx 兽用处方笺 */
const mode = ref<'record' | 'rx'>(props.preset === 'rx' ? 'rx' : 'record')
// 每次打开重置为预设模式（避免上次选择残留）
watch(() => props.modelValue, (v: boolean) => {
  if (v) mode.value = props.preset === 'rx' ? 'rx' : 'record'
})

/*===== 电子病历：只保留现代彩色报告，预览直连打印页 =====*/
const recSrc = computed(() =>
    '/#/doctorPage/d-recordPrint?recordId=' + props.recordId)
const framePrint = (): void => {
  const f = document.querySelector('.pv-frame') as HTMLIFrameElement | null
  if (!f?.contentWindow) { ElMessage.warning('预览尚未加载完成，请稍后重试'); return }
  try { f.contentWindow.print() } catch { ElMessage.error('预览打印失败，请改用「导出 PDF」') }
}
/** 弹窗被浏览器拦截时给出提示，避免点了没反应 */
const openPrintPage = (url: string): void => {
  if (!window.open(url, '_blank')) ElMessage.warning('打印窗口被拦截，请允许弹出窗口后重试')
}
const exportPdf = (): void => {
  openPrintPage(recSrc.value + '&autopdf=1')
}

/*===== 兽用处方笺：联次 + 补录 + 生成编号打印 =====*/
const rx = ref<RxSheet | null>(null)
const rxLoading = ref(false)
const copies = ref(1)
const fill = reactive({hairColor: '', species: '', withdrawalPeriod: '', dispenser: ''})
const rxPreviewSrc = computed(() => '/#/doctorPage/d-rx?recordId=' + props.recordId)
// 补录条默认收起：预览优先占满宽度，需要改信息时再展开
const showRxForm = ref(false)
const missingCount = computed(() => {
  const r = rx.value
  if (!r) return 0
  return [r.hairColor, r.species, r.withdrawalPeriod, r.dispenser].filter(v => !v).length
})
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
      openPrintPage('/#/doctorPage/d-rx?recordId=' + props.recordId + '&auto=1')
    } else {
      ElMessage.error(res.msg || '生成失败')
    }
  }).catch(() => { preparing.value = false })
}
</script>

<style lang="scss" scoped>
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

/* 二级：预览区（电子病历与处方笺共用） */
.pv-body {
  display: flex;
  gap: 16px;
  height: 72vh;
}
.pv-preview {
  position: relative;
  flex: 1; min-width: 0; min-height: 0;
  border: 1px solid #ebeef5; border-radius: 8px;
  overflow: hidden; background: #f5f6fa;
}
.pv-frame { display: block; width: 100%; height: 100%; border: none; }
.pv-loading { padding: 16px; font-size: 13px; color: #8a93a6; }
.pv-tip { float: left; font-size: 12px; color: #909399; line-height: 32px; }

/* 处方笺：补录条 + 全宽 A4 预览（不再左右分栏挤窄 210mm 纸张） */
.rx-body { flex-direction: column; gap: 10px; }
.rx-bar {
  display: flex; align-items: center; gap: 12px; flex-shrink: 0;
  padding: 6px 12px; border: 1px solid #ebeef5; border-radius: 8px; background: #fbfdff;
}
.rx-bar-info { flex: 1; font-size: 12px; color: #8a93a6; }
.rx-form {
  flex-shrink: 0; max-height: 230px; overflow-y: auto;
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px 20px;
  padding: 12px 16px; border: 1px solid #ebeef5; border-radius: 8px; background: #fafbfe;
  .el-alert { grid-column: 1 / -1; }
}
.rx-form-row {
  display: flex; align-items: center; gap: 10px;
  .rx-label { width: 64px; flex-shrink: 0; font-size: 13px; color: #606266; text-align: right; }
  .ro { font-size: 13px; color: #303133; }
}
@media (max-width: 768px) {
  .pv-body { height: 76vh; }
  .rx-form { grid-template-columns: 1fr; }
}
</style>
