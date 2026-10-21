<template>
  <div class="pager-wrap">
    <span class="total-info">共 <b>{{ total }}</b> 条</span>
    <el-pagination
        :current-page="pu.numPage"
        :page-size="pu.pageSize"
        :page-sizes="[10, 20, 30, 50, 100]"
        :total="total"
        background
        layout="sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
    />
  </div>
</template>

<script lang="ts" setup>
/*=====后台分页条，每页默认 30 条=====*/
import {reactive} from "vue";
import {PageQuery} from "@/model/VO/BackQuery";

defineProps<{
  total: number
}>()
const emit = defineEmits<{
  (e: "changePu", value: PageQuery): void,
}>()

const pu = reactive<PageQuery>({
  numPage: 1,
  pageSize: 30
})

const handleSizeChange = (val: number) => {
  pu.pageSize = val
  pu.numPage = 1
  emit("changePu", pu)
}
const handleCurrentChange = (val: number) => {
  pu.numPage = val
  emit("changePu", pu)
}
</script>

<style scoped>
.pager-wrap {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 4px;
  margin-bottom: 0;
  flex-shrink: 0;
}
.total-info {
  font-size: 15px;
  color: #303133;
}
.total-info b {
  font-size: 18px;
  color: #409eff;
  margin: 0 4px;
}
</style>
