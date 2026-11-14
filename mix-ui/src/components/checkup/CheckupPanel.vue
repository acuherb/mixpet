<template>
  <div class="cp-wrap">
    <div class="cp-bar">
      <el-select v-model="tplId" clearable filterable placeholder="选择体检模板" size="small"
                 style="width: 220px" @change="onTplChange">
        <el-option v-for="t in templates" :key="t.templateId" :label="t.name" :value="t.templateId">
          <span>{{ t.name }}</span><span class="cp-opt-cat">{{ t.category }}</span>
        </el-option>
      </el-select>
      <el-button :disabled="!schema.sections.length" size="small" @click="setNormal">全部正常</el-button>
      <el-button :disabled="!schema.sections.length" size="small" @click="clearAll">清空</el-button>
      <el-button :disabled="!schema.sections.length || !petId" :loading="saving" size="small" type="primary" @click="save">
        {{ editingId ? '保存修改' : '保存体检表' }}
      </el-button>
      <el-button v-if="editingId" size="small" @click="resetForm">取消修改</el-button>
      <span class="cp-tip">{{ schema.title || '未选择模板' }} · {{ itemCount }} 项</span>
    </div>

    <div v-if="!petId" class="cp-empty">请先从挂号队列进入接诊，带入宠物后再填体检表</div>
    <div v-else-if="!schema.sections.length" class="cp-empty">选一个体检模板开始逐项勾选，或到「体检表模板」里新建自己的模板</div>
    <template v-else>
      <div v-for="sec in schema.sections" :key="sec.key" class="cp-sec">
        <div class="cp-sec-tit">{{ sec.title }}</div>
        <div class="cp-grid">
          <div v-for="it in sec.items" :key="it.key" class="cp-item"
               :class="{wide: it.type==='textarea'||it.type==='note'||(it.type==='choice'&&!it.side)}">
            <span class="cp-label">{{ it.label }}</span>

            <!-- 纯说明 -->
            <span v-if="it.type==='note'" class="cp-note">{{ it.label }}</span>

            <!-- 勾选（可带左右 / 四肢） -->
            <template v-else-if="it.type==='check'">
              <el-checkbox v-if="!it.side" v-model="results[it.key]" size="small">{{ it.label }}</el-checkbox>
              <template v-else-if="it.side==='lr'">
                <el-checkbox v-model="results[it.key].l" size="small">左</el-checkbox>
                <el-checkbox v-model="results[it.key].r" size="small">右</el-checkbox>
              </template>
              <template v-else>
                <el-checkbox v-model="results[it.key].lf" size="small">左前</el-checkbox>
                <el-checkbox v-model="results[it.key].rf" size="small">右前</el-checkbox>
                <el-checkbox v-model="results[it.key].lh" size="small">左后</el-checkbox>
                <el-checkbox v-model="results[it.key].rh" size="small">右后</el-checkbox>
              </template>
            </template>

            <!-- 单选 -->
            <template v-else-if="it.type==='choice'">
              <el-select v-if="!it.side" v-model="results[it.key]" clearable placeholder="请选择" size="small" style="width:150px">
                <el-option v-for="o in it.options||[]" :key="o" :label="o" :value="o"/>
              </el-select>
              <template v-else>
                <span class="cp-side">左 <el-select v-model="results[it.key].l" clearable size="small" style="width:110px">
                  <el-option v-for="o in it.options||[]" :key="o" :label="o" :value="o"/>
                </el-select></span>
                <span class="cp-side">右 <el-select v-model="results[it.key].r" clearable size="small" style="width:110px">
                  <el-option v-for="o in it.options||[]" :key="o" :label="o" :value="o"/>
                </el-select></span>
              </template>
            </template>

            <!-- 数值 / 文本 -->
            <template v-else>
              <el-input v-if="it.type!=='textarea'" v-model="results[it.key]" size="small" style="width:150px"/>
              <el-input v-else v-model="results[it.key]" :rows="2" size="small" type="textarea" style="width:100%"/>
              <span v-if="it.unit" class="cp-unit">{{ it.unit }}</span>
            </template>
          </div>
        </div>
      </div>

      <div class="cp-conclusion">
        <span>体检结论 / 说明</span>
        <el-input v-model="conclusion" :rows="2" maxlength="1000" placeholder="总体结论、需复查项目、给主人的建议" type="textarea"/>
      </div>

      <div class="cp-saved">
        <div class="cp-saved-tit">已出具的体检报告（{{ saved.length }}）</div>
        <el-table v-if="saved.length" :data="saved" size="small" border>
          <el-table-column label="报告" prop="templateName" min-width="140" show-overflow-tooltip/>
          <el-table-column label="检查时间" width="160">
            <template #default="s">{{ (s.row.checkTime || '').replace('T', ' ').slice(0, 16) }}</template>
          </el-table-column>
          <el-table-column label="检查人" prop="createBy" width="100"/>
          <el-table-column label="操作" width="200">
            <template #default="s">
              <el-button link size="small" type="primary" @click="print(s.row.checkupId)">查看/打印</el-button>
              <el-button link size="small" @click="loadOne(s.row.checkupId)">载入修改</el-button>
              <el-button link size="small" type="danger" @click="del(s.row.checkupId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <p v-else class="cp-none">本次就诊还没有体检报告</p>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {getAge} from '@/utils/TimeUtil'
