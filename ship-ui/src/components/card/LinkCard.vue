<template>
  <div :class="['link-card', `is-${type}`]" @click="$router.push(routerPath)">
    <div class="card-text">
      <div class="card-tit">{{ tit }}</div>
      <p class="card-body">{{ body }}</p>
    </div>
    <span class="card-arrow">→</span>
  </div>
</template>

<script lang="ts" setup>
defineProps<{
  tit: string;
  body: string;
  type: "primary" | "success" | "warning";
  routerPath: string;
}>()
</script>

<style lang="scss" scoped>
.link-card {
  --accent: #1E6FA8;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  box-sizing: border-box;
  width: 100%;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #E4E7ED;
  border-left: 3px solid var(--accent);
  border-radius: 10px;
  cursor: pointer;
  user-select: none;
  transition: border-color .2s, box-shadow .2s, transform .2s;

  &.is-success {
    --accent: #0F8A7E;
  }

  &.is-warning {
    --accent: #C8862B;
  }

  &:hover {
    box-shadow: 0 6px 16px rgba(31, 45, 61, 0.10);
    transform: translateY(-2px);

    .card-arrow {
      color: var(--accent);
      transform: translateX(3px);
    }
  }

  .card-tit {
    font-size: 15px;
    font-weight: 600;
    color: #1F2D3D;
    margin-bottom: 4px;
  }

  .card-body {
    margin: 0;
    font-size: 13px;
    color: #909399;
  }

  .card-arrow {
    font-size: 16px;
    color: #C0C4CC;
    transition: color .2s, transform .2s;
    flex-shrink: 0;
  }
}
</style>
