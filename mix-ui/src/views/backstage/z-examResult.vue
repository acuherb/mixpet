<template>
  <!-- 医技结果录入统一入口：检验 / 影像两个页签（直接复用原录入页组件，逻辑零重复） -->
  <div>
    <PageHead>医技结果录入</PageHead>
    <el-alert :closable="false" show-icon style="margin: 0 0 12px" type="info"
              title="检验与影像结果在此统一录入；开单超 24 小时未出结果的单据会标红提醒，请优先处理"/>
    <el-tabs v-model="tab" type="border-card">
      <!-- name 必须与 script 里的 'lab'/'image' 对应：早期漏写 name，v-model 匹配不上索引名，
           element-plus 把激活态落到未渲染的 pane 上，用户看到整片空白（2026-10-23 实测修复） -->
      <el-tab-pane name="lab" label="检验结果">
        <template #label>
          <span>检验结果<el-badge v-if="labPending > 0" :value="labPending" type="warning" style="margin-left:6px"/></span>
        </template>
        <!-- v-if 切换即重挂载：子页 onMounted 会按 URL status 参数自行筛选 -->
        <LabResultPanel v-if="tab === 'lab'" @pending-count="labPending = $event"/>
      </el-tab-pane>
      <el-tab-pane name="image" label="影像报告">
        <template #label>
          <span>影像报告<el-badge v-if="imagePending > 0" :value="imagePending" type="warning" style="margin-left:6px"/></span>
        </template>
        <ImageResultPanel v-if="tab === 'image'" @pending-count="imagePending = $event"/>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts" setup>
import {ref} from 'vue'
import {useRoute} from 'vue-router'
import PageHead from "@/components/row/PageHead.vue";
import LabResultPanel from "@/views/backstage/z-labResult.vue";
import ImageResultPanel from "@/views/backstage/z-imageResult.vue";

const route = useRoute()
// 从医生工作台/看板进入可带 tab=lab|image（缺省 lab）；status 筛选参数由子页自行读取
const tab = ref<'lab' | 'image'>(route.query.tab === 'image' ? 'image' : 'lab')
// 检验待出结果数（子页 emit 回填，用于页签角标）
const labPending = ref(0)
// 影像待出报告数
const imagePending = ref(0)
</script>
