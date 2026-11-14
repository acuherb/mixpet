<template>
  <div class="rp-page" :style="{'--brand': brand}">
    <!-- 工具栏（不打印、不进 PDF） -->
    <div class="no-print rp-toolbar">
      <el-button @click="goBack">{{ isClient ? '关闭' : '返回' }}</el-button>
      <span class="rp-badge">{{ schema.title || '体检表' }}</span>
      <el-button :disabled="!schema.sections.length" type="primary" @click="doPrint">打印 / 另存为 PDF</el-button>
      <el-button :disabled="!schema.sections.length" :loading="pdfing" @click="exportPdf">下载 PDF 文件</el-button>
      <span class="rp-hint">流式版式，项目自动换行不会被截断；医院名称取自「医院资料」即时生效。</span>
      <span v-if="error" class="rp-err">{{ error }}</span>
    </div>

    <!-- 彩色流式报告：与 d-recordPrint 同款视觉，但用 grid 自动排版 -->
    <div v-if="schema.sections.length" class="rp-sheet rp-color" :style="{'--brand': brand}">
      <div class="rc-head">
        <div class="rc-head-l">
          <div class="rc-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
          <div class="rc-sub">{{ schema.title || '体检表' }}</div>
        </div>
        <div class="rc-head-r">
          <div class="rc-no">检查号 No.{{ meta.checkupNo || '—' }}</div>
          <div class="rc-date">{{ meta.date || nowDate() }}</div>
        </div>
      </div>

      <!-- 表头元信息：全部来自报告快照，历史报告不会随资料变更而变 -->
      <div class="rc-meta-grid">
        <div><span>动物主人</span><b>{{ meta.owner || '—' }}</b></div>
        <div><span>病例编号</span><b>{{ meta.clientNo || '—' }}</b></div>
        <div><span>动物名称</span><b>{{ meta.petName || '—' }}</b></div>
        <div><span>动物编号</span><b>{{ meta.petNo || '—' }}</b></div>
        <div><span>动物种类</span><b>{{ meta.species || '—' }}</b></div>
        <div><span>性别</span><b>{{ meta.sex || '—' }}</b></div>
        <div><span>年龄</span><b>{{ meta.age || '—' }}</b></div>
        <div><span>体重</span><b>{{ meta.weight || '—' }}</b></div>
        <div><span>主治医师</span><b>{{ meta.doctor || '—' }}</b></div>
        <div><span>就诊编号</span><b>{{ meta.caseNo || '—' }}</b></div>
      </div>

      <!-- 分区：grid 自动换行，长文本自动折行，永不被固定坐标裁掉 -->
      <div v-for="sec in schema.sections" :key="sec.key" class="rc-card">
        <div class="rc-card-tit">{{ sec.title }}</div>
        <div class="rc-items">
          <div v-for="it in sec.items" :key="it.key" class="rc-item" :class="{wide: it.type==='textarea'||it.type==='note'||(it.type==='choice'&&!it.side)}">
            <!-- 纯说明 -->
            <div v-if="it.type==='note'" class="it-note">{{ it.label }}</div>

            <!-- 勾选（可带左右 / 四肢） -->
            <div v-else-if="it.type==='check'" class="it-line">
              <template v-if="it.side==='lr'">
                <span class="it-label">{{ it.label }}</span>
                <span class="it-opt">左 <b>{{ tick(sideVal(results[it.key],'l')) }}</b></span>
                <span class="it-opt">右 <b>{{ tick(sideVal(results[it.key],'r')) }}</b></span>
              </template>
              <template v-else-if="it.side==='four'">
                <span class="it-label">{{ it.label }}</span>
                <span class="it-opt">左前 <b>{{ tick(sideVal(results[it.key],'lf')) }}</b></span>
                <span class="it-opt">右前 <b>{{ tick(sideVal(results[it.key],'rf')) }}</b></span>
                <span class="it-opt">左后 <b>{{ tick(sideVal(results[it.key],'lh')) }}</b></span>
                <span class="it-opt">右后 <b>{{ tick(sideVal(results[it.key],'rh')) }}</b></span>
              </template>
              <template v-else>
                <b class="it-box">{{ tick(results[it.key] === true) }}</b><span class="it-label">{{ it.label }}</span>
              </template>
            </div>

            <!-- 单选 -->
            <div v-else-if="it.type==='choice'" class="it-line">
              <span class="it-label">{{ it.label }}</span>
              <template v-if="it.side==='lr'">
                <span v-for="sd in ['l','r']" :key="sd" class="it-side">
                  {{ sd==='l'?'左':'右' }}：
                  <span v-for="o in it.options||[]" :key="o" class="it-opt">
                    <b>{{ tick(sideChoice(results[it.key],sd)===o) }}</b>{{ o }}
                  </span>
                </span>
              </template>
              <template v-else>
                <span v-for="o in it.options||[]" :key="o" class="it-opt">
                  <b>{{ tick(choiceVal(results[it.key])===o) }}</b>{{ o }}
                </span>
              </template>
            </div>

            <!-- 数值 / 文本 -->
            <div v-else class="it-line">
              <span class="it-label">{{ it.label }}</span>
              <span class="it-value">{{ textVal(results[it.key]) || '＿＿＿＿＿＿' }}</span>
              <span v-if="it.unit" class="it-unit">{{ it.unit }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="rc-advice">
        <div class="rc-advice-tit">体检结论 / 说明</div>
        <div class="rc-advice-body">{{ conclusion || '—' }}</div>
      </div>
      <div class="rc-signbox">
        <span>检查医师：{{ meta.doctor || '—' }}</span>
        <span>检查日期：{{ meta.date || nowDate() }}</span>
        <span>打印时间：{{ nowText() }}</span>
      </div>
      <div class="rc-foot">{{ hosp.name || '动物诊疗机构' }} · 本资料仅供宠物主人留存与复诊参考</div>
    </div>

    <div v-else-if="loaded" class="no-print rp-empty">{{ error || '未找到该体检报告，或无权查看' }}</div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {useBrand} from '@/utils/UseBrand'
import {
  CheckupMeta, CheckupResults, CheckupSchema, emptySchema, parseMeta, parseResults, parseSchema
} from '@/model/VO/Checkup'
import {reqCheckupOne, reqCheckupTemplateOne} from '@/request/CheckupApi'

const route = useRoute()
const router = useRouter()
const {hosp, brand, load: loadBrand} = useBrand()
const isClient = computed(() => route.path.startsWith('/reception'))
const schema = ref<CheckupSchema>(emptySchema())
const results = ref<CheckupResults>({})
const meta = ref<CheckupMeta>({})
const conclusion = ref('')
const loaded = ref(false)
const error = ref('')
const pdfing = ref(false)

const nowDate = (): string => new Date().toLocaleDateString('zh-CN')
const nowText = (): string => new Date().toLocaleString('zh-CN')
const tick = (v: boolean): string => v ? '☑' : '☐'
const sideVal = (v: any, k: string): boolean => !!v && typeof v === 'object' && !!v[k]
const sideChoice = (v: any, k: string): string => v && typeof v === 'object' ? (v[k] || '') : ''
const choiceVal = (v: any): string => typeof v === 'string' ? v : ''
const textVal = (v: any): string => v == null ? '' : String(v)

const doPrint = (): void => window.print()
const exportPdf = (): void => {
  const el = document.querySelector('.rp-sheet') as HTMLElement | null
  if (!el) return
  pdfing.value = true
  import('html2pdf.js').then(({default: html2pdf}) => html2pdf().set({
    margin: 8,
    filename: `${schema.value.title || '体检表'}-No.${meta.value.checkupNo || '预览'}.pdf`,
    image: {type: 'jpeg', quality: 0.95},
    html2canvas: {scale: 2},
    jsPDF: {unit: 'mm', format: 'a4', orientation: 'portrait'}
  }).from(el).save())
      .catch(() => ElMessage.error('PDF 导出失败，请改用「打印 / 另存为 PDF」'))
      .finally(() => { pdfing.value = false })
}
const goBack = (): void => {
  if (isClient.value || window.history.length <= 1) window.close()
  else router.back()
}

onMounted(() => {
  loadBrand()
  // 1) 管理端编辑器「预览空表」：schema 走 sessionStorage
  if (route.query.preview === '1') {
    try {
      const p = JSON.parse(sessionStorage.getItem('mixpet_checkup_preview') || '{}')
      if (p.schema) {
        schema.value = p.schema
        meta.value = {hospitalName: hosp.value.name, date: nowDate()}
        loaded.value = true
        return
      }
    } catch { /* 落到下方错误提示 */ }
    error.value = '预览数据已失效，请回到模板编辑器重新点「预览空表」'
    loaded.value = true
    return
  }
  // 2) 模板空表预览（有 templateId）
  const tplId = Number(route.query.tplId)
  if (tplId) {
    reqCheckupTemplateOne(tplId).then(res => {
      if (!res) { error.value = '模板不存在'; return }
      schema.value = parseSchema(res.schemaJson)
      meta.value = {date: nowDate()}
    }).catch(() => { error.value = '模板加载失败' }).finally(() => { loaded.value = true })
    return
  }
  // 3) 已出具的报告：后端已做顾客归属校验，越权返回 null
  const id = Number(route.query.id)
  if (!id) { loaded.value = true; error.value = '缺少检查号'; return }
  reqCheckupOne(id).then(res => {
    if (!res) { error.value = '未找到该体检报告，或无权查看'; return }
    schema.value = parseSchema(res.schemaJson)
    results.value = parseResults(res.resultsJson)
    meta.value = parseMeta(res.metaJson)
    meta.value.checkupNo = meta.value.checkupNo || String(res.checkupId)
    conclusion.value = res.conclusion || ''
    if (route.query.autopdf === '1') setTimeout(() => exportPdf(), 600)
  }).catch(() => { error.value = '报告加载失败，请稍后重试' }).finally(() => { loaded.value = true })
})
</script>

<style lang="scss" scoped>
.rp-page { padding: 20px; background: #f5f6fa; min-height: 100vh; }
.rp-toolbar {
  max-width: 860px; margin: 0 auto 16px; display: flex; align-items: center;
  gap: 10px; flex-wrap: wrap;
  .rp-hint { font-size: 12px; color: #909399; flex-basis: 100%; }
  .rp-err { font-size: 12px; color: #d03050; font-weight: 600; }
  .rp-badge { font-size: 12px; color: var(--brand); background: #eef6fc; border: 1px solid #cfe4f5; border-radius: 14px; padding: 4px 12px; }
}
.rp-sheet {
  max-width: 860px; margin: 0 auto 24px; background: #fff;
  border: 1px solid #e8edf5; border-radius: 12px; box-shadow: 0 8px 28px rgba(31,45,61,.1);
}
.rp-empty { text-align: center; color: #999; margin-top: 60px; }
.rp-color { border: none; border-radius: 12px; padding: 0; overflow: hidden; }
.rc-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 24px; background: linear-gradient(120deg, var(--brand), #2BB6A8); color: #fff;
  .rc-hosp { font-size: 19px; font-weight: 800; letter-spacing: 2px; }
  .rc-sub { font-size: 13px; opacity: .9; margin-top: 2px; }
  .rc-no { font-size: 14px; font-weight: 700; text-align: right; }
  .rc-date { font-size: 12px; opacity: .85; text-align: right; margin-top: 2px; }
}
.rc-meta-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 4px 12px; padding: 14px 24px; background: #f0f7fc; border-bottom: 1px solid #e4e7ed;
  div { display: flex; gap: 6px; font-size: 12px; line-height: 1.8; }
  span { color: #7d8899; flex: none; }
  b { color: #1F2D3D; font-weight: 600; word-break: break-all; }
}
.rc-card {
  margin: 14px 20px; border: 1px solid #ebeef5; border-radius: 10px; overflow: hidden;
  .rc-card-tit {
    padding: 8px 14px; background: #f5f7fa; font-size: 13px; font-weight: 700;
    color: #1F2D3D; border-left: 3px solid var(--brand);
  }
}
.rc-items { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 2px 12px; padding: 8px 12px 12px; }
.rc-item { font-size: 12.5px; color: #303133; border-bottom: 1px dashed #eef2f8; padding: 4px 0; min-width: 0; }
.rc-item.wide { grid-column: 1 / -1; }
.it-line { display: flex; align-items: baseline; gap: 6px; flex-wrap: wrap; line-height: 1.7; }
.it-box { color: var(--brand); font-size: 14px; flex: none; }
.it-label { color: #303133; }
.it-opt { color: #5a687e; white-space: nowrap; }
.it-opt b { color: var(--brand); font-size: 13px; }
.it-side { display: inline-flex; gap: 4px; flex-wrap: wrap; margin-right: 8px; }
.it-value { color: #1F2D3D; border-bottom: 1px solid #c8d2e2; padding: 0 6px; word-break: break-all; }
.it-unit { color: #909399; }
.it-note { color: #606266; line-height: 1.7; white-space: pre-wrap; }
.rc-advice {
  margin: 14px 20px; padding: 12px 16px; background: #fdf6ec;
  border: 1px dashed #e6a23c; border-radius: 10px;
  .rc-advice-tit { font-size: 13px; font-weight: 700; color: #b88230; margin-bottom: 4px; }
  .rc-advice-body { font-size: 13px; color: #606266; line-height: 1.7; white-space: pre-wrap; min-height: 20px; }
}
.rc-signbox {
  display: flex; flex-wrap: wrap; gap: 8px 24px; margin: 14px 20px 6px;
  padding: 10px 12px; border-radius: 8px; background: #f4f8fb;
  font-size: 12px; color: #42536b; font-weight: 600;
}
.rc-foot { padding: 12px 24px 20px; text-align: center; font-size: 11px; color: #a8abb2; }

@page { size: A4 portrait; margin: 10mm; }
@media print {
  .rp-page { padding: 0; background: #fff; }
  .no-print { display: none !important; }
  .rp-sheet { box-shadow: none; margin: 0; max-width: 100%; }
  .rp-color { border-radius: 0; }
  .rc-head, .rc-meta-grid, .rc-card-tit, .rc-advice, .rc-signbox {
    -webkit-print-color-adjust: exact; print-color-adjust: exact;
  }
  .rc-card { break-inside: avoid; }
}
</style>
