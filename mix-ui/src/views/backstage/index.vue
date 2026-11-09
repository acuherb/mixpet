<template>
  <div class="backstage">
    <aside :class="{ collapsed: isCollapse }">
      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon>
          <component :is="isCollapse ? Expand : Fold"/>
        </el-icon>
      </div>
      <!-- 菜单搜索（30+菜单快速定位） -->
      <div v-if="!isCollapse" class="menu-search">
        <el-input v-model="menuKw" clearable placeholder="搜索菜单" size="small" :prefix-icon="Search"/>
      </div>
      <router-link to="/doctorPage/d-console" title="医生操作台（日常干活）" style="display:flex;align-items:center;gap:8px;padding:10px 16px;color:#ffd04b;text-decoration:none;font-size:14px;white-space:nowrap">
        <el-icon><IconMenu/></el-icon>
        <span v-if="!isCollapse">医生操作台</span>
      </router-link>
      <router-link to="/doctorPage/d-flow" title="诊疗流全景（看进度）" style="display:flex;align-items:center;gap:8px;padding:10px 16px;color:#cbd8ea;text-decoration:none;font-size:13px;white-space:nowrap">
        <el-icon><DataBoard/></el-icon>
        <span v-if="!isCollapse">诊疗流全景</span>
      </router-link>
      <router-link to="/backstage" title="个人中心">
        <img :src="'/api/common/download?name='+doctorNow.photo" alt="头像" class="my-avatar"/>
      </router-link>
      <el-menu
          :collapse="isCollapse"
          :collapse-transition="false"
          :default-active="$route.path"
          :accordion="true"
          active-text-color="#ffd04b"
          background-color="#545c64"
          router
          text-color="#fff"
      >
        <!-- 个人中心 -->
        <el-menu-item v-show="menuMatch('个人中心')" index="/backstage" :title="isCollapse ? '个人中心' : ''">
          <el-icon><IconMenu/></el-icon>
          <span>个人中心</span>
        </el-menu-item>

        <!-- ═══ 经营决策（管理者登录首屏） ═══ -->
        <el-sub-menu v-if="isManager" index="g-stat">
          <template #title>
            <el-icon><DataBoard/></el-icon>
            <span>经营决策</span>
          </template>
          <el-menu-item v-show="menuMatch('经营驾驶舱')" index="/backstage/m-flow">经营驾驶舱 · 实时指标</el-menu-item>
          <el-menu-item v-show="menuMatch('经营看板')" index="/backstage/z-dashboard">经营看板</el-menu-item>
          <el-menu-item v-show="menuMatch('财务报表')" index="/backstage/z-report">财务报表</el-menu-item>
        </el-sub-menu>

        <!-- ═══ 管理设置（全院配置 + 组织人员，只在后台做） ═══ -->
        <el-sub-menu v-if="isManager" index="g-config">
          <template #title>
            <el-icon><Setting/></el-icon>
            <span>管理设置</span>
          </template>
          <el-menu-item-group title="全院配置 · 定价字典">
            <el-menu-item v-show="menuMatch('医院资料')" index="/backstage/y-hospitalInfo">医院资料</el-menu-item>
            <el-menu-item v-show="menuMatch('药品管理')" index="/backstage/y-drugList">药品管理</el-menu-item>
            <el-menu-item v-show="menuMatch('服务套餐')" index="/backstage/z-package">服务套餐</el-menu-item>
            <el-menu-item v-show="menuMatch('供应商管理')" index="/backstage/z-supplier">供应商管理</el-menu-item>
            <el-menu-item v-show="menuMatch('会员等级')" index="/backstage/z-memberLevel">会员等级</el-menu-item>
            <el-menu-item v-show="menuMatch('次卡模板')" index="/backstage/z-countCard">次卡模板</el-menu-item>
            <el-menu-item v-show="menuMatch('检验项字典')" index="/backstage/z-labTest">检验项字典</el-menu-item>
            <el-menu-item v-show="menuMatch('影像项字典')" index="/backstage/z-imageTest">影像项字典</el-menu-item>
            <el-menu-item v-show="menuMatch('病历模板')" index="/backstage/z-template">病历模板</el-menu-item>
            <el-menu-item v-show="menuMatch('系统设置')" index="/backstage/z-setting">系统设置</el-menu-item>
            <el-menu-item v-show="menuMatch('公告管理')" index="/backstage/y-noticeList">公告管理</el-menu-item>
            <el-menu-item v-show="menuMatch('聊天数据清理')" index="/backstage/z-chatClean">聊天数据清理</el-menu-item>
            <!-- 数据备份/还原：仅超管（毁库操作，后端 /backup/restore 也限 ADMIN） -->
            <el-menu-item v-if="isAdmin" v-show="menuMatch('数据备份')" index="/backstage/z-backup">数据备份</el-menu-item>
            <!-- 操作日志：高危写操作留痕（管理者以上可查） -->
            <el-menu-item v-show="menuMatch('操作日志')" index="/backstage/z-auditLog">操作日志</el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="组织与人员">
            <el-menu-item v-show="menuMatch('科室列表')" index="/backstage/x-departmentList">科室列表</el-menu-item>
            <el-menu-item v-show="menuMatch('职务/职称')" index="/backstage/z-job">职务/职称</el-menu-item>
            <el-menu-item v-show="menuMatch('医生列表')" index="/backstage/x-doctorList">医生列表</el-menu-item>
            <el-menu-item v-show="menuMatch('医生排班')" index="/backstage/z-schedule">医生排班</el-menu-item>
          </el-menu-item-group>
        </el-sub-menu>

        <!-- ═══ 业务查询 / 对账（次要入口）：日常诊疗一律在医生操作台 ═══
             与医生操作台重复的作业页不再向管理者展示，但旧 URL 全部保留可直达；
             普通医生/医护仍从后台做档案查询与对账。 -->
        <el-sub-menu index="g-query">
          <template #title>
            <el-icon><Tickets/></el-icon>
            <span>业务查询 / 对账</span>
          </template>
          <el-menu-item v-if="doctorNow.authLv >= 2 && !isManager" v-show="menuMatch('今日接待')" index="/backstage/r-flow">今日接待 · 到店/待收银/已诊</el-menu-item>
          <el-menu-item v-if="!isManager" v-show="menuMatch('挂号列表')" index="/backstage/y-appointmentList">挂号列表</el-menu-item>
          <el-menu-item v-if="!isManager" v-show="menuMatch('收银台')" index="/backstage/y-billList">收银台</el-menu-item>
          <el-menu-item v-if="!isManager" v-show="menuMatch('处方笺档案')" index="/backstage/z-rxArchive">处方笺档案</el-menu-item>
          <el-menu-item v-show="menuMatch('寄养管理')" index="/backstage/y-fosterList">寄养管理</el-menu-item>
          <el-menu-item v-show="menuMatch('客户管理')" index="/backstage/y-clientList">客户管理</el-menu-item>
          <el-menu-item v-show="menuMatch('宠物管理')" index="/backstage/y-petList">宠物管理</el-menu-item>
          <el-menu-item v-show="menuMatch('活体领养')" index="/backstage/y-adoptList">活体领养</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>
    <main>
      <router-view></router-view>
    </main>
  </div>
