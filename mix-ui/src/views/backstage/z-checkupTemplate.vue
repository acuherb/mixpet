<template>
  <div class="ct-wrap">
    <PageHead>体检表模板</PageHead>
    <div class="ct-body">
      <!-- 左：模板列表 -->
      <aside class="ct-list">
        <div class="ct-list-head">
          <el-input v-model="seaName" clearable placeholder="搜索模板" size="small" @keyup.enter="loadPage" @clear="loadPage"/>
          <el-button size="small" type="primary" @click="newTemplate">新增</el-button>
        </div>
        <el-table v-loading="loading" :data="page.records" highlight-current-row size="small" @row-click="edit">
          <el-table-column label="模板" min-width="120" prop="name" show-overflow-tooltip/>
          <el-table-column label="状态" width="62">
            <template #default="s">
              <el-tag :type="s.row.active ? 'success' : 'info'" size="small">{{ s.row.active ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="numPage" :page-size="pageSize" :total="page.total"
                       layout="prev, next" size="small" @current-change="loadPage"/>
      </aside>

      <!-- 右：可视化编辑器 -->
      <section v-if="form" class="ct-editor">
        <div class="ct-ed-head">
          <h3>{{ isNew ? '新增模板' : '编辑：' + form.name }}</h3>
          <el-button size="small" @click="preview">预览空表</el-button>
          <el-button :loading="saving" size="small" type="primary" @click="save">保存模板</el-button>
        </div>

        <el-form class="ct-base" inline size="small">
          <el-form-item label="名称"><el-input v-model="form.name" style="width:180px"/></el-form-item>
          <el-form-item label="分类"><el-input v-model="form.category" style="width:110px"/></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :max="999" :min="0" style="width:110px"/></el-form-item>
          <el-form-item label="启用"><el-switch v-model="form.active"/></el-form-item>
          <el-form-item label="说明"><el-input v-model="form.description" style="width:240px"/></el-form-item>
        </el-form>

        <div v-for="(sec, si) in schema.sections" :key="sec.key" class="ct-sec">
          <div class="ct-sec-head">
            <el-input v-model="sec.title" placeholder="分区标题" size="small" style="width:220px"/>
            <span class="ct-sec-cnt">{{ sec.items.length }} 项</span>
            <el-button size="small" :disabled="si===0" @click="moveSec(si,-1)">↑</el-button>
            <el-button size="small" :disabled="si===schema.sections.length-1" @click="moveSec(si,1)">↓</el-button>
            <el-button size="small" type="primary" plain @click="addItem(si)">+ 项目</el-button>
            <el-button size="small" type="danger" plain @click="delSec(si)">删除分区</el-button>
          </div>
          <div v-for="(it, ii) in sec.items" :key="it.key" class="ct-item">
            <el-input v-model="it.label" placeholder="项目名称" size="small" style="width:190px"/>
            <el-select v-model="it.type" size="small" style="width:96px" @change="onTypeChange(it)">
              <el-option label="勾选" value="check"/>
              <el-option label="单选" value="choice"/>
              <el-option label="数值" value="number"/>
              <el-option label="单行" value="text"/>
              <el-option label="多行" value="textarea"/>
              <el-option label="说明" value="note"/>
            </el-select>
            <el-select v-if="it.type==='check'||it.type==='choice'" v-model="it.side" clearable
                       placeholder="不分侧" size="small" style="width:100px" @change="onSideChange(it)">
              <el-option label="左右" value="lr"/>
              <el-option v-if="it.type==='check'" label="四肢" value="four"/>
            </el-select>
            <el-input v-if="it.type==='choice'" v-model="optionsText[it.key]" placeholder="选项，逗号分隔"
                      size="small" style="width:190px" @change="syncOptions(it)"/>
            <el-input v-if="it.type==='number'" v-model="it.unit" placeholder="单位" size="small" style="width:76px"/>
            <el-checkbox v-if="it.type==='check'" v-model="it.normal" label="正常项" size="small"/>
            <el-button size="small" :disabled="ii===0" @click="moveItem(si,ii,-1)">↑</el-button>
            <el-button size="small" :disabled="ii===sec.items.length-1" @click="moveItem(si,ii,1)">↓</el-button>
            <el-button size="small" type="danger" plain @click="delItem(si,ii)">×</el-button>
          </div>
          <div v-if="!sec.items.length" class="ct-sec-empty">还没有项目，点「+ 项目」添加</div>
        </div>
        <el-button class="ct-add-sec" @click="addSec">+ 添加分区</el-button>
      </section>
      <section v-else class="ct-empty">从左侧选一个模板开始编辑，或点「新增」创建自己的体检表</section>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import PageHead from '@/components/row/PageHead.vue'
import {Page} from '@/model/DO/Page'
import {
  CheckupItem, CheckupSchema, CheckupTemplate, emptySchema, nextKey, parseSchema
} from '@/model/VO/Checkup'
import {
  reqCheckupTemplateOne, reqCheckupTemplatePage, reqSaveCheckupTemplate, reqUpdateCheckupTemplate
} from '@/request/CheckupApi'

const page = ref<Page<CheckupTemplate>>({records: [], total: 0})
const seaName = ref('')
const numPage = ref(1)
const pageSize = 10
const loading = ref(false)
const saving = ref(false)
const isNew = ref(false)
const form = ref<CheckupTemplate | null>(null)
const schema = ref<CheckupSchema>(emptySchema())
/** choice 的选项在编辑器里用「逗号分隔」文本编辑，保存时同步回 options 数组 */
const optionsText = reactive<Record<string, string>>({})

const loadPage = (): void => {
  loading.value = true
  reqCheckupTemplatePage({numPage: numPage.value, pageSize, seaName: seaName.value})
      .then(res => { page.value = res || {records: [], total: 0} })
      .catch(() => ElMessage.error('模板列表加载失败'))
      .finally(() => { loading.value = false })
}
onMounted(loadPage)

const rebuildOptionsText = (): void => {
  Object.keys(optionsText).forEach(k => delete optionsText[k])
  for (const s of schema.value.sections) {
    for (const it of s.items) if (it.type === 'choice') optionsText[it.key] = (it.options || []).join('，')
  }
}
const syncOptions = (it: CheckupItem): void => {
  it.options = (optionsText[it.key] || '').split(/[,，]/).map(s => s.trim()).filter(Boolean)
}
const allItemKeys = (): Set<string> =>
    new Set(schema.value.sections.flatMap(s => s.items.map(i => i.key)))

const edit = (row: CheckupTemplate): void => {
  reqCheckupTemplateOne(row.templateId).then(res => {
    if (!res) { ElMessage.error('模板加载失败'); return }
    form.value = {...res}
    schema.value = parseSchema(res.schemaJson)
    isNew.value = false
    rebuildOptionsText()
  })
}
const newTemplate = (): void => {
  form.value = {
    templateId: 0, tplKey: 'Custom_' + Date.now(), name: '新体检表', category: '自定义',
    description: '', schemaJson: '', active: true, isBuiltin: false, sortOrder: 99
  }
  schema.value = {title: '新体检表', sections: [{key: 's1', title: '1. 检查项目', items: []}]}
  isNew.value = true
  rebuildOptionsText()
}

const addSec = (): void => {
  const used = new Set(schema.value.sections.map(s => s.key))
  schema.value.sections.push({key: nextKey('s', used), title: '新分区', items: []})
}
const delSec = (si: number): void => {
  ElMessageBox.confirm('删除该分区及其所有项目？', '确认', {type: 'warning'})
      .then(() => schema.value.sections.splice(si, 1)).catch(() => {})
}
const moveSec = (si: number, d: number): void => {
  const arr = schema.value.sections
  const j = si + d
  if (j < 0 || j >= arr.length) return
  ;[arr[si], arr[j]] = [arr[j], arr[si]]
}
const addItem = (si: number): void => {
  schema.value.sections[si].items.push({
    key: nextKey('i', allItemKeys()), label: '新项目', type: 'check'
  })
}
const delItem = (si: number, ii: number): void => { schema.value.sections[si].items.splice(ii, 1) }
const moveItem = (si: number, ii: number, d: number): void => {
  const arr = schema.value.sections[si].items
  const j = ii + d
  if (j < 0 || j >= arr.length) return
  ;[arr[ii], arr[j]] = [arr[j], arr[ii]]
}
const onTypeChange = (it: CheckupItem): void => {
  if (it.type !== 'check' && it.type !== 'choice') { it.side = undefined; it.normal = false }
  if (it.type !== 'choice') it.options = undefined
  if (it.type !== 'number') it.unit = undefined
  if (it.type === 'choice' && !it.options?.length) it.options = ['正常', '异常']
  if (it.type === 'choice') optionsText[it.key] = (it.options || []).join('，')
}
const onSideChange = (it: CheckupItem): void => {
  if (it.type === 'choice' && it.side === 'four') it.side = 'lr'
}

const save = (): void => {
  if (!form.value) return
  if (!form.value.name.trim()) { ElMessage.warning('请填模板名称'); return }
  if (!schema.value.sections.length) { ElMessage.warning('至少保留一个分区'); return }
  schema.value.title = form.value.name
  for (const s of schema.value.sections) {
    for (const it of s.items) {
      if (!it.label.trim()) { ElMessage.warning('项目名称不能为空'); return }
      if (it.type === 'choice') {
        syncOptions(it)
        if (!it.options?.length) { ElMessage.warning(`「${it.label}」请填写选项`); return }
      }
    }
  }
  form.value.schemaJson = JSON.stringify(schema.value)
  saving.value = true
  const fn = isNew.value ? reqSaveCheckupTemplate : reqUpdateCheckupTemplate
  fn(form.value).then(res => {
    if (res?.code === 1) { isNew.value = false; loadPage() }
  }).catch(() => ElMessage.error('保存失败'))
      .finally(() => { saving.value = false })
}

/** 预览空表：schema 通过 sessionStorage 传给打印页，避免超长 query */
const preview = (): void => {
  if (!form.value) return
  schema.value.title = form.value.name
  for (const s of schema.value.sections) for (const it of s.items) if (it.type === 'choice') syncOptions(it)
  sessionStorage.setItem('mixpet_checkup_preview', JSON.stringify({name: form.value.name, schema: schema.value}))
  window.open('/#/doctorPage/z-checkupPrint?preview=1', '_blank')
}
</script>

<style scoped>
.ct-wrap { padding: 0 4px; }
.ct-body { display: flex; gap: 14px; align-items: flex-start; }
.ct-list { width: 320px; flex: none; background: #fff; border: 1px solid #e8edf5; border-radius: 10px; padding: 12px; }
.ct-list-head { display: flex; gap: 8px; margin-bottom: 10px; }
.ct-editor { flex: 1; min-width: 0; background: #fff; border: 1px solid #e8edf5; border-radius: 10px; padding: 16px; }
.ct-empty { flex: 1; color: #909399; text-align: center; padding: 80px 0; }
.ct-ed-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.ct-ed-head h3 { margin: 0; flex: 1; font-size: 16px; }
.ct-base { border-bottom: 1px dashed #e8edf5; padding-bottom: 6px; margin-bottom: 12px; }
.ct-sec { border: 1px solid #e8edf5; border-radius: 8px; padding: 10px; margin-bottom: 12px; background: #fafcff; }
.ct-sec-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.ct-sec-cnt { font-size: 12px; color: #909399; }
.ct-item { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; flex-wrap: wrap; }
.ct-sec-empty { font-size: 12px; color: #b3bccb; padding: 4px 2px; }
.ct-add-sec { width: 100%; }
</style>
