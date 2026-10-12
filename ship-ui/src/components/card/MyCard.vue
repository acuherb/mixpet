<template>
  <div class="img-card" :class="type === 'success' ? 'is-success' : 'is-primary'">
    <div class="card-avatar">
      <img :src="'/api/common/download?name='+data.photo" alt="头像">
    </div>
    <div class="card-main">
      <div class="card-title">{{ data.tit }}</div>
      <div class="card-meta">
        <span>{{ data.age }}</span>
        <span class="dot">·</span>
        <span>{{ data.sex }}</span>
        <span class="dot">·</span>
        <span>{{ data.job }}</span>
      </div>
      <div v-if="data.info2" class="card-line card-line--mute">{{ data.info2 }}</div>
      <div class="card-line card-line--intro">{{ data.info1 }}</div>
      <button type="button" class="card-action" @click="$router.push(data.link)">查看详情</button>
    </div>
  </div>
</template>

<script lang="ts" setup>
defineProps<{
  type: "primary" | "success"
  data: CardData
}>()

export interface CardData {
  photo: string;
  tit: string;
  age: string;
  sex: string;
  job: string;
  info1: string;
  info2: string;
  link: string;
}
</script>

<style lang="scss" scoped>
.img-card {
  --accent: #1E6FA8;

  width: 350px;
  max-width: 92vw;
  background: #fff;
  border: 1px solid #E4E7ED;
  border-radius: 12px;
  padding: 18px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 18px;
  box-shadow: 0 1px 2px rgba(31, 45, 61, 0.04);
  transition: box-shadow .2s, border-color .2s, transform .2s;
  user-select: none;

  &.is-success {
    --accent: #0F8A7E;
  }

  &:hover {
    border-color: var(--accent);
    box-shadow: 0 6px 16px rgba(31, 45, 61, 0.10);
    transform: translateY(-1px);
  }

  .card-avatar {
    flex-shrink: 0;
    width: 104px;
    height: 104px;
    border-radius: 12px;
    overflow: hidden;
    background: #F5F7FA;
    border: 1px solid #E4E7ED;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
  }

  .card-main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .card-title {
    font-size: 18px;
    font-weight: 600;
    color: #1F2D3D;
    line-height: 1.3;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-meta {
    font-size: 14px;
    color: #909399;
    display: flex;
    align-items: center;
    gap: 6px;

    .dot {
      color: #C0C4CC;
    }
  }

  .card-line {
    font-size: 14px;
    color: #606266;

    &--intro {
      line-height: 1.55;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 3;
      line-clamp: 3;
      overflow: hidden;
      word-break: break-word;
      white-space: pre-wrap;
    }

    &--mute {
      color: #909399;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .card-action {
    margin-top: 6px;
    align-self: flex-start;
    padding: 6px 18px;
    font-size: 14px;
    color: var(--accent);
    background: #fff;
    border: 1px solid var(--accent);
    border-radius: 6px;
    cursor: pointer;
    transition: background .2s, color .2s;

    &:hover {
      background: var(--accent);
      color: #fff;
    }
  }
}
</style>