<template>
  <div class="rx-page">
    <div class="no-print rx-toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" :disabled="!c" @click="doPrint">打印同意书</el-button>
    </div>

    <div v-if="c" class="rx-sheet">
      <div class="rx-title">手术 / 麻醉知情同意书</div>
      <div class="rx-no">编号：{{ c.consentNo }}</div>

      <table class="rx-table">
        <tbody>
        <tr>
          <td class="k">畜主</td><td class="v">{{ c.clientName }}</td>
          <td class="k">宠物名</td><td class="v">{{ c.petName }}</td>
          <td class="k">品种</td><td class="v">{{ c.petVariety || '—' }}</td>
        </tr>
        <tr>
          <td class="k">性别</td><td class="v">{{ c.petSex == null ? '—' : (c.petSex ? '公' : '母') }}</td>
          <td class="k">体重</td><td class="v">{{ c.petWeight != null ? c.petWeight + 'kg' : '—' }}</td>
          <td class="k">芯片号</td><td class="v">{{ c.petChip || '—' }}</td>
        </tr>
        <tr>
          <td class="k">临床诊断</td><td class="v" colspan="5">{{ c.diagnosis || '—' }}</td>
        </tr>
        <tr>
          <td class="k">手术名称</td><td class="v" colspan="3">{{ c.surgeryName || '—' }}</td>
          <td class="k">麻醉方式</td><td class="v">{{ c.anesthesiaType || '—' }}</td>
        </tr>
        <tr>
          <td class="k">预约时间</td><td class="v" colspan="3">{{ fmt(c.scheduledTime) || '—' }}</td>
          <td class="k">主刀兽医</td><td class="v">{{ c.doctorName || '____' }}</td>
        </tr>
        </tbody>
      </table>

      <div class="rx-rp">风险与注意事项告知：</div>
      <div class="rx-risks">{{ c.risks || '—' }}</div>

      <div class="rx-agree">畜主意见：<b>{{ c.agree ? '同意实施手术及麻醉' : '不同意' }}</b></div>

      <table class="rx-table rx-sign">
        <tbody>
        <tr>
          <td class="k">签字人</td><td class="v">{{ c.signer || '____' }}</td>
          <td class="k">签署时间</td><td class="v" colspan="3">{{ fmt(c.signedTime) || '____' }}</td>
        </tr>
        <tr>
          <td class="k">经办医护</td><td class="v">{{ c.operator || '____' }}</td>
          <td class="k">与宠物关系</td><td class="v" colspan="3">畜主</td>
        </tr>
        </tbody>
      </table>
    </div>

    <div v-else-if="!loading" class="no-print rx-empty">未找到该同意书（可能尚未签署）</div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {reqConsentSheet} from "@/request/SurgeryApi";
import {ConsentSheet} from "@/model/VO/ConsentSheet";

const route = useRoute()
const router = useRouter()
const c = ref<ConsentSheet | null>(null)
const loading = ref(true)

onMounted(() => {
  const surgeryId = Number(route.query.surgeryId)
  if (!surgeryId) {
    loading.value = false
    return
  }
  reqConsentSheet(surgeryId).then(res => {
    c.value = res
    loading.value = false
  })
})

const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)
const doPrint = (): void => window.print()
</script>

<style lang="scss" scoped>
.rx-page { padding: 20px; background: #f5f6fa; min-height: 100vh; }
.rx-toolbar { max-width: 720px; margin: 0 auto 16px; text-align: right; }
.rx-sheet { max-width: 720px; margin: 0 auto; background: #fff; border: 2px solid #333; padding: 24px 28px; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); }
.rx-title { text-align: center; font-size: 26px; font-weight: 800; letter-spacing: 4px; margin-bottom: 4px; }
.rx-no { text-align: right; font-size: 13px; color: #555; margin-bottom: 12px; }
.rx-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
.rx-table td, .rx-table th { border: 1px solid #333; padding: 7px 8px; font-size: 14px; }
.rx-table .k { background: #f0f2f5; width: 84px; text-align: center; white-space: nowrap; }
.rx-table .v { min-width: 60px; }
.rx-rp { font-weight: 700; margin: 6px 0; }
.rx-risks { border: 1px solid #333; min-height: 130px; padding: 10px; font-size: 14px; line-height: 1.8; white-space: pre-wrap; margin-bottom: 12px; }
.rx-agree { font-size: 15px; margin: 4px 0 12px; }
.rx-sign td { height: 44px; }
.rx-empty { text-align: center; color: #999; margin-top: 60px; }

@media print {
  .rx-page { padding: 0; background: #fff; }
  .no-print { display: none !important; }
  .rx-sheet { box-shadow: none; border: 2px solid #000; max-width: 100%; margin: 0; }
}
</style>
