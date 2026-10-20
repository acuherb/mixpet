<template>
  <div class="rx-page">
    <div class="no-print rx-toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" :disabled="!req" @click="doPrint">打印申请单</el-button>
    </div>

    <div v-if="req" class="rx-sheet">
      <div class="rx-title">{{ req.type }}</div>
      <div class="rx-no">编号：{{ req.orderNo }}</div>

      <table class="rx-table">
        <tbody>
          <tr>
            <td class="k">畜主</td><td class="v">{{ req.clientName }}</td>
            <td class="k">宠物名</td><td class="v">{{ req.petName }}</td>
            <td class="k">品种</td><td class="v">{{ req.petVariety || '—' }}</td>
          </tr>
          <tr>
            <td class="k">性别</td><td class="v">{{ req.petSex == null ? '—' : (req.petSex ? '公' : '母') }}</td>
            <td class="k">年龄</td><td class="v">{{ req.petBirthday ? getAge(req.petBirthday) : '—' }}</td>
            <td class="k">体重</td><td class="v">{{ req.petWeight != null ? req.petWeight + 'kg' : '—' }}</td>
          </tr>
          <tr>
            <td class="k">芯片号</td><td class="v" colspan="3">{{ req.petChip || '—' }}</td>
            <td class="k">申请日期</td><td class="v">{{ fmt(req.createTime) }}</td>
          </tr>
          <tr>
            <td class="k">临床诊断</td><td class="v" colspan="5">{{ req.diagnosis || '—' }}</td>
          </tr>
        </tbody>
      </table>

      <div class="rx-rp">{{ req.type === '影像申请单' ? '检查项目' : '检验项目' }}</div>
      <!-- 影像：单项目 + 部位/目的 -->
      <template v-if="req.type === '影像申请单'">
        <table class="rx-table">
          <tbody>
            <tr>
              <td class="k">项目</td><td class="v">{{ req.items[0]?.name || '—' }}{{ req.items[0]?.group ? '（' + req.items[0].group + '）' : '' }}</td>
              <td class="k">部位/目的</td><td class="v">{{ req.examPart || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </template>
      <!-- 检验：多项 -->
      <table v-else class="rx-table rx-items">
        <thead>
          <tr>
            <th style="width: 34%">检验项</th>
            <th style="width: 18%">分组</th>
            <th style="width: 28%">参考范围</th>
            <th style="width: 20%">单位</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(it, i) in req.items" :key="i">
            <td>{{ it.name }}</td>
            <td>{{ it.group || '—' }}</td>
            <td>{{ it.refRange || '—' }}</td>
            <td>{{ it.unit || '—' }}</td>
          </tr>
          <tr v-if="!req.items.length"><td colspan="4" style="text-align:center;color:#999">无项目</td></tr>
        </tbody>
      </table>

      <div class="rx-amount">费用：￥{{ (req.fee ?? 0).toFixed(2) }}</div>

      <table class="rx-table rx-sign">
        <tbody>
          <tr>
            <td class="k">申请医生</td><td class="v">{{ req.doctorName }}</td>
            <td class="k">资格证号</td><td class="v">{{ req.doctorCert || '____' }}</td>
          </tr>
          <tr>
            <td class="k">备注/目的</td><td class="v" colspan="3">{{ req.remark || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else-if="!loading" class="no-print rx-empty">未找到该申请单</div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {reqImageRequisition} from "@/request/ImageApi";
import {reqLabRequisition} from "@/request/LabApi";
import {Requisition} from "@/model/entiy/Requisition";
import {getAge} from "@/utils/TimeUtil";

const route = useRoute()
const router = useRouter()
const req = ref<Requisition | null>(null)
const loading = ref(true)

onMounted(() => {
  const orderId = Number(route.query.orderId)
  const type = route.query.type === 'image' ? 'image' : 'lab'
  if (!orderId) { loading.value = false; return }
  const api = type === 'image' ? reqImageRequisition : reqLabRequisition
  api(orderId).then(res => {
    req.value = res
    loading.value = false
  })
})

const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)
const doPrint = (): void => window.print()
</script>

<style lang="scss" scoped>
.rx-page { padding: 20px; background: #f5f6fa; min-height: 100vh; }
.rx-toolbar { max-width: 720px; margin: 0 auto 16px; text-align: right; }
.rx-sheet {
  max-width: 720px; margin: 0 auto; background: #fff;
  border: 2px solid #333; padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.rx-title { text-align: center; font-size: 26px; font-weight: 800; letter-spacing: 4px; margin-bottom: 4px; }
.rx-no { text-align: right; font-size: 13px; color: #555; margin-bottom: 12px; }
.rx-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
.rx-table td, .rx-table th { border: 1px solid #333; padding: 7px 8px; font-size: 14px; }
.rx-table .k { background: #f0f2f5; width: 84px; text-align: center; white-space: nowrap; }
.rx-table .v { min-width: 60px; }
.rx-items th { background: #f0f2f5; text-align: center; }
.rx-rp { font-weight: 700; margin: 6px 0; }
.rx-amount { text-align: right; font-size: 15px; font-weight: 600; margin: 6px 0 12px; }
.rx-sign td { height: 42px; }
.rx-empty { text-align: center; color: #999; margin-top: 60px; }

@media print {
  .rx-page { padding: 0; background: #fff; }
  .no-print { display: none !important; }
  .rx-sheet { box-shadow: none; border: 2px solid #000; max-width: 100%; margin: 0; }
}
</style>
