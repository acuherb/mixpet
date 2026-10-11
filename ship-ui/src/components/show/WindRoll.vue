<template>
  <section ref="myRoll">
    <!-- 背景装饰：模糊光斑 -->
    <div class="blob blob-1" ref="blob1"/>
    <div class="blob blob-2" ref="blob2"/>
    <div class="blob blob-3" ref="blob3"/>
    <!-- 装饰网格 -->
    <div class="grid"/>
    <!-- 主内容 -->
    <div class="hero-inner">
      <div class="hero-tag">pet-hms · 宠物医疗</div>
      <h1 class="hero-title">守护每一只毛孩子</h1>
      <p class="hero-subtitle">
        预约挂号 · 在线咨询 · 宠物档案 · 全程陪伴
      </p>
      <div class="hero-actions">
        <router-link class="btn btn-primary" to="/reception/doctorSee">
          查看医生
        </router-link>
        <router-link class="btn btn-ghost" to="/reception/chat">
          在线咨询
        </router-link>
      </div>
    </div>
  </section>
</template>

<script lang="ts" setup>
import {onBeforeUnmount, onMounted, ref} from "vue";

// 保留 isLogin prop 接口（父组件仍会传入）
defineProps<{
  readonly isLogin: boolean
}>()

const myRoll = ref<HTMLElement>()
const blob1 = ref<HTMLElement>()
const blob2 = ref<HTMLElement>()
const blob3 = ref<HTMLElement>()
let rollHeight = 900

const scrollBlobs = () => {
  const y = window.scrollY
  if (y > rollHeight) return
  if (blob1.value) blob1.value.style.transform = `translate(${y * 0.15}px, ${y * 0.2}px)`
  if (blob2.value) blob2.value.style.transform = `translate(${-y * 0.2}px, ${y * 0.15}px)`
  if (blob3.value) blob3.value.style.transform = `translate(${y * 0.1}px, ${-y * 0.1}px)`
}

onMounted(() => {
  if (myRoll.value?.offsetHeight !== undefined) {
    rollHeight = myRoll.value.offsetHeight
  }
  window.addEventListener('scroll', scrollBlobs, {passive: true})
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', scrollBlobs)
})
</script>

<style lang="scss" scoped>
section {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
  min-height: 520px;
  overflow: hidden;
  max-width: 100vw;
  background: linear-gradient(160deg, #E8F5EE 0%, #F5F7FA 55%, #EAF3FA 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 背景网格 */
.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(30, 111, 168, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(30, 111, 168, 0.05) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(circle at 50% 45%, #000 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(circle at 50% 45%, #000 30%, transparent 75%);
  pointer-events: none;
}

/* 模糊光斑 */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
  pointer-events: none;
  will-change: transform;
}

.blob-1 {
  width: 420px;
  height: 420px;
  background: #7BD1A8;
  top: -80px;
  left: -60px;
  animation: float 12s ease-in-out infinite;
}

.blob-2 {
  width: 380px;
  height: 380px;
  background: #A8D3EC;
  bottom: -100px;
  right: -80px;
  animation: float 15s ease-in-out infinite reverse;
}

.blob-3 {
  width: 260px;
  height: 260px;
  background: #FFD9A0;
  top: 40%;
  right: 25%;
  opacity: 0.35;
  animation: float 18s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0) }
  50% { transform: translateY(-24px) }
}

/* 内容 */
.hero-inner {
  position: relative;
  z-index: 10;
  text-align: center;
  padding: 0 24px;
  max-width: 720px;
}

.hero-tag {
  display: inline-block;
  padding: 6px 14px;
  background: rgba(30, 111, 168, 0.10);
  color: #1E6FA8;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
  margin-bottom: 20px;
}

.hero-title {
  margin: 0 0 16px;
  font-size: 48px;
  font-weight: 700;
  color: #1F2D3D;
  letter-spacing: 1px;
  line-height: 1.2;
}

.hero-subtitle {
  margin: 0 0 32px;
  font-size: 16px;
  color: #606266;
  letter-spacing: 0.5px;
}

.hero-actions {
  display: inline-flex;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: center;
}

.btn {
  display: inline-block;
  padding: 12px 28px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.5px;
  transition: all 0.2s;
  cursor: pointer;

  &.btn-primary {
    background: #1E6FA8;
    color: #fff;
    box-shadow: 0 6px 18px rgba(30, 111, 168, 0.28);

    &:hover {
      background: #2A82C2;
      box-shadow: 0 8px 22px rgba(30, 111, 168, 0.36);
      transform: translateY(-1px);
    }
  }

  &.btn-ghost {
    background: #fff;
    color: #1E6FA8;
    border: 1px solid #D8E3EE;

    &:hover {
      border-color: #1E6FA8;
      background: #F0F7FD;
    }
  }
}

/* 窄屏适配 */
@media (max-width: 768px) {
  .hero-title { font-size: 34px }
  .hero-subtitle { font-size: 14px }
  section { height: 70vh }
}
</style>
