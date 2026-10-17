<template>
  <div class="record-center">
    <!-- 检索栏 -->
    <el-form inline @submit.prevent>
      <el-form-item>
        <el-input v-model="q.recordId" clearable placeholder="病历号" style="width: 110px" @keyup.enter="search"/>
      </el-form-item>
      <el-form-item>
        <el-input v-model="q.petName" clearable placeholder="宠物名" style="width: 110px" @keyup.enter="search"/>
      </el-form-item>
      <el-form-item>
        <el-input v-model="q.clientName" clearable placeholder="客户名" style="width: 110px" @keyup.enter="search"/>
      </el-form-item>
      <el-form-item>
        <el-input v-model="q.doctorName" clearable placeholder="医生名" style="width: 110px" @keyup.enter="search"/>
      </el-form-item>
      <el-form-item>
        <el-input v-model="q.diagnosis" clearable placeholder="诊断关键字" style="width: 130px" @keyup.enter="search"/>
      </el-form-item>
      <el-form-item>
        <el-select v-model="q.status" clearable placeholder="状态" style="width: 104px">
          <el-option :value="null" label="全部状态"/>
          <el-option :value="0" label="接诊中"/>
          <el-option :value="1" label="已完结"/>
          <el-option :value="2" label="已作废"/>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="~"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="onDateChange"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 病历列表 -->
    <el-table v-loading="loading" :data="list.records" stripe @row-click="openDetail">
      <el-table-column label="病历号" prop="recordId" width="80"/>
      <el-table-column label="宠物" prop="petName" width="90" show-overflow-tooltip/>
      <el-table-column label="客户" prop="clientName" width="90" show-overflow-tooltip/>
      <el-table-column label="接诊医生" prop="doctorName" width="100" show-overflow-tooltip/>
      <el-table-column label="诊断" min-width="160" prop="diagnosis" show-overflow-tooltip/>
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" size="small" type="warning">接诊中</el-tag>
          <el-tag v-else-if="scope.row.status === 2" size="small" type="danger">已作废</el-tag>
          <el-tag v-else size="small" type="success">已完结</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="费用" width="90">
        <template #default="scope">￥{{ scope.row.totalMoney ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="就诊时间" min-width="140">
        <template #default="scope">{{ getDisplayTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="70">
        <template #default="scope">
          <el-button size="small" type="primary" link @click.stop="openDetail(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 加载失败态：一键重试 -->
    <el-empty v-if="listError" description="加载失败，请检查网络后重试">
      <el-button size="small" type="primary" @click="load">重新加载</el-button>
    </el-empty>

    <div class="rc-pager">
      <el-pagination
          v-model:current-page="q.numPage"
          :page-size="q.pageSize"
          :total="list.total"
          background
          layout="total, prev, pager, next"
          @current-change="load"
      />
    </div>

    <!-- 病例档案详情 -->
    <el-dialog v-model="detailView" append-to-body destroy-on-close title="病例档案" top="5vh" width="92%">
      <RecordDetailFull v-if="detailId" :record-id="detailId"/>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from "vue";
import {reqRecordPage} from "@/request/RecordApi";
import {Page} from "@/model/DO/Page";
import {RecordDto} from "@/model/DO/RecordDto";
import {getDisplayTime} from "@/utils/TimeUtil";
import RecordDetailFull from "@/components/record/RecordDetailFull.vue";

const q = reactive<{
  recordId?: number | null;
  petName?: string;
  clientName?: string;
  doctorName?: string;
  diagnosis?: string;
  status?: number | null;
  startTime?: string;
  endTime?: string;
  numPage: number;
  pageSize: number;
}>({
  recordId: null, petName: '', clientName: '', doctorName: '', diagnosis: '',
  status: null, startTime: '', endTime: '', numPage: 1, pageSize: 10
})
const dateRange = ref<[string, string] | null>(null)
const list = ref<Page<RecordDto>>({records: [], total: 0})
const loading = ref(false)
// 加载失败态（可一键重试，避免接口异常时表格白屏）
const listError = ref(false)
const detailView = ref(false)
const detailId = ref(0)

const load = (): void => {
  loading.value = true
  loading.value = true
  listError.value = false
  reqRecordPage(q).then(res => {
    loading.value = false
    list.value = res || {records: [], total: 0}
  }).catch(() => { loading.value = false; listError.value = true })
      .finally(() => { loading.value = false })
}
const search = (): void => { q.numPage = 1; load() }
const reset = (): void => {
  q.recordId = null; q.petName = ''; q.clientName = ''; q.doctorName = ''
  q.diagnosis = ''; q.status = null; q.startTime = ''; q.endTime = ''
  dateRange.value = null
  search()
}
const onDateChange = (val: [string, string] | null): void => {
  q.startTime = val ? val[0] : ''
  q.endTime = val ? val[1] : ''
}
const openDetail = (row: RecordDto): void => {
  detailId.value = row.recordId
  detailView.value = true
}

onMounted(load)
</script>

<style lang="scss" scoped>
.record-center {
  .rc-pager {
    display: flex;
    justify-content: flex-end;
    margin-top: 14px;
  }
}
</style>
