<template>
  <!-- 统一报告外壳：品牌头 / 患者信息条 / 正文分区 / 签名区 / 页脚。
       各文书只提供数据与正文 slot，不再各自拼一套公文表格。 -->
  <div class="rm-page" :class="{'rm-ticket': ticket}">
    <div v-if="$slots.toolbar" class="no-print rm-toolbar"><slot name="toolbar"/></div>

    <div class="rm-sheet" :style="brandStyle">
      <header class="rm-head">
        <div>
          <div class="rm-hosp">{{ hospName || '动物诊疗机构' }}</div>
          <div class="rm-doc-title">{{ title }}</div>
        </div>
        <div class="rm-head-meta">
          <span v-if="docNo" class="rm-doc-no">编号 {{ docNo }}</span>
          <div>{{ dateText }}</div>
        </div>
      </header>

      <section v-if="fields?.length" class="rm-patient">
        <div v-for="(f, i) in fields" :key="i" class="rm-field">
          <span class="rm-k">{{ f.label }}</span>
          <span class="rm-v">{{ f.value === null || f.value === undefined || f.value === '' ? '—' : f.value }}</span>
        </div>
      </section>

      <main class="rm-body"><slot/></main>

      <section v-if="signers?.length" class="rm-sign">
        <div v-for="(s, i) in signers" :key="i" class="rm-sign-item">
          <span class="rm-k">{{ s.label }}</span>
          <span class="rm-v">{{ s.value || '—' }}</span>
        </div>
      </section>

      <footer class="rm-foot">
        <span>{{ footer || '本单据由本院诊疗系统生成，仅供本院诊疗与宠主留存参考。' }}</span>
        <span v-if="printTime" class="rm-print-time">打印时间 {{ nowText() }}</span>
      </footer>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed} from 'vue'

type Field = {label: string; value?: string | number | null}

const props = defineProps<{
  title: string
  docNo?: string
  hospName?: string
  brand?: string
  fields?: Field[]
  signers?: Field[]
  footer?: string
  printTime?: boolean
  ticket?: boolean
}>()

const brandStyle = computed<Record<string, string>>(() => ({
  '--rm-brand': props.brand || '#1E6FA8'
}))
const dateText = new Date().toLocaleDateString('zh-CN')
const nowText = (): string => new Date().toLocaleString('zh-CN')
</script>
