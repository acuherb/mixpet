<template>
  <!-- 挂号成功凭证卡：替代「toast 一闪而过」，明确挂上了/几点/找谁/单号 -->
  <el-dialog v-model="view" width="400px" :show-close="true" title="挂号成功">
    <div class="ticket">
      <div class="tk-head">
        <el-icon class="tk-ico"><CircleCheckFilled/></el-icon>
        <span class="tk-no">单号 No.{{ ticket.appointId }}</span>
      </div>
      <div class="tk-grid">
        <div class="tk-field"><span class="k">就诊时间</span><span class="v">{{ shortT(ticket.date) }}</span></div>
        <div class="tk-field"><span class="k">宠物</span><span class="v">{{ ticket.petName || '—' }}</span></div>
        <div class="tk-field"><span class="k">主人</span><span class="v">{{ ticket.clientName || '—' }}</span></div>
        <div class="tk-field"><span class="k">科室</span><span class="v">{{ ticket.departmentName || '—' }}</span></div>
        <div class="tk-field"><span class="k">医生</span><span class="v">{{ ticket.doctorName || '—' }}</span></div>
      </div>
      <p class="tk-tip">已加入接诊队列；如需改期或取消请提前联系前台或到挂号列表操作</p>
    </div>
    <template #footer>
      <el-button @click="view = false">完成</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {CircleCheckFilled} from '@element-plus/icons-vue'
import {ref} from 'vue'

/** 挂号凭证卡：管理端挂号列表与医生端挂号弹窗共用 */
const view = ref(false)

interface AppointTicketData {
  appointId: number | null
  date: string
  petName: string
  clientName: string
  departmentName: string
  doctorName: string
}

const ticket = ref<AppointTicketData>({
  appointId: null, date: '', petName: '', clientName: '',
  departmentName: '', doctorName: ''
})

const open = (data: AppointTicketData): void => {
  ticket.value = data
  view.value = true
}
defineExpose({open})

const shortT = (t: string): string => (t || '').replace('T', ' ').slice(0, 16)
</script>

<style scoped>
.ticket {
  text-align: center;
}
.tk-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}
.tk-ico {
  font-size: 34px;
  color: #67c23a;
}
.tk-no {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
.tk-grid {
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 8px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
  text-align: left;
}
.tk-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.tk-field .k {
  font-size: 12px;
  color: #909399;
}
.tk-field .v {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.tk-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.5;
}
</style>
