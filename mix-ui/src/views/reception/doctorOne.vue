<template>
  <div class="base-page">
    <!-- 导航栏-->
    <PageHead>医生详情</PageHead>
    <MyRow2>
      <img :src="'/api/common/download?name='+doctorDetail.doctorPhoto" alt="头像" class="my-avatar"/>
      <ul>
        <li><span style="font-weight: bolder">{{ doctorDetail.doctorName }}</span></li>
        <li class="doc-motto">{{ doctorDetail.doctorMotto ? "座右铭："+doctorDetail.doctorMotto : "" }}</li>
        <li>
          <router-link :to="'/reception/chat/'+doctorDetail.doctorId">
            <el-button type="primary">咨询</el-button>
          </router-link>
        </li>
      </ul>
      <ul class="hide-narrow">
        <li>{{ getAge(doctorDetail.doctorAge)}}&nbsp;&nbsp;&nbsp;{{ doctorDetail.doctorGender ? "男" : "女" }}</li>
        <li>{{ doctorDetail.jobName || '宠物医生' }}&nbsp;&nbsp;&nbsp;{{ departmentNames.join("、") }}</li>
        <li>{{ doctorDetail.doctorTel }}</li>
        <li class="doc-intro">{{ doctorDetail.doctorInfo }}</li>
      </ul>
      <template #right>
        <h3 class="appoint-title">近期挂号</h3>
        <table class="appoint">
          <thead>
          <tr>
            <th>挂号人</th>
            <th>宠物</th>
            <th>描述</th>
            <th>时间</th>
            <th>状态</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="a in appointPageList" :key="a.appointmentId">
            <td>{{ a.clientName }}</td>
            <td>{{ a.petName }}</td>
            <td class="appoint-desc">{{ a.appointmentInfo }}</td>
            <td>{{ moveT(a.appointmentDate) }}</td>
            <td>{{ appointState(a.isSuc) }}</td>
          </tr>
          </tbody>
        </table>
        <el-pagination
            v-if="appointDetail.length > appointPage.pageSize"
            small layout="prev, pager, next, total"
            :current-page="appointPage.current"
            :page-size="appointPage.pageSize"
            :total="appointDetail.length"
            @current-change="appointChange"/>
      </template>
    </MyRow2>
  </div>
</template>

<script lang="ts" setup>
import PageHead from "@/components/row/PageHead.vue";
import {getAge, moveT} from "@/utils/TimeUtil";
import {DoctorDto} from "@/model/DO/DoctorDto";
import {AppointDto} from "@/model/DO/AppointDto";
import {computed, onMounted, reactive, ref} from "vue";
import {reqDoctorOne} from "@/request/DoctorApi";
import {reqDoctorOneA} from "@/request/AppointApi";
import MyRow2 from "@/components/row/MyRow2.vue";
// 如此获取传参
const props = defineProps<{
  doctorId: string
}>()
const doctorDetail = ref<DoctorDto>({
  "doctorId": 0,
  "username": "",
  "doctorName": "",
  "doctorGender": true,
  "doctorAge": "",
  "doctorPhoto": "",
  "doctorTel": "",
  "doctorEmail": null,
  "authLv": 0,
  "doctorInfo": "",
  "departmentName": "",
  "departmentIds": [],
  "departmentNames": []
})
const appointDetail = ref<AppointDto[]>([]);
const departmentNames = computed(() => doctorDetail.value.departmentNames || [])
// 挂号分页
const appointPage = reactive({current: 1, pageSize: 5})
const appointPageList = computed(() =>
    appointDetail.value.slice((appointPage.current - 1) * appointPage.pageSize,
        appointPage.current * appointPage.pageSize)
)
const appointChange = (val: number): void => {
  appointPage.current = val
}
const appointState = (s?: boolean | null): string => s ? "已完成" : "待就诊"
onMounted(() => {
  reqDoctorOne(parseInt(props.doctorId)).then(res => {
    doctorDetail.value = res
  })
  reqDoctorOneA(parseInt(props.doctorId)).then(res => {
    appointDetail.value = res
  })
})
</script>

<style lang="scss" scoped>
/*响应式*/
@media (min-width: 768px) {
  .base-page {
    width: 92vw;
    left: 4vw;

    img.my-avatar {
      border-radius: 50%;
      margin-top: 10px;
      width: 128px;
      height: 128px;
    }
  }
}

@media (max-width: 768px) {
  .base-page {
    width: 96vw;
    left: 2vw;

    img.my-avatar {
      border-radius: 50%;
      width: 64px;
      height: 64px;
    }

    .hide-narrow {
      display: none;
    }
  }
}


/*设置大致布局*/
.base-page {
  position: absolute;
  top: 12vh;
  background-color: whitesmoke;
  box-shadow: 5px 5px 10px 0 rgba(0, 0, 0, 0.5);
  padding: 16px;
  border-radius: 12px;
  box-sizing: border-box;
  /*医生卡片的信息*/
  ul {
    list-style: none;
    margin-left: -24px;
    text-align: left;
  }

  li.doc-motto {
    font-size: 13px;
    color: #909399;
    max-width: 240px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  li.doc-intro {
    margin-top: 8px;
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.8;
  }
}

/* 挂号的表格 */
table.appoint {
  width: 100%;
  border-collapse: collapse;

  thead {
    background-color: lightblue;
  }

  th, td {
    border: 1px solid #e0e0e0;
    padding: 8px 6px;
  }

  td.appoint-desc {
    max-width: 240px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.appoint-title {
  margin: 0 0 8px;
  font-size: 16px;
  color: #1F2D3D;
  text-align: left;
}
</style>