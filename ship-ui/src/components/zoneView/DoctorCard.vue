<template>
  <div class="doctor-card-row">
    <MyCard
        v-for="doctor in cardList"
        :key="doctor.doctorId"
        :data='{
        photo: doctor.doctorPhoto,
        tit: doctor.doctorName,
        age: getAge(doctor.doctorAge),
        sex: doctor.doctorGender ? "男" : "女",
        job: doctor.jobName || "宠物医生",
        info1: doctor.doctorInfo,
        info2: doctor.doctorMotto ? "座右铭："+doctor.doctorMotto : "",
        link:"/reception/doctorOne/"+doctor.doctorId
      }' :type="'primary'"/>
  </div>
</template>

<script lang="ts" setup>
/*=====前台的医生卡片批量展示=====*/
import MyCard from "@/components/card/MyCard.vue";
import {getAge} from "@/utils/TimeUtil";
import {DoctorDto} from "@/model/DO/DoctorDto";
// 接收参数：医生卡片
defineProps<{
  readonly cardList: DoctorDto[]
}>()
</script>

<style scoped>
/* 自适应网格：大屏自动多放，窄屏自动换行，和宠物列表一致 */
.doctor-card-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  justify-items: center;
  align-items: stretch;
  grid-gap: 16px;
}

/* 卡片占满所在列，避免被内容撑宽 */
.doctor-card-row :deep(.img-card) {
  --card-w: 100%;
  width: var(--card-w);
  max-width: 100%;
  align-items: stretch;
  min-height: 168px;
}

/* 卡片主体：垂直均匀分布，保证同排等高 */
.doctor-card-row :deep(.img-card .card-main) {
  justify-content: space-between;
}

/* 简介固定最多 3 行并占固定高度，避免 admin 长简介撑高卡片挤压邻卡 */
.doctor-card-row :deep(.img-card .card-line--intro) {
  height: 3.1em;
  min-height: 3.1em;
  -webkit-line-clamp: 3;
  line-clamp: 3;
}

.doctor-card-row :deep(.img-card .card-line--mute) {
  min-height: 1.5em;
}
</style>