</template>

<script lang="ts" setup>
import {DataBoard, Tickets, Setting, Search, Expand, Fold, Menu as IconMenu} from '@element-plus/icons-vue'
import {computed, onMounted, onUnmounted, reactive, ref} from "vue";
import {reqGetLoginD} from "@/request/DoctorApi";

const doctorNow = reactive({
  name: "",
  photo: "zs.jpg",
  authLv: 0
})

// 是否管理者（MANAGER=6 / ADMIN=8）：经营决策与管理设置组对其可见；
// 业务查询/对账组里与医生操作台重复的作业入口对其隐藏（临床日常不在这里）。
const isManager = computed(() => doctorNow.authLv >= 6)
// 是否超管（ADMIN=8）：数据备份/还原等毁库操作仅其可见可为
const isAdmin = computed(() => doctorNow.authLv >= 8)

// 侧边栏折叠
const isCollapse = ref(localStorage.getItem('asideCollapse') === '1')
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
  localStorage.setItem('asideCollapse', isCollapse.value ? '1' : '0')
}
// 菜单搜索：按标签文本过滤（空关键词显示全部）
const menuKw = ref('')
const menuMatch = (label: string): boolean =>
    !menuKw.value.trim() || label.includes(menuKw.value.trim())
// 响应式：窄屏自动折叠
const checkWidth = () => {
  if (window.innerWidth < 992) {
    isCollapse.value = true
  }
}
// 个人中心与侧边栏各持一份登录医生信息：资料保存后由 w-doctorOne 广播，这里同步刷新，
// 避免用整页 reload 来同步头像/姓名/权限（权限变化还要即刻反映到菜单可见性）。
const loadDoctorNow = (): void => {
  reqGetLoginD().then(res => {
    if (!res) return
    doctorNow.photo = res.doctorPhoto || doctorNow.photo
    doctorNow.name = res.doctorName || doctorNow.name
    doctorNow.authLv = Number(res.authLv ?? 0)
  })
}
onMounted(() => {
  checkWidth()
  window.addEventListener('resize', checkWidth)
  window.addEventListener('mixpet:profile-updated', loadDoctorNow)
})
onUnmounted(() => {
  window.removeEventListener('resize', checkWidth)
  window.removeEventListener('mixpet:profile-updated', loadDoctorNow)
})
loadDoctorNow()
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

    /* 菜单搜索框 */
    .menu-search {
      padding: 8px 10px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
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