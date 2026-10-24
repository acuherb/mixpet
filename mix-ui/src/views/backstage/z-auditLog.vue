<template>
  <div>
    <PageHead>操作日志</PageHead>
    <el-alert :closable="false" show-icon style="margin-bottom: 12px" type="info"
              title="高危写操作留痕：收款/退款/发药/顾客审核/药品入库/数据备份还原/处方作废等；操作人一律取登录态，不可伪造。"/>
    <BackOpCol role="操作日志" @query="nowQuery">
      <el-input v-model="qp.operator" clearable placeholder="操作人姓名" size="large" style="width: 180px"
                @input="debQuery" @keyup.enter="nowQuery"/>
      <el-select v-model="qp.module" clearable placeholder="模块" size="large" style="width: 140px; margin-left: 12px"
                 @change="nowQuery">
        <el-option v-for="m in modules" :key="m.value" :label="m.label" :value="m.value"/>
      </el-select>
    </BackOpCol>
    <el-table :data="list.records" max-height="calc(100vh - 260px)" border stripe>
      <el-table-column label="时间" prop="createTime" width="170"/>
      <el-table-column label="操作人" prop="operator" width="100" show-overflow-tooltip/>
      <el-table-column label="角色" width="90">
        <template #default="s">
          <el-tag size="small" :type="s.row.role === 'ADMIN' ? 'danger' : s.row.role === 'MANAGER' ? 'warning' : 'info'" effect="plain">
            {{ s.row.role || '—' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="模块" width="100">
        <template #default="s"><el-tag size="small" effect="plain">{{ moduleName(s.row.module) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="动作" width="150" show-overflow-tooltip>
        <template #default="s">{{ actionName(s.row.action) }}</template>
      </el-table-column>
      <el-table-column label="对象" min-width="120" prop="target" show-overflow-tooltip/>
      <el-table-column label="来源IP" width="130" prop="ip" show-overflow-tooltip/>
      <template #empty><el-empty :image-size="60" description="暂无操作记录"/></template>
    </el-table>
    <BackPage :total="list.total" @changePu="changePuB"/>
  </div>
</template>

<script lang="ts" setup>
import BackOpCol from "@/components/row/BackOpCol.vue";
import BackPage from "@/components/row/BackPage.vue";
import PageHead from "@/components/row/PageHead.vue";
import {debounce} from "@/utils/DebounceUtil";
import {onMounted, reactive, ref} from "vue";
import reqApi from "@/request/reqAPI";
import {Page} from "@/model/DO/Page";
import {PageQuery} from "@/model/VO/BackQuery";

interface AuditLogRow {
  logId: number;
  operator: string;
  role: string;
  module: string;
  action: string;
  target: string;
  detail: string;
  ip: string;
  createTime: string;
}

const list = ref<Page<AuditLogRow>>({records: [], total: 0} as any)
const qp = reactive({numPage: 1, pageSize: 15, operator: '', module: ''})

const modules = [
  {value: 'bill', label: '收银账单'},
  {value: 'backup', label: '数据备份'},
  {value: 'client', label: '顾客审核'},
  {value: 'drug', label: '药品库存'},
  {value: 'appointment', label: '挂号'},
  {value: 'record', label: '病历处方'}
]
const moduleName = (m: string): string => modules.find(x => x.value === m)?.label || m || '—'
const actionName = (a: string): string => ({
  pay: '收款', dispense: '发药核对', refund: '退款冲正', create: '数据备份', delete: '删除备份',
  restore: '数据还原(毁库)', audit: '顾客审核', inbound: '药品入库', miss: '标记爽约', cancelRx: '处方作废'
} as Record<string, string>)[a] || a

const sendQuery = (): void => {
  const params: Record<string, any> = {numPage: qp.numPage, pageSize: qp.pageSize}
  if (qp.operator.trim()) params.operator = qp.operator.trim()
  if (qp.module) params.module = qp.module
  reqApi<string, Page<AuditLogRow>>("auditLog/page", {params}).then(res => { list.value = res })
}
const nowQuery = (): void => { qp.numPage = 1; sendQuery() }
const debQuery = debounce(nowQuery, 400)
const changePuB = (val: PageQuery) => { qp.numPage = val.numPage; qp.pageSize = val.pageSize; sendQuery() }
onMounted(() => sendQuery())
</script>
