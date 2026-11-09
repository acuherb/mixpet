<template>
  <div class="backstage">
    <aside :class="{ collapsed: isCollapse }">
      <div :title="isCollapse ? '展开侧栏' : '收起侧栏'" class="collapse-btn" @click="toggleCollapse">
        <el-icon>
          <component :is="isCollapse ? Expand : Fold"/>
        </el-icon>
      </div>

      <!-- 用户卡：头像/姓名/权限 → 个人中心；三端互切固定在这一块，不再是几个手写裸链接 -->
      <router-link class="side-user" title="个人中心" to="/backstage">
        <img :src="'/api/common/download?name='+doctorNow.photo" alt="头像" class="my-avatar"/>
        <span v-if="!isCollapse" class="su-text">
          <b>{{ doctorNow.name || '我的工作台' }}</b>
          <small>{{ roleLabel }}</small>
        </span>
      </router-link>
      <div v-if="!isCollapse" class="side-switch">
        <router-link to="/doctorPage/d-console"><el-icon><IconMenu/></el-icon>医生操作台</router-link>
        <router-link to="/doctorPage/d-flow"><el-icon><DataBoard/></el-icon>诊疗流全景</router-link>
      </div>
      <div v-if="!isCollapse" class="menu-search">
        <el-input v-model="menuKw" clearable placeholder="搜索菜单" size="small" :prefix-icon="Search"/>
      </div>
      <div v-if="!isCollapse && noMatch" class="menu-empty">没有匹配的菜单</div>

      <el-menu
          :key="menuKey"
          :accordion="!searching"
          :collapse="isCollapse"
          :collapse-transition="false"
          :default-active="searching ? '' : $route.path"
          :default-openeds="openGroups"
          active-text-color="#ffd04b"
          background-color="#545c64"
          router
          text-color="#dce3ec"
      >
        <!-- ═══ 经营决策（管理者登录首屏） ═══ -->
        <!-- 分组用 v-show 隐藏而非 v-if 移除：搜索时不会反复挂载/卸载 sub-menu -->
        <el-sub-menu v-if="isManager" v-show="groupMatch('stat')" index="g-stat">
          <template #title><el-icon><DataBoard/></el-icon><span>经营决策</span></template>
          <el-menu-item v-show="menuMatch('经营驾驶舱')" index="/backstage/m-flow">经营驾驶舱</el-menu-item>
          <el-menu-item v-show="menuMatch('经营看板')" index="/backstage/z-dashboard">经营看板</el-menu-item>
          <el-menu-item v-show="menuMatch('财务报表')" index="/backstage/z-report">财务报表</el-menu-item>
        </el-sub-menu>

        <!-- ═══ 业务查询 / 对账（次要入口，日常诊疗一律在医生操作台） ═══
             与医生端重复的作业页不再向管理者展示，旧 URL 全部保留可直达。 -->
        <el-sub-menu v-show="groupMatch('query')" index="g-query">
          <template #title><el-icon><Tickets/></el-icon><span>业务查询 / 对账</span></template>
          <el-menu-item v-if="doctorNow.authLv >= 2 && !isManager" v-show="menuMatch('今日接待')" index="/backstage/r-flow">今日接待</el-menu-item>
          <el-menu-item v-if="!isManager" v-show="menuMatch('挂号列表')" index="/backstage/y-appointmentList">挂号列表</el-menu-item>
          <el-menu-item v-if="!isManager" v-show="menuMatch('收银台')" index="/backstage/y-billList">收银台</el-menu-item>
          <el-menu-item v-if="!isManager" v-show="menuMatch('处方笺档案')" index="/backstage/z-rxArchive">处方笺档案</el-menu-item>
          <el-menu-item v-show="menuMatch('客户管理')" index="/backstage/y-clientList">客户管理</el-menu-item>
          <el-menu-item v-show="menuMatch('宠物管理')" index="/backstage/y-petList">宠物管理</el-menu-item>
          <el-menu-item v-show="menuMatch('寄养管理')" index="/backstage/y-fosterList">寄养管理</el-menu-item>
          <el-menu-item v-if="isManager" v-show="menuMatch('活体领养')" index="/backstage/y-adoptList">活体领养</el-menu-item>
        </el-sub-menu>

        <!-- ═══ 经营配置（改一次、用很久的门面与定价） ═══ -->
        <el-sub-menu v-if="isManager" v-show="groupMatch('biz')" index="g-biz">
          <template #title><el-icon><Setting/></el-icon><span>经营配置</span></template>
          <el-menu-item v-show="menuMatch('药品管理')" index="/backstage/y-drugList">药品管理</el-menu-item>
          <el-menu-item v-show="menuMatch('服务套餐')" index="/backstage/z-package">服务套餐</el-menu-item>
          <el-menu-item v-show="menuMatch('医院资料')" index="/backstage/y-hospitalInfo">医院资料</el-menu-item>
          <el-menu-item v-show="menuMatch('供应商管理')" index="/backstage/z-supplier">供应商管理</el-menu-item>
          <el-menu-item v-show="menuMatch('会员等级')" index="/backstage/z-memberLevel">会员等级</el-menu-item>
          <el-menu-item v-show="menuMatch('次卡模板')" index="/backstage/z-countCard">次卡模板</el-menu-item>
        </el-sub-menu>

        <!-- ═══ 临床字典 ═══ -->
        <el-sub-menu v-if="isManager" v-show="groupMatch('clinic')" index="g-clinic">
          <template #title><el-icon><Document/></el-icon><span>临床字典</span></template>
          <el-menu-item v-show="menuMatch('检验项字典')" index="/backstage/z-labTest">检验项字典</el-menu-item>
          <el-menu-item v-show="menuMatch('影像项字典')" index="/backstage/z-imageTest">影像项字典</el-menu-item>
          <el-menu-item v-show="menuMatch('病历模板')" index="/backstage/z-template">病历模板</el-menu-item>
        </el-sub-menu>

        <!-- ═══ 组织与人员 ═══ -->
        <el-sub-menu v-if="isManager" v-show="groupMatch('org')" index="g-org">
          <template #title><el-icon><OfficeBuilding/></el-icon><span>组织与人员</span></template>
          <el-menu-item v-show="menuMatch('医生列表')" index="/backstage/x-doctorList">医生列表</el-menu-item>
          <el-menu-item v-show="menuMatch('医生排班')" index="/backstage/z-schedule">医生排班</el-menu-item>
          <el-menu-item v-show="menuMatch('科室列表')" index="/backstage/x-departmentList">科室列表</el-menu-item>
          <el-menu-item v-show="menuMatch('职务/职称')" index="/backstage/z-job">职务/职称</el-menu-item>
        </el-sub-menu>

        <!-- ═══ 系统与安全 ═══ -->
        <el-sub-menu v-if="isManager" v-show="groupMatch('sys')" index="g-sys">
          <template #title><el-icon><Lock/></el-icon><span>系统与安全</span></template>
          <el-menu-item v-show="menuMatch('系统设置')" index="/backstage/z-setting">系统设置</el-menu-item>
          <el-menu-item v-show="menuMatch('操作日志')" index="/backstage/z-auditLog">操作日志</el-menu-item>
          <el-menu-item v-show="menuMatch('公告管理')" index="/backstage/y-noticeList">公告管理</el-menu-item>
          <el-menu-item v-show="menuMatch('聊天数据清理')" index="/backstage/z-chatClean">聊天数据清理</el-menu-item>
          <!-- 数据备份/还原：仅超管（后端 /backup/restore 也限 ADMIN） -->
          <el-menu-item v-if="isAdmin" v-show="menuMatch('数据备份')" index="/backstage/z-backup">数据备份</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>
    <main>
      <router-view></router-view>
    </main>
  </div>