import {useBrand} from '@/utils/UseBrand'
import {reqGetLoginD} from '@/request/DoctorApi'
import {reqPetArchive} from '@/request/PetApi'
import {
  CheckupMeta, CheckupResults, CheckupSchema, CheckupTemplate,
  emptyResults, emptySchema, normalResults, parseResults, parseSchema
} from '@/model/VO/Checkup'
import {
  reqCheckupByPet, reqCheckupByRecord, reqCheckupOne, reqCheckupTemplates,
  reqDelCheckup, reqSaveCheckup
} from '@/request/CheckupApi'

const props = defineProps<{ petId: number; recordId?: number }>()

const {hosp, load: loadBrand} = useBrand()
const templates = ref<CheckupTemplate[]>([])
const tplId = ref<number | null>(null)
const schema = ref<CheckupSchema>(emptySchema())
const results = ref<CheckupResults>({})
const conclusion = ref('')
const editingId = ref<number | null>(null)
const saved = ref<any[]>([])
const saving = ref(false)
const archive = ref<any>(null)
const doctorName = ref('')

const itemCount = computed(() => schema.value.sections.reduce((n, s) => n + s.items.length, 0))

onMounted(() => {
  loadBrand()
  reqCheckupTemplates().then(res => { templates.value = res || [] }).catch(() => {})
  reqGetLoginD().then(d => { doctorName.value = d?.doctorName || '' }).catch(() => {})
  loadSaved()
})
watch(() => [props.petId, props.recordId], () => {
  if (props.petId) {
    reqPetArchive(props.petId).then(res => { archive.value = res || null }).catch(() => {})
    loadSaved()
  }
})

const loadSaved = (): void => {
  const req = props.recordId ? reqCheckupByRecord(props.recordId) : reqCheckupByPet(props.petId || 0)
  req.then(res => { saved.value = res || [] }).catch(() => { saved.value = [] })
}

const onTplChange = (): void => {
  const t = templates.value.find(x => x.templateId === tplId.value)
  schema.value = t ? parseSchema(t.schemaJson) : emptySchema()
  results.value = emptyResults(schema.value)
  conclusion.value = ''
  editingId.value = null
}
const setNormal = (): void => { results.value = normalResults(schema.value) }
const clearAll = (): void => { results.value = emptyResults(schema.value) }
const resetForm = (): void => { onTplChange() }

