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
        <!-- 管理员：二级菜单 -->
        <el-sub-menu v-if="isAdmin" index="" :title="isCollapse ? '个人中心' : ''">
          <template #title>
            <el-tooltip content="个人中心" placement="right" :disabled="!isCollapse">
              <el-icon>
                <icon-menu/>
              </el-icon>
            </el-tooltip>
            <span>个人中心</span>
          </template>
          <el-menu-item-group :title="doctorNow.name">
            <el-menu-item index="/backstage">个人资料</el-menu-item>
            <el-menu-item index="/backstage/x-departmentList">科室列表</el-menu-item>
            <el-menu-item index="/backstage/z-job">职务/职称</el-menu-item>
            <el-menu-item index="/backstage/x-doctorList">医生列表</el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>

        <!-- 医生：一级菜单 -->
        <el-menu-item v-else index="/backstage" :title="isCollapse ? '个人中心' : ''">
          <el-tooltip content="个人中心" placement="right" :disabled="!isCollapse">
            <el-icon>
              <icon-menu/>
            </el-icon>
          </el-tooltip>
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-dashboard" :title="isCollapse ? '经营看板' : ''">
          <el-icon>
              <DataAnalysis/>
            </el-icon>
          <span>经营看板</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-remind" :title="isCollapse ? '随访提醒' : ''">
          <el-icon>
              <Bell/>
            </el-icon>
          <span>随访提醒</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-follow" :title="isCollapse ? '随访记录' : ''">
          <el-icon>
              <Phone/>
            </el-icon>
          <span>随访记录</span>
        </el-menu-item>
        <el-menu-item index="/backstage/y-clientList" :title="isCollapse ? '用户列表' : ''">
          <el-icon>
              <User/>
            </el-icon>
          <span>用户列表</span>
        </el-menu-item>
        <el-menu-item index="/backstage/y-petList" :title="isCollapse ? '宠物列表' : ''">
          <el-icon>
              <Football/>
            </el-icon>
          <span>宠物列表</span>
        </el-menu-item>
        <el-menu-item index="/backstage/y-appointmentList" :title="isCollapse ? '挂号列表' : ''">
          <el-icon>
              <Tickets/>
            </el-icon>
          <span>挂号列表</span>
        </el-menu-item>
        <el-menu-item index="/backstage/y-drugList" :title="isCollapse ? '药品列表' : ''">
          <el-icon>
              <FirstAidKit/>
            </el-icon>
          <span>药品列表</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-package" :title="isCollapse ? '套餐管理' : ''">
          <el-icon>
              <Goods/>
            </el-icon>
          <span>套餐管理</span>
        </el-menu-item>
        <el-menu-item index="/backstage/y-billList" :title="isCollapse ? '收费列表' : ''">
          <el-icon>
              <Money/>
            </el-icon>
          <span>收费列表</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-labTest" :title="isCollapse ? '检验项字典' : ''">
          <el-icon>
              <FirstAidKit/>
            </el-icon>
          <span>检验项字典</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-labResult" :title="isCollapse ? '化验结果' : ''">
          <el-icon>
              <DataLine/>
            </el-icon>
          <span>化验结果录入</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-hospital" :title="isCollapse ? '住院管理' : ''">
          <el-icon>
              <OfficeBuilding/>
            </el-icon>
          <span>住院管理</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-surgery" :title="isCollapse ? '手术麻醉' : ''">
          <el-icon>
              <FirstAidKit/>
            </el-icon>
          <span>手术麻醉</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-imageTest" :title="isCollapse ? '影像项字典' : ''">
          <el-icon>
              <Camera/>
            </el-icon>
          <span>影像项字典</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-imageResult" :title="isCollapse ? '影像报告' : ''">
          <el-icon>
              <Picture/>
            </el-icon>
          <span>影像报告录入</span>
        </el-menu-item>
        <el-menu-item index="/backstage/z-immune" :title="isCollapse ? '免疫档案' : ''">
          <el-icon>
              <CirclePlusFilled/>
            </el-icon>
          <span>免疫档案</span>
        </el-menu-item>
        <el-menu-item index="/backstage/y-adoptList" :title="isCollapse ? '领养列表' : ''">
          <el-icon>
              <School/>
            </el-icon>
          <span>领养列表</span>
        </el-menu-item>
        <el-menu-item index="/backstage/y-fosterList" :title="isCollapse ? '寄养列表' : ''">
          <el-icon>
              <OfficeBuilding/>
            </el-icon>
          <span>寄养列表</span>
        </el-menu-item>
        <el-menu-item index="/backstage/y-noticeList" :title="isCollapse ? '公告列表' : ''">
          <el-icon>
              <DataBoard/>
            </el-icon>
          <span>公告列表</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <main>
      <router-view></router-view>
    </main>
  </div>
</template>

<script lang="ts" setup>
import {Camera, CirclePlusFilled, DataAnalysis, DataBoard, DataLine, Expand, FirstAidKit, Fold, Football, Menu as IconMenu, Money, OfficeBuilding, Picture, School, Tickets, User, Bell, Goods,} from '@element-plus/icons-vue'
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