</template>

<script lang="ts" setup>
import {DataBoard, Document, Lock, OfficeBuilding, Setting, Search, Tickets, Expand, Fold, Menu as IconMenu} from '@element-plus/icons-vue'
import {computed, onMounted, onUnmounted, reactive, ref} from "vue";
import {reqGetLoginD} from "@/request/DoctorApi";

const doctorNow = reactive({
  name: "",
  photo: "zs.jpg",
  authLv: 0
})

// 是否管理者（MANAGER=6 / ADMIN=8）：经营决策与四个管理组对其可见；
// 业务查询/对账组里与医生操作台重复的作业入口对其隐藏（临床日常不在这里）。
const isManager = computed(() => doctorNow.authLv >= 6)
// 是否超管（ADMIN=8）：数据备份/还原等毁库操作仅其可见可为
const isAdmin = computed(() => doctorNow.authLv >= 8)
const roleLabel = computed(() => {
  const lv = doctorNow.authLv
  return lv >= 8 ? '超级管理员' : lv >= 6 ? '管理员' : lv >= 4 ? '医生' : lv >= 2 ? '医护' : '未分配权限'
})

// 侧边栏折叠
const isCollapse = ref(localStorage.getItem('asideCollapse') === '1')
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
  localStorage.setItem('asideCollapse', isCollapse.value ? '1' : '0')
}
// 响应式：窄屏自动折叠
const checkWidth = () => {
  if (window.innerWidth < 992) isCollapse.value = true
}

