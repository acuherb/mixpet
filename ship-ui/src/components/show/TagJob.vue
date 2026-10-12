<template>
  <el-tag v-if="authLv===8" type="danger">
    {{ getJob(authLv) }}
  </el-tag>
  <el-tag v-else-if="authLv===6" type="warning">
    {{ getJob(authLv) }}
  </el-tag>
  <el-tag v-else>
    {{ getJob(authLv) }}
  </el-tag>
</template>

<script lang="ts" setup>
/*=====医生职业的封装展示标签=====*/
import {getJob} from "@/utils/JobUtil";

defineProps<{
  authLv: number
}>()
</script>

<style scoped>

</style>