<template>
  <div class="rx-page a4">
    <div class="no-print rx-toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" :disabled="!c" @click="doPrint">打印同意书</el-button>
    </div>

    <div v-if="c" class="rx-sheet">
      <!-- 品牌头 -->
      <div class="rx-head" :style="{background: `linear-gradient(135deg, ${brand} 0%, #4ba3e0 100%)`}">
        <div>
          <div class="rx-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
          <div class="rx-doc-tit">手术 / 麻醉知情同意书</div>
        </div>
        <div class="rx-meta">
          <span class="rx-copy-tag">畜 主 留 存</span>
          <div class="rx-no">编号 {{ c.consentNo }}</div>
        </div>
      </div>

      <!-- 前记 -->
      <div class="rx-sec">
        <div class="rx-sec-tit">前 记</div>
        <div class="rx-grid">
          <div class="rx-field"><span class="rx-k">畜主</span><span class="rx-v">{{ c.clientName }}</span></div>
          <div class="rx-field"><span class="rx-k">宠物名</span><span class="rx-v">{{ c.petName }}</span></div>
          <div class="rx-field"><span class="rx-k">品种</span><span class="rx-v">{{ c.petVariety || '—' }}</span></div>
          <div class="rx-field"><span class="rx-k">性别</span><span class="rx-v">{{ c.petSex == null ? '—' : (c.petSex ? '公' : '母') }}</span></div>
          <div class="rx-field"><span class="rx-k">体重</span><span class="rx-v">{{ c.petWeight != null ? c.petWeight + ' kg' : '—' }}</span></div>
          <div class="rx-field"><span class="rx-k">芯片号</span><span class="rx-v">{{ c.petChip || '—' }}</span></div>
          <div class="rx-field wide"><span class="rx-k">临床诊断</span><span class="rx-v">{{ c.diagnosis || '—' }}</span></div>
          <div class="rx-field"><span class="rx-k">主刀兽医</span><span class="rx-v">{{ c.doctorName || '____' }}</span></div>
          <div class="rx-field wide"><span class="rx-k">手术名称</span><span class="rx-v">{{ c.surgeryName || '—' }}</span></div>
          <div class="rx-field"><span class="rx-k">麻醉方式</span><span class="rx-v">{{ c.anesthesiaType || '—' }}</span></div>
          <div class="rx-field"><span class="rx-k">预约时间</span><span class="rx-v">{{ fmt(c.scheduledTime) || '—' }}</span></div>
        </div>
      </div>

      <!-- 风险与注意事项告知（法律核心段：原文保留） -->
      <div class="rx-sec">
        <div class="rx-sec-tit">风 险 与 注 意 事 项 告 知</div>
        <div class="rx-risks">{{ c.risks || '—' }}</div>
      </div>

      <!-- 畜主意见 -->
      <div class="rx-sec">
        <div class="rx-sec-tit">畜 主 意 见</div>
        <div class="rx-agree">
          本人已阅读并理解上述风险与注意事项，<b :class="{no: !c.agree}">{{ c.agree ? '同意实施手术及麻醉' : '不同意' }}</b>
        </div>
      </div>

      <!-- 签字区 -->
      <div class="rx-sec">
        <div class="rx-sec-tit">签 字</div>
        <div class="rx-sign">
          <span class="rx-sign-item"><span class="rx-k">签字人</span><span class="rx-v">{{ c.signer || '____' }}</span></span>
          <span class="rx-sign-item"><span class="rx-k">与宠物关系</span><span class="rx-v">畜主</span></span>
          <span class="rx-sign-item"><span class="rx-k">签署时间</span><span class="rx-v">{{ fmt(c.signedTime) || '____' }}</span></span>
          <span class="rx-sign-item"><span class="rx-k">经办医护</span><span class="rx-v">{{ c.operator || '____' }}</span></span>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="no-print rx-empty">未找到该同意书（可能尚未签署）</div>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {reqConsentSheet} from "@/request/SurgeryApi";
import {ConsentSheet} from "@/model/VO/ConsentSheet";
import {useBrand} from "@/utils/UseBrand";

const route = useRoute()
const router = useRouter()
const c = ref<ConsentSheet | null>(null)
const loading = ref(true)
const {hosp, brand, load: loadBrand} = useBrand()

onMounted(() => {
  const surgeryId = Number(route.query.surgeryId)
  loadBrand()
  if (!surgeryId) { loading.value = false; return }
  reqConsentSheet(surgeryId).then(res => {
    c.value = res
    loading.value = false
  })
})

const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)
const doPrint = (): void => window.print()
</script>

<style lang="scss" scoped>
/* 法定单据现代设计语言（与处方笺/申请单同一套） */
@import "@/styles/rx-modern.scss";

/* 同意书私有：A4 竖版 + 风险告知段排版 */
@page {
  size: A4;
  margin: 0;
}
.rx-sheet { min-height: 297mm; }

.rx-risks {
  font-size: 13px;
  line-height: 2;
  color: #1F2D3D;
  white-space: pre-wrap;
  background: #F8FAFC;
  border: 1px solid #EBEEF5;
  border-radius: 10px;
  padding: 14px 16px;
}

.rx-agree {
  font-size: 14px;
  color: #1F2D3D;
  line-height: 2;

  b { color: #67C23A; font-size: 15px; letter-spacing: 1px; }
  b.no { color: #F56C6C; }
}
</style>
