<template>
  <div class="my-row">
    <slot></slot>
  </div>
</template>

<script lang="ts" setup>
</script>

<style scoped>
.img-row.my-row,
.my-row {
  --card-wid: 350px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(var(--card-wid), 1fr));
  justify-items: center;
  grid-gap: 10px;
}
</style>