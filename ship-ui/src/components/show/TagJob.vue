<template>
  <el-tag v-if="authLv===8" type="danger">
    {{ permName(authLv) }}
  </el-tag>
  <el-tag v-else-if="authLv===6" type="warning">
    {{ permName(authLv) }}
  </el-tag>
  <el-tag v-else>
    {{ permName(authLv) }}
  </el-tag>
</template>

<script lang="ts" setup>
/*=====医生权限等级标签（按 auth_lv 显示权限名，与职称解耦）=====*/
import {permName} from "@/utils/PermissionUtil";

defineProps<{
  authLv: number
}>()
</script>

<style scoped>

</style>