// 菜单搜索：按标签过滤；整组无命中就隐藏组标题（不留空组），命中组自动展开。
// 组标签只服务于搜索联动，路由与权限仍以模板里的 index / v-if 为准。
type GroupKey = 'stat' | 'query' | 'biz' | 'clinic' | 'org' | 'sys'
const managerGroups: GroupKey[] = ['stat', 'biz', 'clinic', 'org', 'sys']
const queryStaffOnly = ['今日接待', '挂号列表', '收银台', '处方笺档案']
const queryCommon = ['客户管理', '宠物管理', '寄养管理']
const queryManagerOnly = ['活体领养']  // 领养整模块后端是 @Secured(MANAGER)，低档连列表都取不到
const groupLabels: Record<GroupKey, string[]> = {
  stat: ['经营驾驶舱', '经营看板', '财务报表'],
  query: [...queryStaffOnly, ...queryCommon, ...queryManagerOnly],
  biz: ['药品管理', '服务套餐', '医院资料', '供应商管理', '会员等级', '次卡模板'],
  clinic: ['检验项字典', '影像项字典', '病历模板'],
  org: ['医生列表', '医生排班', '科室列表', '职务/职称'],
  sys: ['系统设置', '操作日志', '公告管理', '聊天数据清理', '数据备份'],
}
const menuKw = ref('')
const menuMatch = (label: string): boolean =>
    !menuKw.value.trim() || label.includes(menuKw.value.trim())
const groupMatch = (g: GroupKey): boolean => {
  // 管理者专属组对普通医生一律不命中，避免搜出他根本看不到的组
  if (managerGroups.includes(g) && !isManager.value) return false
  const kw = menuKw.value.trim()
  if (!kw) return true
  // 管理者看不到 query 组里那 4 个重复入口，搜索时也不该因它们命中而显示空组
  // 查询组：低档只看得到 queryCommon；管理者看得到档案项 + 领养，看不到那 4 个医生端重复入口
  const labels = g === 'query'
    ? (isManager.value ? [...queryCommon, ...queryManagerOnly] : queryCommon)
    : groupLabels[g]
  return labels.some(l => l.includes(kw))
}
const searching = computed(() => !!menuKw.value.trim())
const openGroups = computed<string[]>(() => {
  if (!searching.value) return []  // 非搜索模式：交给 ElMenu 按 active 项自动展开
  const keys = Object.keys(groupLabels) as GroupKey[]
  // default-openeds / open() 认的是 sub-menu 的 index，不是分组键，必须补 'g-' 前缀
  return keys.filter(groupMatch).map(k => 'g-' + k)
})
const noMatch = computed(() => searching.value && openGroups.value.length === 0)
// default-openeds 只在 ElMenu 初始化时生效，命中组变化时用 key 重建一次；
// 搜索时清掉 default-active 并关掉手风琴，否则 active 项所在组会抢走唯一展开名额。
// 不调用 ElMenu 内部 open/close（v-if 移除分组会让注册表缺项而抛 TypeError）。
const menuKey = computed(() => searching.value
    ? 'search:' + openGroups.value.join(',')
    : 'normal')

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
    width: 208px;
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

    .collapse-btn {
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      cursor: pointer;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      &:hover { background-color: rgba(255, 255, 255, 0.1); }
    }

    /* 用户卡：小头像 + 姓名 + 权限，取代原来的 72px 大头像和重复的「个人中心」菜单项 */
    .side-user {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      color: #fff;
      text-decoration: none;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);

      &:hover { background-color: rgba(255, 255, 255, 0.08); }

      img.my-avatar {
        width: 34px;
        height: 34px;
        flex-shrink: 0;
        border-radius: 50%;
        object-fit: cover;
        background: #6b7480;
      }

      .su-text {
        display: flex;
        flex-direction: column;
        min-width: 0;
        line-height: 1.35;

        b { font-size: 13px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        small { font-size: 11px; color: #aeb9c6; }
      }
    }

    /* 三端互切：统一样式，不再一个黄字一个灰字 */
    .side-switch {
      padding: 6px 8px 8px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);

      a {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 6px;
        font-size: 13px;
        color: #cbd8ea;
        text-decoration: none;
        border-radius: 4px;

        &:hover { color: #ffd04b; background-color: rgba(255, 255, 255, 0.08); }
      }
    }

    .menu-search {
      padding: 8px 10px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    }

    .menu-empty {
      padding: 16px 10px;
      font-size: 12px;
      color: #aeb9c6;
      text-align: center;
    }

    .el-menu {
      margin-top: 6px;
      border-right: none;
    }

    &.collapsed {
      width: 64px;

      .side-user { justify-content: center; padding: 10px 0; }
      img.my-avatar { width: 32px; height: 32px; }
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

/* 统一层级：一级组标题与二级菜单项高度/字号分开，子项明确缩进 */
aside :deep(.el-sub-menu__title),
aside :deep(.el-menu-item) { height: 38px; line-height: 38px; font-size: 13px; }
aside:not(.collapsed) :deep(.el-sub-menu .el-menu-item) { padding-left: 40px !important; font-size: 12.5px; }

/* 无奈之举，各个弹出框中的input太宽了 */
::deep(.el-form-item) {
  width: 50%;
  z-index: 0;
}

::deep(.el-table) {
  width: 100%;
}
</style>
