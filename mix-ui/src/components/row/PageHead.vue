<template>
  <!-- 公共页头：统一返回按钮 + 标题 -->
  <header class="page-header">
    <BackBtn v-if="showBack" @click="$router.back()"/>
    <span class="ph-title"><slot></slot></span>
  </header>
</template>

<script lang="ts" setup>
withDefaults(defineProps<{ showBack?: boolean }>(), { showBack: true })
</script>

<style lang="scss" scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
  -webkit-user-select: none;
  user-select: none;

  .ph-title {
    font-size: 18px;
    font-weight: 700;
    color: #303133;
  }
}
</style>
