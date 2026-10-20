import {RouteRecordRaw} from "vue-router";

/**
 * 后台页面的路由
 * 父路径：/backstage
 */
export const backstage: RouteRecordRaw[] = [
    {
        // 管理员个人中心
        path: "",
        component: () => import('@/views/backstage/w-doctorOne.vue')
    }, {
        // 经营看板
        path: "z-dashboard",
        component: () => import('@/views/backstage/z-dashboard.vue')
    }, {
        // 收费表
        path: "y-billList",
        component: () => import('@/views/backstage/y-billList.vue')
    }, {
        // 批量入库（Excel 粘贴导入）
        path: "y-drugInbound",
        component: () => import('@/views/backstage/y-drugInbound.vue')
    }, {
        // 医院资料（单行配置：名称/地址/电话/logo）
        path: "y-hospitalInfo",
        component: () => import('@/views/backstage/y-hospitalInfo.vue')
    }, {
        // 病例中心（病历检索 + 全量档案）
        path: "z-recordList",
        component: () => import('@/views/backstage/z-recordList.vue')
    }, {
        // 处方笺档案（734号：调档/补打/作废）
        path: "z-rxArchive",
        component: () => import('@/views/backstage/z-rxArchive.vue')
    }, {
        // 检验/影像报告单打印
        path: "z-examReport",
        component: () => import('@/views/backstage/z-examReport.vue')
    }, {
        // 科室表
        path: "x-departmentList",
        component: () => import('@/views/backstage/x-departmentList.vue')
    }, {
        // 职务/职称字典
        path: "z-job",
        component: () => import('@/views/backstage/z-job.vue')
    }, {
        // 检验/影像申请单（国标，可打印）
        path: "z-requisition",
        component: () => import('@/views/backstage/z-requisition.vue')
    }, {
        // 随访/复诊提醒
        path: "z-remind",
        component: () => import('@/views/backstage/z-remind.vue')
    }, {
        // 随访记录管理
        path: "z-follow",
        component: () => import('@/views/backstage/z-follow.vue')
    }, {
        // 系统设置/字典（集中管理各模块枚举）
        path: "z-setting",
        component: () => import('@/views/backstage/z-setting.vue')
    }, {
        // 医生排班
        path: "z-schedule",
        component: () => import('@/views/backstage/z-schedule.vue')
    }, {
        // 供应商管理
        path: "z-supplier",
        component: () => import('@/views/backstage/z-supplier.vue')
    }, {
        // 经营财务报表
        path: "z-report",
        component: () => import('@/views/backstage/z-report.vue')
    }, {
        // 会员等级/会员卡模板
        path: "z-memberLevel",
        component: () => import('@/views/backstage/z-memberLevel.vue')
    }, {
        // 次卡模板
        path: "z-countCard",
        component: () => import('@/views/backstage/z-countCard.vue')
    }, {
        // 套餐字典
        path: "z-package",
        component: () => import('@/views/backstage/z-package.vue')
    }, {
        // 手术/麻醉管理
        path: "z-surgery",
        component: () => import('@/views/backstage/z-surgery.vue')
    }, {
        // 电子病历模板
        path: "z-template",
        component: () => import('@/views/backstage/z-template.vue')
    }, {
        // 手术/麻醉知情同意书（可打印）
        path: "z-consent",
        component: () => import('@/views/backstage/z-consent.vue')
    },{
        // 领养表
        path: "y-adoptList",
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
        component: () => import('@/views/backstage/y-drugList.vue')
    }, {
        // 医生表
        path: "x-doctorList",
        component: () => import('@/views/backstage/x-doctorList.vue')
    }, {
        // 寄养表
        path: "y-fosterList",
        component: () => import('@/views/backstage/y-fosterList.vue')
    }, {
        // 公告表
        path: "y-noticeList",
        component: () => import('@/views/backstage/y-noticeList.vue')
    }, {
        // 宠物表
        path: "y-petList",
        component: () => import('@/views/backstage/y-petList.vue')
    }, {
        // 检验项字典
        path: "z-labTest",
        component: () => import('@/views/backstage/z-labTest.vue')
    }, {
        // 医技结果录入：统一入口（化验+影像）
        path: "z-examResult",
        component: () => import('@/views/backstage/z-examResult.vue')
    }, {
        // 化验结果录入（兼容旧路径，重定向到统一入口）
        path: "z-labResult",
        redirect: "/backstage/z-examResult?tab=lab"
    }, {
        // 住院管理
        path: "z-hospital",
        component: () => import('@/views/backstage/z-hospital.vue')
    }, {
        // 影像项字典
        path: "z-imageTest",
        component: () => import('@/views/backstage/z-imageTest.vue')
    }, {
        // 影像报告录入（兼容旧路径，重定向到统一入口）
        path: "z-imageResult",
        redirect: "/backstage/z-examResult?tab=image"
    }, {
        // 免疫档案
        path: "z-immune",
        component: () => import('@/views/backstage/z-immune.vue')
    }, {
        // 聊天数据清理
        path: "z-chatClean",
        component: () => import('@/views/backstage/z-chatClean.vue')
    }
]