/** 表头快照：主人/宠物/医生/日期等，写进报告，之后改资料不影响历史 */
const buildMeta = (): CheckupMeta => {
  const pet = archive.value?.pet || {}
  const client = archive.value?.client || {}
  return {
    hospitalName: hosp.value.name || '',
    caseNo: props.recordId ? String(props.recordId) : '',
    owner: client.clientName || '',
    clientNo: client.clientId ? String(client.clientId) : '',
    petName: pet.petName || '',
    petNo: props.petId ? String(props.petId) : '',
    species: pet.petVariety || '',
    sex: pet.petSex === true ? '公' : pet.petSex === false ? '母' : '',
    age: pet.petBirthday ? getAge(pet.petBirthday) : '',
    weight: archive.value?.stat?.weight != null ? String(archive.value.stat.weight) : '',
    doctor: doctorName.value,
    date: new Date().toLocaleString('zh-CN')
  }
}

const save = (): void => {
  if (!props.petId) { ElMessage.warning('缺少宠物信息'); return }
  if (!tplId.value) { ElMessage.warning('请选择体检模板'); return }
  saving.value = true
  reqSaveCheckup({
    checkupId: editingId.value || undefined,
    templateId: tplId.value,
    petId: props.petId,
    recordId: props.recordId || undefined,
    schemaJson: JSON.stringify(schema.value),
    resultsJson: JSON.stringify(results.value),
    metaJson: JSON.stringify(buildMeta()),
    conclusion: conclusion.value
  }).then(res => {
    if (res?.code === 1) {
      ElMessage.success('体检表已保存')
      editingId.value = null
      loadSaved()
    }
  }).catch(() => ElMessage.error('保存失败'))
      .finally(() => { saving.value = false })
}

const loadOne = (id: number): void => {
  reqCheckupOne(id).then(res => {
    if (!res) { ElMessage.error('报告不存在或无权查看'); return }
    schema.value = parseSchema(res.schemaJson)
    results.value = {...emptyResults(schema.value), ...parseResults(res.resultsJson)}
    conclusion.value = res.conclusion || ''
    tplId.value = res.templateId
    editingId.value = id
    ElMessage.info('已载入，修改后点「保存修改」')
  })
}
const print = (id: number): void => {
  window.open('/#/doctorPage/z-checkupPrint?id=' + id, '_blank')
}
const del = (id: number): void => {
  ElMessageBox.confirm('删除这份体检报告？', '确认', {type: 'warning'})
      .then(() => reqDelCheckup(id).then(res => {
        if (res?.code === 1) { if (editingId.value === id) onTplChange(); loadSaved() }
      })).catch(() => {})
}
</script>

<style scoped>
.cp-wrap { padding: 4px 0; }
.cp-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.cp-tip { font-size: 12px; color: #909399; }
.cp-opt-cat { float: right; color: #a8abb2; font-size: 12px; }
.cp-empty { color: #909399; text-align: center; padding: 40px 0; }
.cp-sec { border: 1px solid #e8edf5; border-radius: 8px; margin-bottom: 10px; overflow: hidden; }
.cp-sec-tit { padding: 6px 12px; background: #f5f7fa; font-size: 13px; font-weight: 700; border-left: 3px solid #1E6FA8; }
.cp-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); gap: 4px 12px; padding: 8px 12px; }
.cp-item { display: flex; align-items: center; gap: 6px; min-height: 30px; flex-wrap: wrap; font-size: 12.5px; }
.cp-item.wide { grid-column: 1 / -1; }
.cp-label { color: #5a687e; flex: none; }
.cp-note { color: #606266; }
.cp-unit, .cp-side { color: #909399; font-size: 12px; }
.cp-conclusion { margin: 12px 0; }
.cp-conclusion > span { display: block; font-size: 13px; font-weight: 700; margin-bottom: 6px; }
.cp-saved { border-top: 1px dashed #e8edf5; padding-top: 12px; }
.cp-saved-tit { font-size: 13px; font-weight: 700; margin-bottom: 8px; }
.cp-none { color: #b3bccb; font-size: 12px; }
</style>
