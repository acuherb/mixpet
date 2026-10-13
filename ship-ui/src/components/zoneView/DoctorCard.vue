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
.doctor-card-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(440px, 1fr));
  justify-items: center;
  grid-gap: 16px;
}
</style>