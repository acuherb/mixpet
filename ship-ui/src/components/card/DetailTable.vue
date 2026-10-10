<template>
  <img :src="'/api/common/download?name='+rolePhoto" alt="头像" class="my-avatar"/>
  <!-- 用户、管理员、医生的个人信息展示-->
  <table class="myTable">
    <tr v-for="t in tabCon">
      <td class="bold">{{ t.tit }}</td>
      <td>{{ t.con }}</td>
    </tr>
    <tr>
      <td class="bold">操作</td>
      <td>
        <div class="btn-group">
          <el-button type="primary" @click="updateRole">修改资料</el-button>
          <el-button type="warning" @click="updatePwd">重置密码</el-button>
        </div>
      </td>
    </tr>
    <tr>
      <td class="bold">其它操作</td>
      <td>
        <div class="btn-group">
          <el-button type="danger" @click="logout">退出登录</el-button>
        </div>
      </td>
    </tr>
  </table>
</template>

<script lang="ts" setup>
/*=====三个角色的个人信息展示页面=====*/
import {reqLogout} from "@/request/PowerApi";
import {useRouter} from "vue-router";

defineProps<{
  rolePhoto: string,
  tabCon: DetailTabType[]
}>()
const emit = defineEmits<{
  (e: "upRole"): void,
  (e: "upPwd"): void
}>()

interface DetailTabType {
  tit: string,
  con: any
}

const updateRole = () => {
  emit("upRole");
}
const updatePwd = () => {
  emit("upPwd");
}
// 退出登录
const router = useRouter()
const logout = (): void => {
  reqLogout().then(resp => {
    if (resp.code === 204) router.push("/")
  })
}
</script>

<style lang="scss" scoped>
img.my-avatar {
  border-radius: 50%;
  width: 128px;
  height: 128px;
  position: relative;
  left: 50%;
  margin-left: -64px;
}

/*设置信息展示表格*/
.myTable {
  border-collapse: collapse;
  text-align: center;
  position: relative;
  width: 60%;
  left: 20%;
  margin-bottom: 20px;

  td {
    border: none;
    padding: 4px 2px;
  }

  .bold {
    text-align: right;
    font-weight: bold;
    white-space: nowrap;   /* 关键：字段名不换行 */
    width: 1%;             /* 关键：内容自适应宽度，不被压扁 */
    padding-right: 12px;
  }

  .bold + td {
    text-align: left;
    padding-left: 50px;
    word-break: break-all; /* 关键：长内容自动换行 */
  }
}

/* 按钮组：自动换行，不挤压 */
.btn-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 窄屏适配：表格铺开，压缩字段间距 */
@media (max-width: 768px) {
  .myTable {
    width: 92%;
    left: 4%;
    font-size: 14px;

    .bold {
      padding-right: 8px;
      font-size: 13px;
    }

    .bold + td {
      padding-left: 12px;
    }
  }

  /* 窄屏时按钮整行铺开 */
  .btn-group {
    flex-direction: column;
    gap: 6px;

    .el-button {
      width: 100%;
      margin: 0;
    }
  }
}
</style>
