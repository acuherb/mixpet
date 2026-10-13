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
        // 套餐字典
        path: "z-package",
        component: () => import('@/views/backstage/z-package.vue')
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
        // 化验结果录入
        path: "z-labResult",
        component: () => import('@/views/backstage/z-labResult.vue')
    }, {
        // 住院管理
        path: "z-hospital",
        component: () => import('@/views/backstage/z-hospital.vue')
    }, {
        // 影像项字典
        path: "z-imageTest",
        component: () => import('@/views/backstage/z-imageTest.vue')
    }, {
        // 影像报告录入
        path: "z-imageResult",
        component: () => import('@/views/backstage/z-imageResult.vue')
    }, {
        // 免疫档案
        path: "z-immune",
        component: () => import('@/views/backstage/z-immune.vue')
    }
]