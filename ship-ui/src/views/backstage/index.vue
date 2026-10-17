<template>
  <div class="backstage">
    <aside :class="{ collapsed: isCollapse }">
      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon>
          <component :is="isCollapse ? Expand : Fold"/>
        </el-icon>
      </div>
      <router-link :to="$route.path==='/backstage'?'/doctorPage':'/backstage'">
        <img :src="'/api/common/download?name='+doctorNow.photo" alt="头像" class="my-avatar"/>
      </router-link>
      <el-menu
          :collapse="isCollapse"
          :collapse-transition="false"
          :default-active="$route.path"
          active-text-color="#ffd04b"
          background-color="#545c64"
          router
          text-color="#fff"
      >
        <!-- 个人中心 -->
        <el-menu-item index="/backstage" :title="isCollapse ? '个人中心' : ''">
          <el-icon><IconMenu/></el-icon>
          <span>个人中心</span>
        </el-menu-item>

        <!-- 数据中心 -->
        <el-sub-menu index="g-stat" :title="isCollapse ? '数据中心' : ''">
          <template #title>
            <el-icon><DataBoard/></el-icon>
            <span>数据中心</span>
          </template>
          <el-menu-item index="/backstage/z-dashboard">经营看板</el-menu-item>
          <el-menu-item index="/backstage/z-report">财务报表</el-menu-item>
        </el-sub-menu>

        <!-- 接待管理（前台作业线：挂号 → 收费 → 寄养接待） -->
        <el-sub-menu index="g-reception" :title="isCollapse ? '接待管理' : ''">
          <template #title>
            <el-icon><Tickets/></el-icon>
            <span>接待管理</span>
          </template>
          <el-menu-item index="/backstage/y-appointmentList">挂号列表</el-menu-item>
          <el-menu-item index="/backstage/y-billList">收银台</el-menu-item>
          <el-menu-item index="/backstage/y-fosterList">寄养管理</el-menu-item>
        </el-sub-menu>

        <!-- 诊疗管理 -->
        <el-sub-menu index="g-medical" :title="isCollapse ? '诊疗管理' : ''">
          <template #title>
            <el-icon><FirstAidKit/></el-icon>
            <span>诊疗管理</span>
          </template>
          <el-menu-item index="/backstage/z-labResult">化验结果录入</el-menu-item>
          <el-menu-item index="/backstage/z-imageResult">影像报告录入</el-menu-item>
          <el-menu-item index="/backstage/z-surgery">手术麻醉</el-menu-item>
          <el-menu-item index="/backstage/z-hospital">住院管理</el-menu-item>
          <el-menu-item index="/backstage/z-immune">疫苗驱虫</el-menu-item>
          <el-menu-item index="/backstage/z-remind">随访提醒</el-menu-item>
          <el-menu-item index="/backstage/z-follow">随访记录</el-menu-item>
        </el-sub-menu>

        <!-- 商品与库存 -->
        <el-sub-menu index="g-goods" :title="isCollapse ? '商品与库存' : ''">
          <template #title>
            <el-icon><Goods/></el-icon>
            <span>商品与库存</span>
          </template>
          <el-menu-item index="/backstage/y-drugList">药品管理</el-menu-item>
          <el-menu-item index="/backstage/y-drugInbound">批量入库</el-menu-item>
          <el-menu-item index="/backstage/z-package">服务套餐</el-menu-item>
          <el-menu-item index="/backstage/z-supplier">供应商管理</el-menu-item>
        </el-sub-menu>

        <!-- 客户运营（客户资产与会员体系） -->
        <el-sub-menu index="g-customer" :title="isCollapse ? '客户运营' : ''">
          <template #title>
            <el-icon><User/></el-icon>
            <span>客户运营</span>
          </template>
          <el-menu-item index="/backstage/y-clientList">客户管理</el-menu-item>
          <el-menu-item index="/backstage/y-petList">宠物管理</el-menu-item>
          <el-menu-item index="/backstage/y-adoptList">活体领养</el-menu-item>
          <el-menu-item index="/backstage/z-memberLevel">会员等级</el-menu-item>
          <el-menu-item index="/backstage/z-countCard">次卡模板</el-menu-item>
        </el-sub-menu>

        <!-- 系统管理（设置 + 各类字典/模板 + 公告） -->
        <el-sub-menu index="g-config" :title="isCollapse ? '系统管理' : ''">
          <template #title>
            <el-icon><Setting/></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/backstage/z-setting">系统设置</el-menu-item>
          <el-menu-item index="/backstage/y-hospitalInfo">医院资料</el-menu-item>
          <el-menu-item index="/backstage/z-labTest">检验项字典</el-menu-item>
          <el-menu-item index="/backstage/z-imageTest">影像项字典</el-menu-item>
          <el-menu-item index="/backstage/z-template">病历模板</el-menu-item>
          <el-menu-item index="/backstage/z-chatClean">聊天数据清理</el-menu-item>
          <el-menu-item index="/backstage/y-noticeList">公告管理</el-menu-item>
        </el-sub-menu>

        <!-- 组织管理（仅管理员：科室/职务/医生/排班） -->
        <el-sub-menu v-if="isAdmin" index="g-org" :title="isCollapse ? '组织管理' : ''">
          <template #title>
            <el-icon><IconMenu/></el-icon>
            <span>组织管理</span>
          </template>
          <el-menu-item-group :title="doctorNow.name">
            <el-menu-item index="/backstage/x-departmentList">科室列表</el-menu-item>
            <el-menu-item index="/backstage/z-job">职务/职称</el-menu-item>
            <el-menu-item index="/backstage/x-doctorList">医生列表</el-menu-item>
            <el-menu-item index="/backstage/z-schedule">医生排班</el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>
      </el-menu>
    </aside>
    <main>
      <router-view></router-view>
    </main>
  </div>
