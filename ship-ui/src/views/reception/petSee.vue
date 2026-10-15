<template>
  <div class="my-space">
    <PageHead>查看全部宠物</PageHead>
    <PetCard :card-list="petCardTxt.records"/>
    <el-pagination :current-page="page.numPage" :page-size="page.pageSize" :total="petCardTxt.total"
                   background
                   hide-on-single-page
                   layout="total, prev, pager, next"
                   @current-change="changePage"
    />
  </div>
</template>

<script lang="ts" setup>
// 宠物的卡片信息
import {Page} from "@/model/DO/Page";
import {Pet} from "@/model/entiy/Pet";
import PetCard from "@/components/zoneView/PetCard.vue";
import {onMounted, onUnmounted, reactive} from "vue";
import PageHead from "@/components/row/PageHead.vue";
import {PageQuery} from "@/model/VO/BackQuery";
import {reqPetSee} from "@/request/PetApi";

// 根据屏幕宽度决定每页展示数量：手机一列竖向滚动，大屏自动多放几行
const decidePageSize = (): number => {
  const w = window.innerWidth
  if (w < 640) return 6        // 手机：一列，一页 6 条竖向滚动
  if (w < 1200) return 10      // 平板/普通屏：每行 2~3 个
  if (w < 1600) return 15      // 桌面：每行 4~5 个，一页 3 行
  return 20                    // 大屏：每行 5+ 个，一页 4 行
}

const page: PageQuery = reactive({
  numPage: 1,
  pageSize: decidePageSize()
})
const petCardTxt: Page<Pet> = reactive({records: [], total: 0})

const load = () => {
  reqPetSee(page).then(res => {
    petCardTxt.records = res.records
    petCardTxt.total = res.total
  })
}

const onResize = () => {
  page.pageSize = decidePageSize()
  page.numPage = 1
  load()
}

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', onResize)
})
// 分页条
const changePage = (val: number): void => {
  page.numPage = val
  load()
}
</script>

<style scoped src="public/css/my-space.css"/>