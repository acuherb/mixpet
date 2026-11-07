<template>
  <div class="rx-page a4">
    <div class="no-print rx-toolbar">
      <el-button @click="router.back()">返回</el-button>
      <el-button type="primary" :disabled="!req" @click="doPrint">打印申请单</el-button>
    </div>

    <div v-if="req" class="rx-sheet">
      <!-- 品牌头：医院名 + 品牌渐变；单据类型与编号右侧 -->
      <div class="rx-head" :style="{background: `linear-gradient(135deg, ${brand} 0%, #4ba3e0 100%)`}">
        <div>
          <div class="rx-hosp">{{ hosp.name || '动物诊疗机构' }}</div>
          <div class="rx-doc-tit">{{ req.type === 'image' ? '影像检查申请单' : '检验申请单' }}</div>
        </div>
        <div class="rx-meta">
          <span class="rx-copy-tag">送 检 联</span>
          <div class="rx-no">编号 {{ req.orderNo }}</div>
        </div>
      </div>

      <!-- 前记：字段网格 -->
      <div class="rx-sec">
        <div class="rx-sec-tit">前 记</div>
        <div class="rx-grid">
          <div class="rx-field"><span class="rx-k">畜主</span><span class="rx-v">{{ req.clientName }}</span></div>
          <div class="rx-field"><span class="rx-k">宠物名</span><span class="rx-v">{{ req.petName }}</span></div>
          <div class="rx-field"><span class="rx-k">品种</span><span class="rx-v">{{ req.petVariety || '—' }}</span></div>
          <div class="rx-field"><span class="rx-k">性别</span><span class="rx-v">{{ req.petSex == null ? '—' : (req.petSex ? '公' : '母') }}</span></div>
          <div class="rx-field"><span class="rx-k">年龄</span><span class="rx-v">{{ req.petBirthday ? getAge(req.petBirthday) : '—' }}</span></div>
          <div class="rx-field"><span class="rx-k">体重</span><span class="rx-v">{{ req.petWeight != null ? req.petWeight + ' kg' : '—' }}</span></div>
          <div class="rx-field wide"><span class="rx-k">芯片号</span><span class="rx-v">{{ req.petChip || '—' }}</span></div>
          <div class="rx-field"><span class="rx-k">申请日期</span><span class="rx-v">{{ fmt(req.createTime) }}</span></div>
          <div class="rx-field full"><span class="rx-k">临床诊断</span><span class="rx-v">{{ req.diagnosis || '—' }}</span></div>
        </div>
      </div>

      <!-- 正文：检查/检验项目 -->
      <div class="rx-sec">
        <div class="rx-sec-tit">{{ req.type === 'image' ? '检 查 项 目' : '检 验 项 目' }}</div>
        <!-- 影像：单项目 + 部位/目的 -->
        <template v-if="req.type === 'image'">
          <div class="rx-grid">
            <div class="rx-field wide"><span class="rx-k">项目</span><span class="rx-v">{{ req.items[0]?.name || '—' }}{{ req.items[0]?.group ? '（' + req.items[0].group + '）' : '' }}</span></div>
            <div class="rx-field"><span class="rx-k">费用</span><span class="rx-v">￥{{ (req.fee ?? 0).toFixed(2) }}</span></div>
            <div class="rx-field full"><span class="rx-k">部位/目的</span><span class="rx-v">{{ req.examPart || '—' }}</span></div>
            <div class="rx-field full"><span class="rx-k">备注</span><span class="rx-v">{{ req.remark || '—' }}</span></div>
          </div>
        </template>
        <!-- 检验：多项表 -->
        <table v-else class="rx-items">
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
            <tr class="rx-amount-row"><td colspan="4">费用：￥{{ (req.fee ?? 0).toFixed(2) }}</td></tr>
          </tbody>
        </table>
      </div>

      <!-- 后记：申请医生 -->
      <div class="rx-sec">
        <div class="rx-sec-tit">后 记</div>
        <div class="rx-sign">
          <span class="rx-sign-item"><span class="rx-k">申请医生</span><span class="rx-v">{{ req.doctorName }}</span></span>
          <span class="rx-sign-item"><span class="rx-k">资格证号</span><span class="rx-v">{{ req.doctorCert || '____' }}</span></span>
          <span class="rx-sign-item"><span class="rx-k">接收时间</span><span class="rx-v">（科室填写）</span></span>
        </div>
      </div>
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
import {useBrand} from "@/utils/UseBrand";
import {ElMessage} from "element-plus";

const route = useRoute()
const router = useRouter()
const req = ref<Requisition | null>(null)
const loading = ref(true)
const {hosp, brand, load: loadBrand} = useBrand()

onMounted(() => {
  const orderId = Number(route.query.orderId)
  const type = route.query.type === 'image' ? 'image' : 'lab'
  loadBrand()
  if (!orderId) { loading.value = false; return }
  const api = type === 'image' ? reqImageRequisition : reqLabRequisition
  api(orderId).then(res => {
    req.value = res
    loading.value = false
  }).catch(() => {
    loading.value = false
    ElMessage.error('申请单数据加载失败，请返回重试')
  })
})

const fmt = (t: string | null): string => (t || '').replace('T', ' ').slice(0, 16)
const doPrint = (): void => window.print()
</script>

<style lang="scss" scoped>
/* 法定单据现代设计语言（与处方笺/同意书同一套：品牌头/卡片分区/字段网格） */
@import "@/styles/rx-modern.scss";

/* 申请单私有：A4 竖版 */
@page {
  size: A4;
  margin: 0;
}
.rx-sheet {
  min-height: 297mm;
}
</style>
