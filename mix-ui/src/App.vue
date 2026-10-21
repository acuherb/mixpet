<template>
  <router-view v-slot="{ Component }">
    <transition name="fade-up">
      <component :is="Component"/>
    </transition>
  </router-view>
</template>
<script lang="ts" setup>
import {onMounted, watch} from 'vue'
import {hospital, loadHospital} from "@/utils/hospital";

// 医院信息：App 启动加载一次（页脚/title/品牌色注入用），标题随医院名联动
onMounted(() => {
  loadHospital()
  watch(hospital, h => {
    document.title = (h && h.name) ? `${h.name} · 宠物医院管理系统` : '宠物医院管理系统'
  }, {immediate: true})
})
</script>
<style lang="scss" scoped>
/* 路由切换过渡：短促淡入+上移（避免 3D 翻转在慢设备上白闪/抖动/横向滚动） */
.fade-up-enter-active {
  animation: fade-up-in 0.25s ease-out;
}

.fade-up-leave-active {
  animation: fade-up-out 0.15s ease-in;
}

@keyframes fade-up-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fade-up-out {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}
</style>
