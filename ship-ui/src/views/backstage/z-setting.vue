<template>
  <div class="setting-wrap">
    <!-- 左：字典分类 -->
    <el-card shadow="never" class="left">
      <template #header><b>设置分类</b></template>
      <el-menu :default-active="currentType" @select="onSelect">
        <el-menu-item v-for="d in dictTypes" :key="d.type" :index="d.type">{{ d.label }}</el-menu-item>
      </el-menu>
    </el-card>

    <!-- 右：当前分类的字典值管理 -->
    <el-card shadow="never" class="right">
      <template #header>
        <b>{{ dictLabel(currentType) }}</b>
        <span class="tip">各模块相关下拉选项统一在此维护，停用后不再出现在开单/建档下拉中</span>
      </template>
      <div class="add-row">
        <el-input v-model="newKey" placeholder="输入字典值" size="small" style="width: 220px" @keyup.enter="addDict"/>
        <el-input-number v-model="newSort" :min="0" controls-position="right" placeholder="排序" size="small" style="width: 110px; margin-left: 8px"/>
        <el-button size="small" style="margin-left: 8px" type="primary" @click="addDict">添加</el-button>
      </div>
      <el-table :data="dictList" border size="small" style="margin-top: 10px">
        <el-table-column label="字典值" min-width="200">
          <template #default="s">
            <el-input v-if="s.row.editing" v-model="s.row.dictKey" size="small"/>
            <span v-else>{{ s.row.dictKey }}</span>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="130">
          <template #default="s">
            <el-input-number v-if="s.row.editing" v-model="s.row.sort" :min="0" controls-position="right" size="small" style="width: 100%"/>
            <span v-else>{{ s.row.sort }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="s">
            <el-tag v-if="s.row.active" size="small" type="success">启用</el-tag>
            <el-tag v-else size="small" type="info">停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="210">
          <template #default="s">
            <template v-if="s.row.editing">
              <el-button size="small" type="primary" @click="saveEdit(s.row)">保存</el-button>
              <el-button size="small" @click="s.row.editing = false">取消</el-button>
            </template>
            <template v-else>
              <el-button size="small" @click="s.row.editing = true">编辑</el-button>
              <el-button :type="s.row.active ? 'info' : 'success'" size="small" @click="toggle(s.row)">
                {{ s.row.active ? '停用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="del(s.row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {dictLabel, dictTypes, SysDict} from "@/model/entiy/SysDict";
import {reqAddDict, reqDelDict, reqDictByType, reqDictToggle, reqUpdateDict} from "@/request/DictApi";
import {ElMessage, ElMessageBox} from "element-plus";

type DictRow = SysDict & { editing: boolean }

const currentType = ref('lab_group')
const dictList = ref<DictRow[]>([])
const newKey = ref('')
const newSort = ref<number>(0)

const loadDicts = (): void => {
  reqDictByType(currentType.value).then(res => {
    dictList.value = (res || []).map(d => ({...d, editing: false}))
  })
}
const onSelect = (type: string): void => {
  currentType.value = type
  newKey.value = ''
  newSort.value = 0
  loadDicts()
}
const addDict = (): void => {
  if (!newKey.value.trim()) { ElMessage.warning('请输入字典值'); return }
  reqAddDict({dictId: 0, dictType: currentType.value, dictKey: newKey.value.trim(), sort: newSort.value ?? 0, active: true})
      .then(res => {
        if (res.code === 1) { ElMessage.success('已添加'); newKey.value = ''; newSort.value = 0; loadDicts() }
        else ElMessage.error(res.msg || '添加失败')
      })
}
const saveEdit = (row: DictRow): void => {
  if (!row.dictKey.trim()) { ElMessage.warning('字典值不能为空'); return }
  const {editing, ...rest} = row
  reqUpdateDict(rest).then(res => {
    if (res.code === 1) { ElMessage.success('已保存'); loadDicts() }
    else ElMessage.error(res.msg || '保存失败')
  })
}
const toggle = (row: DictRow): void => {
  reqDictToggle(row.dictId, !row.active).then(res => { if (res.code === 1) loadDicts() })
}
const del = (row: DictRow): void => {
  ElMessageBox.confirm(`确定删除「${row.dictKey}」吗？`, '删除确认', {type: 'warning'})
      .then(() => {
        reqDelDict(row.dictId).then(res => { if (res.code === 1) { ElMessage.success('已删除'); loadDicts() } })
      })
      .catch(() => ElMessage.info('已取消'))
}

onMounted(() => { loadDicts() })
</script>

<style scoped>
.setting-wrap { display: flex; gap: 12px; align-items: flex-start; }
.left { width: 180px; flex-shrink: 0; }
.right { flex: 1; }
.tip { margin-left: 12px; font-size: 12px; color: #909399; }
.add-row { display: flex; align-items: center; }
</style>
