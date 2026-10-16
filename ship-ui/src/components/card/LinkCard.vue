<template>
  <div :class="['link-card', `is-${type}`]" @click="onCardClick">
    <span v-if="badge && badge > 0" class="card-badge">{{ badge > 99 ? '99+' : badge }}</span>
    <div class="card-text">
      <div class="card-tit">{{ tit }}</div>
      <p class="card-body">{{ body }}</p>
    </div>
    <span class="card-arrow">→</span>
  </div>
</template>

<script lang="ts" setup>
import {useRouter} from "vue-router";

const router = useRouter()
const props = defineProps<{
  tit: string;
  body: string;
  type: "primary" | "success" | "warning";
  routerPath: string;
  badge?: number;
  needLogin?: boolean;
  guest?: boolean;
}>()
const emit = defineEmits<{
  (e: 'needLogin'): void
}>()
// 需登录且当前为游客时，交由父组件引导登录；否则正常跳转
const onCardClick = (): void => {
  if (props.needLogin && props.guest) emit('needLogin')
  else router.push(props.routerPath)
}
</script>

<style lang="scss" scoped>
.link-card {
  --accent: #1E6FA8;

  position: relative;
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

  .card-badge {
    position: absolute;
    top: 10px;
    right: 12px;
    min-width: 20px;
    height: 20px;
    padding: 0 6px;
    border-radius: 10px;
    background: #F56C6C;
    color: #fff;
    font-size: 12px;
    line-height: 20px;
    text-align: center;
    box-shadow: 0 2px 6px rgba(245, 108, 108, .4);
  }
}
</style>
