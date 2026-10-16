<template>
  <div>
    <PageHead>聊天数据清理</PageHead>
    <el-card shadow="never" class="clean-card">
      <template #header>
        <b>历史聊天数据</b>
        <span style="color:#909399; font-size:12px; margin-left:8px">
          系统不自动删除，超期记录需手动清理（会同时删除记录及对应图片/文件）
        </span>
      </template>

      <div class="clean-row">
        <span>保留天数：</span>
        <el-input-number v-model="retainDays" :min="0" :max="3650" controls-position="right" size="small"/>
        <span class="tip">0=全部清理；1~3650=删除超过该天数的记录</span>
      </div>

      <div class="clean-row">
        <el-button type="primary" size="small" :loading="loading" @click="loadCount">查询可清理数量</el-button>
        <span v-if="count !== null" :class="count > 0 ? 'warn' : 'ok'">
          {{ count > 0 ? (retainDays === 0 ? `将清理全部聊天记录共 ${count} 条` : `已超过 ${retainDays} 天的聊天记录共 ${count} 条`) : '当前没有需要清理的聊天记录' }}
        </span>
      </div>

      <el-divider/>

      <div class="clean-row">
        <el-button type="danger" size="small" :disabled="!count" :loading="cleaning" @click="doClean">
          清理超期聊天记录
        </el-button>
        <span class="tip">清理操作不可恢复，请谨慎执行</span>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import PageHead from "@/components/row/PageHead.vue";
import {ElMessage, ElMessageBox} from "element-plus";
import {reqCleanExpiredMsg, reqExpiredMsgNum} from "@/request/MsgApi";

// 默认保留90天
const retainDays = ref<number>(90)
const count = ref<number | null>(null)
const loading = ref(false)
const cleaning = ref(false)

const loadCount = () => {
  loading.value = true
  count.value = null
  reqExpiredMsgNum(retainDays.value).then(res => {
    count.value = Number(res) || 0
  }).catch((err) => {
    console.error('查询超期聊天数量失败:', err)
    ElMessage.error('查询失败：请确认后端服务是否正常运行')
    count.value = null
  }).finally(() => {
    loading.value = false
  })
}

const doClean = () => {
  ElMessageBox.confirm(
      retainDays.value === 0
          ? '将删除全部聊天记录及其图片/文件，不可恢复，是否继续？'
          : `将删除超过 ${retainDays.value} 天的聊天记录及其图片/文件，不可恢复，是否继续？`,
      '确认清理',
      {type: 'warning'}
  ).then(() => {
    cleaning.value = true
    reqCleanExpiredMsg(retainDays.value).then(res => {
      if (res.code === 1) {
        ElMessage.success('清理成功')
        count.value = 0
      } else {
        ElMessage.error(res.msg || '清理失败')
      }
    }).finally(() => {
      cleaning.value = false
    })
  }).catch(() => {
    // 取消
  })
}

onMounted(loadCount)
</script>

<style lang="scss" scoped>
.clean-card {
  max-width: 640px;
}

.clean-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.tip {
  color: #909399;
  font-size: 12px;
}

.warn {
  color: #e6a23c;
}

.ok {
  color: #67c23a;
}
</style>
