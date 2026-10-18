<template>
  <article class="card">
    <div class="card-tit">单号：{{ a.appointmentId }}<span class="right">挂号人：{{ a.clientName }}</span></div>
    <ul class="appoint_con">
      <li>描述：<span class="info">{{ a.appointmentInfo }}</span></li>
      <li>就诊时间：<span class="date">{{ moveT(a.appointmentDate) }}</span></li>
      <li>挂号时间：<span class="date">{{ moveT(a.createTime || "") }}</span></li>
      <li>科室：
        <el-tag>{{ a.departmentName }}</el-tag>
      </li>
      <li>结果：
        <el-tag v-if="a.isSuc" type="success">已处理</el-tag>
        <el-tag v-else type="warning">未处理</el-tag>
        <el-tag v-if="a.isSuc && status === 0" size="small" style="margin-left:6px" type="warning">接诊中</el-tag>
        <el-tag v-else-if="a.isSuc && status === 2" size="small" style="margin-left:6px" type="danger">已作废</el-tag>
      </li>
    </ul>
    <div class="appoint-btn">
      <el-button v-if="!a.isSuc" type="success" @click="toSuc">接诊</el-button>
      <el-button v-else-if="status === 0" type="warning" @click="toSuc">继续开单</el-button>
      <el-button v-else type="info" plain @click="toSuc">查看</el-button>
      <!-- 爽约：预约时间已过且未处理的单才显示 -->
      <el-button v-if="!a.isSuc && isOverdue" size="small" plain type="danger" @click="toMiss">标记爽约</el-button>
    </div>
    <div class="card_content">
      <div class="card-tit">宠物：{{ a.petName }}</div>
      <div class="pet-con">
        <img :src="'/api/common/download?name='+a.petPhoto" alt="头像"/>
        <ul>
          <li>品种：
            <el-tag>{{ a.petVariety }}</el-tag>
          </li>
          <li>性别：
            <TagSex :sex="a.petSex"/>
          </li>
          <li>年龄：{{ getAge(a.petBirthday)}}</li>
          <li>健康状况：
            <el-tag>{{ a.petStatus }}</el-tag>
          </li>
        </ul>
      </div>
    </div>
  </article>
</template>

<script lang="ts" setup>
import TagSex from "@/components/show/TagSex.vue";
import {AppointDto2} from "@/model/DO/AppointDto2";
import {getAge, moveT} from "@/utils/TimeUtil";
import {computed, ref} from "vue";

const props = defineProps<{
  a: AppointDto2
  /** 病历状态：0接诊中 1已完结 2已作废（已处理挂号才有） */
  status?: number | null
}>();

const myBorColor = ref<string>("#E6A23C")
const myShaColor = ref<string>("#f3d19e")
if (props.a.isSuc) {
  myBorColor.value = "#67C23A"
  myShaColor.value = "#b3e19d"
}

const emit = defineEmits<{
  (e: "toSuc"): void
  (e: "toMiss"): void
}>()

const toSuc = () => {
  emit("toSuc")
}
const toMiss = () => {
  emit("toMiss")
}
/** 预约时间已过（可标记爽约） */
const isOverdue = computed(() => !!props.a.appointmentDate && new Date(props.a.appointmentDate).getTime() < Date.now())
</script>

<style lang="scss" scoped>
$bor-color: v-bind(myBorColor);
$sha-color: v-bind(myBorColor);
.card {
  position: relative;
  width: 370px;
  max-width: 90vw;
  height: 200px;
  overflow: hidden;
  background-color: #FAFAFA;
  padding: 12px;
  box-sizing: border-box;
  box-shadow: 5px 5px 10px 0 rgba(0, 0, 0, 0.3);
  border-radius: 20px;
  border: 2px solid $bor-color;
  -webkit-user-select: none;
  user-select: none;

  &:hover {
    box-shadow: 5px 5px 10px 0 $sha-color;
  }

  .card-tit {
    font-weight: bold;
    padding: 4px;
    border-bottom: #cccccc solid;

    .right {
      float: right;
    }
  }

  .appoint_con {
    li {
      font-size: 15px;
    }

    span.info {
      color: #67C23A;

      &:hover {
        color: #529b2e;
      }
    }

    span.date {
      color: #E6A23C;

      &:hover {
        color: #b88230;
      }
    }
  }

  .appoint-btn {
    position: absolute;
    right: 10px;
    top: 64px;

    button {
      margin-top: 2px;
    }
  }

  .card_content {
    position: absolute;
    left: 0;
    bottom: 0;
    width: 100%;
    height: 192px;
    box-sizing: border-box;
    padding: 8px;
    background: #FAFAFA;
    border: 2px solid $bor-color;
    border-top-left-radius: 24px;
    transform: translate(200px, 154px);
    transition: transform .5s;
    overflow: hidden;

    &:before {
      content: "";
      z-index: -1;
      position: absolute;
      top: -32px;
      left: -32px;
      width: 120px; /* 圆的直径 */
      height: 120px; /* 圆的直径 */
      background-color: $bor-color; /* 圆的背景色 */
      border-radius: 50%;
    }

    &:hover {
      transform: translate(4px, 28px);
    }

    .pet-con {
      margin-top: 4px;
      display: flex;
      font-size: small;

      img {
        width: 96px;
        height: 96px;
        border-radius: 32px;
        border: 3px solid;
        border-color: #f89898 #eebe77 #79bbff #95d475;
      }
    }
  }
}
</style>
