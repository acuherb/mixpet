<template>
  <div class="my-space">
    <PageHead>查看全部医生</PageHead>
    <DoctorCard :card-list="doctorCardTxt.records"/>
    <el-pagination
        :current-page="page.numPage"
        :page-size="page.pageSize"
        :total="doctorCardTxt.total"
        hide-on-single-page
        layout="total, prev, pager, next"
        @current-change="changePage"/>
  </div>
</template>

<script lang="ts" setup>
import {Page} from "@/model/DO/Page";
import DoctorCard from "@/components/zoneView/DoctorCard.vue";
import {onMounted, onUnmounted, reactive} from "vue";
import PageHead from "@/components/row/PageHead.vue";
import {DoctorDto} from "@/model/DO/DoctorDto";
import {PageQuery} from "@/model/VO/BackQuery";
import {reqDoctorSee} from "@/request/DoctorApi";

// 根据屏幕宽度决定每页展示数量：手机一列竖向滚动，大屏自动多放几行
const decidePageSize = (): number => {
  const w = window.innerWidth
  if (w < 640) return 6        // 手机
  if (w < 1200) return 9       // 平板/普通屏：每行 2~3 个
  if (w < 1600) return 12      // 桌面：每行 3~4 个
  return 15                    // 大屏
}

const page: PageQuery = reactive({
  numPage: 1,
  pageSize: decidePageSize()
})
const doctorCardTxt: Page<DoctorDto> = reactive({records: [], total: 0})

const load = () => {
  reqDoctorSee(page).then(res => {
    doctorCardTxt.records = res.records
    doctorCardTxt.total = res.total
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