</template>

<script lang="ts" setup>
import {DataBoard, Tickets, Goods, FirstAidKit, User, Setting, Expand, Fold, Menu as IconMenu} from '@element-plus/icons-vue'
import {computed, onMounted, onUnmounted, reactive, ref} from "vue";
import {reqGetLoginD} from "@/request/DoctorApi";

const doctorNow = reactive({
  name: "",
  photo: "zs.jpg",
  authLv: 0
})

// 是否管理员（院长 authLv=8）
const isAdmin = computed(() => doctorNow.authLv === 8)

// 侧边栏折叠
const isCollapse = ref(localStorage.getItem('asideCollapse') === '1')
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
  localStorage.setItem('asideCollapse', isCollapse.value ? '1' : '0')
}
// 响应式：窄屏自动折叠
const checkWidth = () => {
  if (window.innerWidth < 992) {
    isCollapse.value = true
  }
}
onMounted(() => {
  checkWidth()
  window.addEventListener('resize', checkWidth)
})
onUnmounted(() => {
  window.removeEventListener('resize', checkWidth)
})
reqGetLoginD().then(res => {
  doctorNow.photo = res.doctorPhoto
  doctorNow.name = res.doctorName
  doctorNow.authLv = res.authLv
})
</script>

<style lang="scss" scoped>
.backstage {
  height: 100vh;
  display: flex;
  overflow: hidden;
  /*侧边栏*/
  aside {
    width: 180px;
    flex-shrink: 0;
    height: 100%;
    background-color: #545c64;
    box-sizing: border-box;
    padding: 4px;
    overflow-y: auto;
    overflow-x: hidden;
    transition: width 0.25s;
    -webkit-user-select: none;
    user-select: none;

    /* 折叠按钮 */
    .collapse-btn {
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      cursor: pointer;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      &:hover {
        background-color: rgba(255, 255, 255, 0.1);
      }
    }

    /*头像*/
    img.my-avatar {
      border-radius: 50%;
      position: relative;
      height: 72px;
      width: 72px;
      top: 20px;
      left: 50%;
      margin-left: -36px;
      transition: all 0.25s;
    }

    /*导航菜单*/
    .el-menu {
      position: relative;
      top: 16px;
      border-right: none;
    }

    /* 折叠状态 */
    &.collapsed {
      width: 64px;

      img.my-avatar {
        height: 40px;
        width: 40px;
        margin-left: -20px;
        top: 12px;
      }

      .el-menu {
        top: 12px;
      }
    }
  }

  main {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    box-sizing: border-box;
    padding: 12px;
    background-color: #F0EAD6;
    z-index: 0;
  }
}

/* 无奈之举，各个弹出框中的input太宽了 */
:deep(.el-form-item) {
  width: 50%;
  z-index: 0;
}

:deep(.el-table) {
  width: 100%;
}
</style>