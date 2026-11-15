<template>
  <!-- 统一的返回按钮：品牌蓝描边 + 左箭头图标，业务动作由外部 @click 透传 -->
  <el-button class="std-back-btn" v-bind="$attrs">
    <el-icon class="std-back-ico"><ArrowLeft/></el-icon>
    <slot>返回</slot>
  </el-button>
</template>
<script lang="ts" setup>
import { ArrowLeft } from '@element-plus/icons-vue'
defineOptions({ inheritAttrs: false })
</script>
<style scoped>
.std-back-btn {
  color: var(--brand, #1E6FA8);
  border-color: var(--brand, #1E6FA8);
  background: #fff;
}
.std-back-btn:hover,
.std-back-btn:focus {
  background: var(--brand-soft, #eaf3fa);
  border-color: var(--brand-dark, #155a8a);
  color: var(--brand-dark, #155a8a);
}
.std-back-ico { margin-right: 4px; }
</style>
