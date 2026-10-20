<template>
  <div>
    <PageHead>病例中心</PageHead>
    <RecordCenter :default-mine="false"/>
  </div>
</template>

<script lang="ts" setup>
import PageHead from "@/components/row/PageHead.vue";
import RecordCenter from "@/components/record/RecordCenter.vue";
</script>