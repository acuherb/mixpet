<template>
  <router-view v-slot="{ Component }">
    <transition name="flip">
      <component :is="Component"/>
    </transition>
  </router-view>
</template>
<script lang="ts" setup>
import {onMounted, watch} from 'vue'
import {hospital, loadHospital} from "@/utils/hospital";

console.log(import.meta.env.VITE_BACK_URL)

// 医院信息：App 启动加载一次（页脚/title 用），标题随医院名联动
onMounted(() => {
  loadHospital()
  watch(hospital, h => {
    document.title = (h && h.name) ? `${h.name} · 宠物医院管理系统` : '宠物医院管理系统'
  }, {immediate: true})
})
</script>
<style lang="scss" scoped>
.flip-enter-active {
  animation: flip-in 0.5s;
}

.flip-leave-active {
  animation: flip-out 0.5s;
}

@keyframes flip-in {
  from {
    transform: rotateY(90deg);
  }
  to {
    transform: rotateY(0deg);
  }
}

@keyframes flip-out {
  from {
    transform: rotateY(0deg);
  }
  to {
    transform: rotateY(90deg);
  }
}
</style>
