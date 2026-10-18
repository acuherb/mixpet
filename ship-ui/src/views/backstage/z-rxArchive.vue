<template>
  <div>
    <PageHead>处方笺档案</PageHead>
    <el-alert :closable="false" show-icon style="margin: 0 12px 12px" type="info"
              title="依据农业农村部公告第734号《兽医处方格式及应用规范》：兽医处方一式三联（机构留存/药房留存/动物主人留存），电子处方末行标注「以下为空白」；处方由开具、核发单位按规定妥善保存，供上级检查调档。"/>
    <!-- 筛选区 -->
    <el-card shadow="never" style="margin: 0 12px 12px">
      <div class="filters">
        <el-date-picker v-model="range" end-placeholder="结束日期" format="YYYY-MM-DD"
                        range-separator="~" size="default" start-placeholder="开始日期"
                        type="daterange" value-format="YYYY-MM-DD" @change="sendQuery"/>
        <el-select v-model="qp.doctorId" clearable placeholder="全部医生" size="default" style="width:150px" @change="sendQuery">
          <el-option v-for="d in doctorNameL" :key="d.roleId" :label="d.roleName" :value="d.roleId"/>
        </el-select>
        <el-input v-model="qp.keyword" clearable placeholder="宠物 / 畜主 / 编号" size="default"
                  style="width:200px" @input="debQuery" @keyup.enter="sendQuery"/>
        <el-select v-model="qp.status" clearable placeholder="全部状态" size="default" style="width:120px" @change="sendQuery">
          <el-option label="有效" value="VALID"/>
          <el-option label="已作废" value="VOID"/>
        </el-select>
        <el-checkbox v-model="qp.printed" label="仅已打印" border size="default" @change="sendQuery"/>
      </div>
    </el-card>

    <!-- 批量操作条 -->
    <div v-if="selRows.length" class="batch-bar">
      <span>已选 <b>{{ selRows.length }}</b> 张</span>
      <el-button size="small" type="primary" :loading="batching" @click="batchPrint">批量打印</el-button>
      <el-button size="small" type="success" :loading="batching" @click="batchPdf">批量导出 PDF</el-button>
      <el-button size="small" type="text" @click="clearSel">取消选择</el-button>
    </div>

    <!-- 列表 -->
    <el-card shadow="never" style="margin: 0 12px 12px">
      <el-table v-loading="loading" :data="page.records" row-key="printNo" @selection-change="onSel">
        <el-table-column type="selection" width="36"/>
        <el-table-column label="处方笺编号" min-width="150" prop="printNo"/>
        <el-table-column label="开具日期" width="130">
          <template #default="s">{{ fmt(s.row.prescriptionTime) }}</template>
        </el-table-column>
        <el-table-column label="畜主" min-width="90" prop="clientName" show-overflow-tooltip/>
        <el-table-column label="宠物" min-width="90" prop="petName" show-overflow-tooltip/>
        <el-table-column label="医生" min-width="90" prop="doctorName" show-overflow-tooltip/>
        <el-table-column label="注册号" min-width="120">
          <template #default="s">
            <span v-if="s.row.doctorCert">{{ s.row.doctorCert }}</span>
            <span v-else style="color:#f56c6c">未填</span>
          </template>
        </el-table-column>
        <el-table-column label="联次" width="70">
          <template #default="s">{{ s.row.copies === 3 ? '三联' : '单联' }}</template>
        </el-table-column>
        <el-table-column label="打印时间" width="140">
          <template #default="s">{{ s.row.printedAt ? fmt(s.row.printedAt) : '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="s">
            <el-tag :type="s.row.status === 'VOID' ? 'danger' : 'success'" size="small">
              {{ s.row.status === 'VOID' ? '已作废' : '有效' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="180">
          <template #default="s">
            <el-button v-if="s.row.status !== 'VOID'" link type="primary" @click="reprint(s.row)">补打</el-button>
            <el-button link type="info" @click="goRecord(s.row.recordId)">病历详情</el-button>
            <el-button v-if="s.row.status !== 'VOID'" link type="danger" @click="voidRx(s.row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <BackPage :total="page.total" @changePu="changePu"/>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import PageHead from "@/components/row/PageHead.vue";
import BackPage from "@/components/row/BackPage.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {Page} from "@/model/DO/Page";
import {RxArchiveVo} from "@/model/entiy/RxSheet";
import {reqRxArchive, reqRxPrintPrepare, reqRxVoid} from "@/request/RxApi";
import {reqDoctorName} from "@/request/DoctorApi";
import {NameDo} from "@/model/DO/NameDo";
import {debounce} from "@/utils/DebounceUtil";
import {ElMessage, ElMessageBox} from "element-plus";

const router = useRouter()
const doctorNameL = ref<NameDo[]>([])
const range = ref<[string, string] | null>(null)
const qp = reactive<PageQuery & {doctorId: number | null; keyword: string; status: string; printed: boolean}>({
  numPage: 1, pageSize: 10, doctorId: null, keyword: '', status: '', printed: false
})
const page = ref<Page<RxArchiveVo>>({records: [], total: 0})
const loading = ref(false)
const selRows = ref<RxArchiveVo[]>([])
const batching = ref(false)

const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)

/** 默认本月 */
const monthRange = (): [string, string] => {
  const t = new Date()
  const s = `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-01`
  const e = `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`
  return [s, e]
}

const sendQuery = (): void => {
  loading.value = true
  const params: any = {
    numPage: qp.numPage, pageSize: qp.pageSize,
    doctorId: qp.doctorId || undefined,
    keyword: qp.keyword || undefined,
    status: qp.status || undefined,
    printed: qp.printed ? true : undefined
  }
  if (range.value) { params.startDate = range.value[0]; params.endDate = range.value[1] }
  reqRxArchive(params).then(res => {
    page.value = res || {records: [], total: 0}
    loading.value = false
  }).catch(() => { loading.value = false })
}
const debQuery = debounce(sendQuery, 400)
const changePu = (v: PageQuery): void => { qp.numPage = v.numPage; qp.pageSize = v.pageSize; sendQuery() }
const onSel = (rows: RxArchiveVo[]): void => { selRows.value = rows }
const clearSel = (): void => { selRows.value = [] }

/** 单张补打：预检(复用编号) → 打开打印视图 */
const reprint = (row: RxArchiveVo): void => {
  reqRxPrintPrepare({recordId: row.recordId, copies: row.copies || 1}).then(res => {
    if (res.code === 1) window.open('/#/doctorPage/d-rx?recordId=' + row.recordId + '&auto=1', '_blank')
    else ElMessage.error(res.msg || '补打失败')
  }).catch(() => ElMessage.error('网络异常'))
}

/** 批量打印：逐条预检确保编号已生成，再同链路逐张打开打印视图 */
const batchPrint = async (): Promise<void> => {
  if (!selRows.value.length) return
  batching.value = true
  const rows = selRows.value.filter(r => r.status !== 'VOID')
  let ok = 0
  for (const r of rows) {
    try {
      const res = await reqRxPrintPrepare({recordId: r.recordId, copies: r.copies || 1})
      if (res.code === 1) { ok++; window.open('/#/doctorPage/d-rx?recordId=' + r.recordId + '&auto=1', '_blank') }
    } catch { /* 跳过失败行 */ }
  }
  batching.value = false
  ElMessage.success(`已打开 ${ok} 张打印视图（跳过的行请检查后重试）`)
  sendQuery()
}

/** 批量导出 PDF：逐条预检后同链路逐张打开（页面自动下载 PDF） */
const batchPdf = async (): Promise<void> => {
  if (!selRows.value.length) return
  batching.value = true
  const rows = selRows.value.filter(r => r.status !== 'VOID')
  let ok = 0
  for (const r of rows) {
    try {
      const res = await reqRxPrintPrepare({recordId: r.recordId, copies: r.copies || 1})
      if (res.code === 1) { ok++; window.open('/#/doctorPage/d-rx?recordId=' + r.recordId + '&pdf=1', '_blank') }
    } catch { /* 跳过失败行 */ }
  }
  batching.value = false
  ElMessage.success(`已触发 ${ok} 张 PDF 下载`)
  sendQuery()
}

/** 作废（填原因，占号不回收） */
const voidRx = (row: RxArchiveVo): void => {
  ElMessageBox.prompt(`作废处方笺编号 ${row.printNo}（占号不回收）`, '作废确认', {
    confirmButtonText: '确认作废', cancelButtonText: '取消',
    inputType: 'textarea', inputPlaceholder: '请填写作废原因（如：开具错误/重复开方）',
    inputPattern: /\S+/, inputErrorMessage: '请填写作废原因'
  }).then(({value}) => {
    reqRxVoid(row.printNo, value as string).then(res => {
      if (res.code === 1) { ElMessage.success('已作废'); sendQuery() }
      else ElMessage.error(res.msg || '作废失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}

/** 跳病例中心查看病历 */
const goRecord = (recordId: number): void => {
  router.push('/backstage/z-recordList?recordId=' + recordId)
}

onMounted(() => {
  range.value = monthRange()
  reqDoctorName().then(res => { doctorNameL.value = res || [] })
  sendQuery()
})
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 12px 12px;
  padding: 8px 14px;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
}
</style>
