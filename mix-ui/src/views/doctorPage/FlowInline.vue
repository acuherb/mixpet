<template>
  <div class="flow-inline">
    <div class="fi-bar">
      <el-button size="small" @click="backFlow"><el-icon><ArrowLeft/></el-icon> 返回诊疗流</el-button>
      <span class="fi-title">{{ title }}</span>
      <el-button size="small" plain @click="openStandalone">独立页打开</el-button>
    </div>
    <!-- 当前患者上下文：卡片带过来的 petName/clientName/单号，避免同一只宠物被反复搜索或找错人 -->
    <div v-if="patientName || idTags.length" class="fi-patient">
      正在处理：<b>{{ patientName || '—' }}</b>
      <span v-if="clientName"> · {{ clientName }}</span>
      <span v-for="tag in idTags" :key="tag" class="fi-id">{{ tag }}</span>
    </div>
    <div class="fi-body">
      <component :is="comp" v-if="comp" :key="route.fullPath"/>
      <el-empty v-else description="该页面暂不支持流内打开，请点「独立页打开」"/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed, defineAsyncComponent} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ArrowLeft} from '@element-plus/icons-vue'

/**
 * 流内联深页容器：按 __flowTarget 从白名单解析组件并就地渲染。
 * 白名单是刻意的——不是所有页面都适合在 76% 抽屉里跑（如登录/404/独立大页）。
 * 原目标 query 由守卫平铺到当前路由 query，深页 useRoute().query 照常可读 recordId 等参数。
 */
const route = useRoute()
const router = useRouter()

const loaders: Record<string, () => Promise<any>> = {
  '/doctorPage/d-appoint': () => import('@/views/doctorPage/d-appoint.vue'),
  '/doctorPage/d-record': () => import('@/views/doctorPage/d-record.vue'),
  '/doctorPage/d-chat': () => import('@/views/doctorPage/d-chat.vue'),
  '/doctorPage/d-lab': () => import('@/views/doctorPage/d-lab.vue'),
  '/doctorPage/d-image': () => import('@/views/doctorPage/d-image.vue'),
  '/doctorPage/d-rx': () => import('@/views/doctorPage/d-rx.vue'),
  '/doctorPage/d-recordPrint': () => import('@/views/doctorPage/d-recordPrint.vue'),
  '/doctorPage/z-examResult': () => import('@/views/backstage/z-examResult.vue'),
  '/doctorPage/y-appointmentList': () => import('@/views/backstage/y-appointmentList.vue'),
  '/doctorPage/z-surgery': () => import('@/views/backstage/z-surgery.vue'),
  '/doctorPage/z-hospital': () => import('@/views/backstage/z-hospital.vue'),
  '/doctorPage/z-immune': () => import('@/views/backstage/z-immune.vue'),
  '/doctorPage/z-follow': () => import('@/views/backstage/z-follow.vue'),
  '/doctorPage/y-billList': () => import('@/views/backstage/y-billList.vue'),
  '/doctorPage/z-rxArchive': () => import('@/views/backstage/z-rxArchive.vue'),
  '/doctorPage/z-examReport': () => import('@/views/backstage/z-examReport.vue'),
  '/doctorPage/z-requisition': () => import('@/views/backstage/z-requisition.vue'),
  '/doctorPage/z-labTest': () => import('@/views/backstage/z-labTest.vue'),
  '/doctorPage/z-imageTest': () => import('@/views/backstage/z-imageTest.vue'),
  '/doctorPage/z-template': () => import('@/views/backstage/z-template.vue'),
  '/doctorPage/z-consent': () => import('@/views/backstage/z-consent.vue'),
  '/doctorPage/z-remind': () => import('@/views/backstage/z-remind.vue'),
  '/doctorPage/z-refRange': () => import('@/views/backstage/z-refRange.vue'),
  '/doctorPage/z-diagnosis': () => import('@/views/backstage/z-diagnosis.vue'),
}

const titles: Record<string, string> = {
  'd-appoint': '接诊', 'd-lab': '检验开单', 'd-image': '影像开单', 'd-rx': '处方',
  'z-examResult': '医技结果', 'y-billList': '收银', 'z-follow': '随访',
  'z-surgery': '手术记录', 'z-hospital': '入院登记', 'z-immune': '免疫接种',
  'd-record': '病例中心', 'd-recordPrint': '病历打印', 'd-chat': '咨询',
  'y-appointmentList': '挂号列表', 'z-examReport': '报告单打印', 'z-requisition': '申请单打印',
  'z-rxArchive': '处方笺档案', 'z-labTest': '检验项字典', 'z-imageTest': '影像项字典',
  'z-template': '病历模板', 'z-consent': '知情同意书', 'z-remind': '复诊/疫苗提醒',
  'z-refRange': '参考区间', 'z-diagnosis': '诊断字典',
}

const target = computed<string>(() => String(route.query.__flowTarget || ''))
const title = computed<string>(() => {
  const leaf = target.value.split('/').pop() || ''
  return titles[leaf] || leaf || '流内处理'
})
const comp = computed(() => {
  const loader = loaders[target.value]
  return loader ? defineAsyncComponent(loader) : null
})

const patientName = computed<string>(() => String(route.query.petName || route.query.seaName || ''))
const clientName = computed<string>(() => String(route.query.clientName || ''))
const idTags = computed<string[]>(() => {
  const pairs: [string, string][] = [
    ['appointmentId', '挂号'], ['recordId', '病历'], ['orderId', '申请单'],
    ['billId', '账单'], ['followId', '随访'],
  ]
  return pairs.filter(([k]) => route.query[k]).map(([k, label]) => `${label} #${route.query[k]}`)
})

const backFlow = (): void => { router.push('/doctorPage/d-flow') }

/** 独立页打开：拼回原始 query（去掉内部标记），__flowStandalone 让守卫放行，避免又被拦回抽屉 */
const openStandalone = (): void => {
  const q: Record<string, any> = {...route.query, __flowStandalone: '1'}
  delete q.__flowTarget
  router.push({path: target.value || '/doctorPage/d-flow', query: q})
}
</script>

<style lang="scss" scoped>
.flow-inline { display: flex; flex-direction: column; height: 100%; }
.fi-bar { display: flex; align-items: center; gap: 12px; padding: 0 0 12px; border-bottom: 1px solid #e4e7ed; margin-bottom: 12px; }
.fi-title { flex: 1; font-weight: 600; color: #1F2D3D; font-size: 15px; }
.fi-patient { background: #f4f8ff; border: 1px solid #d9e6ff; border-radius: 6px; padding: 8px 12px; margin-bottom: 10px; color: #3f5b8b; font-size: 13px; }
.fi-id { display: inline-block; margin-left: 10px; padding: 1px 8px; border-radius: 10px; background: #e8f0ff; color: #2f5597; }
.fi-body { flex: 1; overflow: auto; }
</style>
