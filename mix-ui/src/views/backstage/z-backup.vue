<template>
  <div>
    <PageHead>数据备份</PageHead>

    <!-- 说明与安全警示 -->
    <el-alert :closable="false" show-icon style="margin-bottom: 12px" type="info"
              title="备份为全库 SQL（mysqldump），含全部业务数据与账号密码哈希——文件请妥善保管，建议定期下载到本机/U盘/另一台机器"/>
    <el-alert :closable="false" show-icon style="margin-bottom: 16px" type="warning"
              title="还原是危险操作：会用备份内容覆盖当前全部数据。本功能已设四道闸——仅超管可还原、需输入文件名二次确认、还原前自动为当前数据创建快照（出事可回退）、备份/还原互斥。"/>

    <!-- 操作栏 -->
    <div class="bk-bar">
      <el-button :icon="Download" round type="primary" :loading="creating" @click="doCreate">立即备份</el-button>
      <span class="bk-tip">每天凌晨 2:00 自动备份（保留最近 7 份，仅清理 auto_ 前缀文件）；备份保存在服务器 db-backup/runtime/</span>
    </div>

    <!-- 备份列表 -->
    <el-table :data="list" border empty-text="暂无备份，点上方「立即备份」创建第一份">
      <el-table-column label="文件名" min-width="220" prop="name" show-overflow-tooltip>
        <template #default="s">
          {{ s.row.name }}
          <el-tag v-if="s.row.snapshot" size="small" type="warning" effect="plain" style="margin-left:6px">还原前快照</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="110">
        <template #default="s">{{ humanSize(s.row.size) }}</template>
      </el-table-column>
      <el-table-column label="时间" prop="time" width="170"/>
      <el-table-column fixed="right" label="操作" width="230">
        <template #default="s">
          <el-button size="small" type="primary" link @click="doDownload(s.row.name)">下载</el-button>
          <el-button v-if="isAdmin" size="small" type="danger" link @click="doRestore(s.row)">还原</el-button>
          <el-button size="small" type="info" link @click="doDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 还原确认弹窗：二次输入文件名 -->
    <el-dialog v-model="rsView" title="还原数据（危险操作）" width="92%" style="max-width: 480px"
               :close-on-click-modal="false">
      <el-alert :closable="false" show-icon type="error" style="margin-bottom:12px"
                title="将用该备份覆盖当前全部数据！还原前会自动为当前数据创建快照（pre_restore_ 前缀），如结果不符预期可用快照再次还原回退。"/>
      <div class="rs-target">目标备份：<b>{{ rsRow?.name }}</b></div>
      <el-form label-width="0">
        <el-form-item>
          <el-input v-model="rsConfirm" clearable placeholder="请完整输入上方文件名以确认操作"/>
        </el-form-item>
      </el-form>
      <div class="rs-tip">仅超管可执行；确认后立即开始，期间请勿关闭页面。</div>
      <template #footer>
        <el-button round @click="rsView = false">取消</el-button>
        <el-button round type="danger" :loading="rsSaving" @click="submitRestore">确认还原</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import PageHead from "@/components/row/PageHead.vue";
import {Download} from '@element-plus/icons-vue'
import {onMounted, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {BackupFileVo, reqBackupCreate, reqBackupDel, reqBackupList, reqBackupRestore} from "@/request/BackupApi";
import {reqGetLoginD} from "@/request/DoctorApi";

const list = ref<BackupFileVo[]>([])
const creating = ref(false)
const isAdmin = ref(false)

const humanSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

const loadList = (): void => {
  reqBackupList().then(res => { list.value = res || [] })
}

const doCreate = (): void => {
  creating.value = true
  reqBackupCreate().then(res => {
    creating.value = false
    if (res.code === 1) { ElMessage.success(res.msg || '备份成功'); loadList() }
    else ElMessage.error(res.msg || '备份失败')
  }).catch(() => { creating.value = false })
}

const doDownload = (name: string): void => {
  // 走浏览器下载（带 Cookie 会话）
  const a = document.createElement('a')
  a.href = '/api/backup/download?name=' + encodeURIComponent(name)
  a.download = name
  a.click()
}

const doDelete = (row: BackupFileVo): void => {
  ElMessageBox.confirm(
      `确认删除备份「${row.name}」？删除后不可恢复。`,
      '删除备份',
      {confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    reqBackupDel(row.name).then(res => {
      if (res.code === 1) { ElMessage.success('已删除'); loadList() }
      else ElMessage.error(res.msg || '删除失败')
    })
  }).catch(() => ElMessage.info('已取消'))
}

// 还原：二次输入文件名确认
const rsView = ref(false)
const rsSaving = ref(false)
const rsRow = ref<BackupFileVo | null>(null)
const rsConfirm = ref('')
const doRestore = (row: BackupFileVo): void => {
  rsRow.value = row
  rsConfirm.value = ''
  rsView.value = true
}
const submitRestore = (): void => {
  if (!rsRow.value) return
  if (rsConfirm.value.trim() !== rsRow.value.name) {
    ElMessage.warning('输入的文件名与目标不一致，已中止')
    return
  }
  rsSaving.value = true
  reqBackupRestore(rsRow.value.name, rsConfirm.value.trim()).then(res => {
    rsSaving.value = false
    if (res.code === 1) {
      ElMessage.success(res.msg || '还原成功')
      rsView.value = false
      loadList()
    } else ElMessage.error(res.msg || '还原失败')
  }).catch(() => { rsSaving.value = false })
}

onMounted(() => {
  // 还原按钮仅超管（authLv>=8）可见；后端 /backup/restore 也限 ADMIN 双保险
  reqGetLoginD().then(res => { isAdmin.value = (res.authLv ?? 0) >= 8 }).catch(() => {})
  loadList()
})
</script>

<style lang="scss" scoped>
.bk-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;

  .bk-tip { font-size: 12px; color: #909399; }
}

.rs-target {
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
  word-break: break-all;
}

.rs-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
