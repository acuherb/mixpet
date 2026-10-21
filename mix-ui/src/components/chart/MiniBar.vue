<template>
  <!-- 统一迷你柱状图：固定柱宽/网格参考线/有值最小可见/0值留空/空态，替换原各页手写柱状 -->
  <div v-if="items.length" class="mini-bar">
    <div class="mb-grid"><i/><i/><i/><i/></div>
    <div v-for="(it, i) in items" :key="i" class="mb-col" :title="`${it.name}：${it.label ?? it.value}`">
      <span class="mb-val">{{ it.label ?? '' }}</span>
      <div class="mb-track"><div class="mb-fill" :style="{height: pct(it.value) + '%'}"/></div>
      <span class="mb-name">{{ it.name }}</span>
    </div>
  </div>
  <el-empty v-else :description="emptyText"/>
</template>

<script lang="ts" setup>
import {computed} from 'vue'

interface BarItem {
  name: string
  value: number
  /** 柱顶数值文案（如 ￥123.00），缺省不显示 */
  label?: string
}

const props = withDefaults(defineProps<{ items: BarItem[]; emptyText?: string }>(), {
  emptyText: '暂无数据'
})

// 归一化柱高：有值最小 3% 可见（杜绝 1px 细线），0 值留空；全 0 时 max=1 防除零
const max = computed(() => Math.max(1, ...props.items.map(i => i.value || 0)))
const pct = (v: number): number => (v > 0 ? Math.max(3, Math.round((v / max.value) * 100)) : 0)
</script>

<style lang="scss" scoped>
.mini-bar {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  height: 200px;
  padding-top: 10px;
  overflow-x: auto;

  /* 4 等分网格参考线 */
  .mb-grid {
    position: absolute;
    inset: 10px 0 26px 0;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    pointer-events: none;

    i { border-top: 1px dashed #ebeef5; }
  }

  .mb-col {
    flex: 1;
    min-width: 36px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    height: 100%;
  }

  .mb-val {
    font-size: 10px;
    color: #909399;
    white-space: nowrap;
  }

  .mb-track {
    width: 100%;
    max-width: 34px;
    flex: 1;
    display: flex;
    align-items: flex-end;
    background: #f5f6f8;
    border-radius: 6px 6px 0 0;
  }

  .mb-fill {
    width: 100%;
    background: linear-gradient(180deg, var(--brand), var(--brand-dark));
    border-radius: 6px 6px 0 0;
    transition: height .3s;
  }

  .mb-name {
    font-size: 11px;
    color: #606266;
    white-space: nowrap;
  }
}
</style>
