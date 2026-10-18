<template>
  <section ref="myRoll">
    <!-- 背景装饰：模糊光斑（颜色随主题色） -->
    <div class="blob blob-1" ref="blob1"/>
    <div class="blob blob-2" ref="blob2"/>
    <!-- 顶部过渡雾：把顶栏深色柔和延伸下来 -->
    <div class="roll-fade"/>
    <!-- 主内容 -->
    <div class="hero-inner">
      <div class="hero-tag">mixpet · 宠物医疗</div>
      <h1 class="hero-title">守护每一只毛孩子</h1>
      <p class="hero-subtitle">
        预约挂号 · 在线咨询 · 宠物档案 · 全程陪伴
      </p>
    </div>
    <!-- 向下滚动提示 -->
    <div class="scroll-cue" @click="scrollDown">
      <span class="cue-line"/>
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
let rollHeight = 900

const scrollBlobs = () => {
  const y = window.scrollY
  if (y > rollHeight) return
  if (blob1.value) blob1.value.style.transform = `translate(${y * 0.15}px, ${y * 0.2}px)`
  if (blob2.value) blob2.value.style.transform = `translate(${-y * 0.2}px, ${y * 0.15}px)`
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

// 点击滚动提示，平滑滚到公告区
const scrollDown = () => {
  window.scrollTo({top: window.innerHeight - 60, behavior: 'smooth'})
}
</script>

<style lang="scss" scoped>
section {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
  min-height: 520px;
  overflow: hidden;
  max-width: 100vw;
  /* 背景随主题色联动：品牌淡调 → 页面背景 → 白，与顶栏/内容区同一色调线 */
  background: linear-gradient(160deg, var(--brand-soft) 0%, var(--bg) 55%, #ffffff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 顶部过渡雾：把顶栏的品牌淡色轻柔延至横幅 */
.roll-fade {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 72px;
  background: linear-gradient(to bottom, var(--brand-soft) 0%, transparent 100%);
  opacity: 0.55;
  pointer-events: none;
}

/* 模糊光斑（主题色系，低透明度保持柔和） */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.22;
  pointer-events: none;
  will-change: transform;
}

.blob-1 {
  width: 420px;
  height: 420px;
  background: var(--brand);
  top: -80px;
  left: -60px;
  animation: float 12s ease-in-out infinite;
}

.blob-2 {
  width: 380px;
  height: 380px;
  background: var(--brand-2);
  bottom: -100px;
  right: -80px;
  animation: float 15s ease-in-out infinite reverse;
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
  background: rgba(255, 255, 255, 0.72);
  color: var(--brand-dark);
  border: 1px solid rgba(255, 255, 255, 0.9);
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
  margin: 0;
  font-size: 16px;
  color: #606266;
  letter-spacing: 0.5px;
}

/* 向下滚动提示：一条细线缓慢下探 */
.scroll-cue {
  position: absolute;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  width: 26px;
  height: 44px;
  display: flex;
  justify-content: center;
  cursor: pointer;
  z-index: 10;

  .cue-line {
    width: 1.5px;
    height: 100%;
    background: linear-gradient(180deg, transparent, #1E6FA8 40%, transparent);
    animation: cue-drop 2s ease-in-out infinite;
  }

  &:hover .cue-line {
    background: linear-gradient(180deg, transparent, #2A82C2 40%, transparent);
  }
}

@keyframes cue-drop {
  0%, 100% { transform: translateY(-6px); opacity: 0.4 }
  50% { transform: translateY(6px); opacity: 1 }
}

/* 窄屏适配 */
@media (max-width: 768px) {
  .hero-title { font-size: 34px }
  .hero-subtitle { font-size: 14px }
  section { height: 70vh }
}
</style>