<template>
  <div>
    <PageHead>批量入库</PageHead>

    <!-- 操作说明 -->
    <el-alert :closable="false" show-icon style="margin: 0 80px 12px" type="info">
      <template #title>
        从 Excel 复制五列数据（<b>药品名 / 数量 / 批号 / 有效期 / 备注</b>），在下方粘贴框
        <b>Ctrl+V</b> 即可批量填入；药品名自动匹配（匹配不到的行会标黄提示）；也可直接在表格中逐行填写。
      </template>
    </el-alert>

    <!-- 粘贴框 -->
    <div style="margin: 0 80px 12px">
      <el-input v-model="pasteText" :rows="3" placeholder="在此 Ctrl+V 粘贴 Excel 数据（Tab 分列、回车分行）" type="textarea"/>
    </div>

    <!-- 可编辑表格 -->
    <div style="margin: 0 80px 40px">
      <el-table :data="rows" border size="small">
        <el-table-column label="药品" min-width="230">
          <template #default="scope">
            <el-select v-model="scope.row.drugId" filterable placeholder="选择药品" size="small" style="width: 100%">
              <el-option v-for="d in drugList" :key="d.drugId"
                         :label="`${d.drugName}${d.drugSpec ? '（' + d.drugSpec + '）' : ''}`" :value="d.drugId"/>
            </el-select>
            <div v-if="scope.row.rawName && !scope.row.drugId" style="color: #E6A23C; font-size: 11px">
              未匹配到药品：{{ scope.row.rawName }}，请手动选择
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110">
          <template #default="scope">
            <el-input-number v-model="scope.row.quantity" :min="1" size="small" style="width: 90px"/>
          </template>
        </el-table-column>
        <el-table-column label="批号" width="130">
          <template #default="scope">
            <el-input v-model="scope.row.batchNo" clearable maxlength="32" size="small"/>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="150">
          <template #default="scope">
            <el-date-picker v-model="scope.row.expireDate" placeholder="选择有效期" size="small"
                            style="width: 130px" type="date" value-format="YYYY-MM-DD"/>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120">
          <template #default="scope">
            <el-input v-model="scope.row.note" clearable maxlength="128" placeholder="如：采购入库" size="small"/>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="60">
          <template #default="scope">
            <el-button :icon="Delete" circle size="small" title="删除本行" type="danger" @click="rows.splice(scope.$index, 1)"/>
          </template>
        </el-table-column>
        <template #empty>
          <span style="color: #909399">暂无数据——粘贴 Excel 或点下方「添加一行」</span>
        </template>
      </el-table>

      <div style="margin-top: 12px">
        <el-button :icon="Plus" plain size="small" @click="addRow">添加一行</el-button>
        <el-button style="margin-left: 10px" :disabled="!validRows.length" :loading="submitting" type="primary" @click="submitAll">
          确认入库（{{ validRows.length }} 条）
        </el-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue'
import {Delete, Plus} from '@element-plus/icons-vue'
import PageHead from "@/components/row/PageHead.vue";
import {reqDrugInbound, reqDrugPage} from "@/request/DrugApi";
import {Drug} from "@/model/entiy/Drug";
import {ElMessage} from "element-plus";

interface InRow {
  drugId: number | null;
  /** 粘贴时原始药品名（匹配失败时用于提示） */
  rawName?: string;
  quantity: number;
  batchNo: string;
  expireDate: string | null;
  note: string;
}

const drugList = ref<Drug[]>([])
const rows = ref<InRow[]>([])
const pasteText = ref('')
const submitting = ref(false)

const addRow = (): void => {
  rows.value.push({drugId: null, quantity: 1, batchNo: '', expireDate: null, note: ''})
}

/** 粘贴解析：Tab 分列、换行分行；五列 = 药品名/数量/批号/有效期/备注，药品名自动匹配 */
const onPaste = (e: ClipboardEvent): void => {
  const text = e.clipboardData?.getData('text') || ''
  if (!text.trim()) return
  e.preventDefault()
  const lines = text.split(/\r?\n/).filter(l => l.trim())
  let matched = 0
  for (const line of lines) {
    const cols = line.split('\t').map(c => c.trim())
    const [name, qty, batchNo, expireDate, note] = cols
    const drug = name ? drugList.value.find(d => d.drugName.includes(name) || name.includes(d.drugName)) : undefined
    if (drug) matched++
    rows.value.push({
      drugId: drug?.drugId ?? null,
      rawName: drug ? undefined : (name || undefined),
      quantity: Math.max(1, Number(qty) || 1),
      batchNo: batchNo || '',
      expireDate: expireDate || null,
      note: note || ''
    })
  }
  pasteText.value = ''
  ElMessage.success(`已解析 ${lines.length} 行，其中匹配到药品 ${matched} 行，请核对后提交`)
}

const validRows = computed(() => rows.value.filter(r => r.drugId && r.quantity > 0))

/** 逐条入库（串行，避免并发库存问题），汇总成功/失败 */
const submitAll = (): void => {
  if (!validRows.value.length) { ElMessage.warning('没有可入库的行（需选择药品且数量≥1）'); return }
  submitting.value = true
  let ok = 0
  const errs: string[] = []
  const run = async (): Promise<void> => {
    for (const r of validRows.value) {
      try {
        const res = await reqDrugInbound({
          drugId: r.drugId!, quantity: r.quantity,
          batchNo: r.batchNo || undefined, expireDate: r.expireDate || undefined, note: r.note || undefined
        })
        if (res.code === 1) ok++
        else errs.push(res.msg || '未知错误')
      } catch {
        errs.push('网络异常')
      }
    }
  }
  run().finally(() => {
    submitting.value = false
    if (ok) ElMessage.success(`入库成功 ${ok} 条，库存已更新`)
    if (errs.length) ElMessage.error(`失败 ${errs.length} 条，首条原因：${errs[0]}`)
    if (ok) rows.value = []
  })
}

onMounted(() => {
  reqDrugPage({numPage: 1, pageSize: 500}).then(res => { drugList.value = res?.records || [] })
})
</script>
