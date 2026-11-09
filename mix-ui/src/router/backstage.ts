import {RouteRecordRaw} from "vue-router";

/**
 * 后台页面的路由
 * 父路径：/backstage
 * 三端职责：后台只做「医院级管理设置 / 经营决策」，临床日常在 /doctorPage；
 * 业务查档与对账保留旧路由兼容，但不再是后台主菜单的一级入口。
 * meta.minAuthLv 与侧栏 isManager(>=6) / isAdmin(>=8) 及后端 @Secured 对齐：
 *   6 = MANAGER/ADMIN，8 = 仅 ADMIN（数据备份），2 = NURSE 及以上；
 *   未标 meta 的业务页保持开放，写操作仍由后端 @Secured 真拦截。
 */
export const backstage: RouteRecordRaw[] = [
    {
        // 个人中心：所有员工改自己的资料，不设权限门槛
        path: "",
        component: () => import('@/views/backstage/w-doctorOne.vue')
    }, {
        // 经营看板（管理者）
        path: "z-dashboard",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-dashboard.vue')
    }, {
        // 前台接待流（复用诊疗流脊柱：今日预约→待收银→已诊），护士及以上
        path: "r-flow",
        meta: {minAuthLv: 2},
        component: () => import('@/views/backstage/r-flow.vue')
    }, {
        // 管理驾驶舱（复用诊疗流脊柱：营收/挂号/待结算/住院/药品预警），管理层及以上
        path: "m-flow",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/m-flow.vue')
    }, {
        // 收费表（业务查档/对账，不设门槛）
        path: "y-billList",
        component: () => import('@/views/backstage/y-billList.vue')
    }, {
        // 收费凭证打印页（统一报告外壳，独立窗口，数据一次性传递）
        path: "y-billPrint",
        component: () => import('@/views/backstage/y-billPrint.vue')
    }, {
        // 批量入库（Excel 粘贴导入）
        path: "y-drugInbound",
        component: () => import('@/views/backstage/y-drugInbound.vue')
    }, {
        // 医院资料（单行配置：名称/地址/电话/logo）
        path: "y-hospitalInfo",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/y-hospitalInfo.vue')
    }, {
        // 病例中心（病历检索 + 全量档案）
        path: "z-recordList",
        redirect: "/doctorPage/d-record"
    }, {
        // 处方笺档案（调档/补打/作废）
        path: "z-rxArchive",
        redirect: "/doctorPage/z-rxArchive"
    }, {
        // 检验/影像报告单打印
        path: "z-examReport",
        redirect: "/doctorPage/z-examReport"
    }, {
        // 科室表
        path: "x-departmentList",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/x-departmentList.vue')
    }, {
        // 职务/职称字典
        path: "z-job",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-job.vue')
    }, {
        // 检验/影像申请单（可打印）
        path: "z-requisition",
        redirect: "/doctorPage/z-requisition"
    }, {
        // 随访/复诊提醒
        path: "z-remind",
        redirect: "/doctorPage/z-remind"
    }, {
        // 随访记录管理
        path: "z-follow",
        redirect: "/doctorPage/z-follow"
    }, {
        // 系统设置/字典（集中管理各模块枚举）
        path: "z-setting",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-setting.vue')
    }, {
        // 医生排班
        path: "z-schedule",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-schedule.vue')
    }, {
        // 供应商管理
        path: "z-supplier",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-supplier.vue')
    }, {
        // 经营财务报表
        path: "z-report",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-report.vue')
    }, {
        // 会员等级/会员卡模板
        path: "z-memberLevel",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-memberLevel.vue')
    }, {
        // 次卡模板
        path: "z-countCard",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-countCard.vue')
    }, {
        // 套餐字典
        path: "z-package",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-package.vue')
    }, {
        // 手术/麻醉管理
        path: "z-surgery",
        redirect: "/doctorPage/z-surgery"
    }, {
        // 电子病历模板
        path: "z-template",
        redirect: "/doctorPage/z-template"
    }, {
        // 手术/麻醉知情同意书（可打印）
        path: "z-consent",
        redirect: "/doctorPage/z-consent"
    },{
        // 领养表（整模块后端为 MANAGER，低档不给入口）
        path: "y-adoptList",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/y-adoptList.vue')
    }, {
        // 挂号表
        path: "y-appointmentList",
        component: () => import('@/views/backstage/y-appointmentList.vue')
    }, {
        // 用户表
        path: "y-clientList",
        component: () => import('@/views/backstage/y-clientList.vue')
    }, {
        // 药品表
        path: "y-drugList",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/y-drugList.vue')
    }, {
        // 医生表
        path: "x-doctorList",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/x-doctorList.vue')
    }, {
        // 寄养表
        path: "y-fosterList",
        component: () => import('@/views/backstage/y-fosterList.vue')
    }, {
        // 公告表
        path: "y-noticeList",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/y-noticeList.vue')
    }, {
        // 宠物表
        path: "y-petList",
        component: () => import('@/views/backstage/y-petList.vue')
    }, {
        // 检验项字典
        path: "z-labTest",
        redirect: "/doctorPage/z-labTest"
    }, {
        // 医技结果录入：统一入口（化验+影像）
        path: "z-examResult",
        redirect: "/doctorPage/z-examResult"
    }, {
        // 化验结果录入（兼容旧路径，重定向到统一入口）
        path: "z-labResult",
        redirect: "/doctorPage/z-examResult?tab=lab"
    }, {
        // 住院管理
        path: "z-hospital",
        redirect: "/doctorPage/z-hospital"
    }, {
        // 影像项字典
        path: "z-imageTest",
        redirect: "/doctorPage/z-imageTest"
    }, {
        // 影像报告录入（兼容旧路径，重定向到统一入口）
        path: "z-imageResult",
        redirect: "/doctorPage/z-examResult?tab=image"
    }, {
        // 免疫档案
        path: "z-immune",
        redirect: "/doctorPage/z-immune"
    }, {
        // 聊天数据清理
        path: "z-chatClean",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-chatClean.vue')
    }, {
        // 数据备份/还原（仅超管 ADMIN 可见可操作）
        path: "z-backup",
        meta: {minAuthLv: 8},
        component: () => import('@/views/backstage/z-backup.vue')
    }, {
        // 操作日志（高危写操作留痕，管理者以上可查）
        path: "z-auditLog",
        meta: {minAuthLv: 6},
        component: () => import('@/views/backstage/z-auditLog.vue')
    }
]
