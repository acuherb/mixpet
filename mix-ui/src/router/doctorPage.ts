import {RouteRecordRaw} from "vue-router";

/**
 * 用户页面的路由
 * 父路径：/doctorPage
 */
export const doctorPage: RouteRecordRaw[] = [
    {
        // 网站主页
        path: "",
        component: () => import('@/views/doctorPage/index.vue'),
    }, {
        // 医生的挂号
        path: "d-appoint",
        component: () => import('@/views/doctorPage/d-appoint.vue'),
    }, {
        // 病例中心（病历检索 + 全量档案）
        path: "d-record",
        component: () => import('@/views/doctorPage/d-record.vue'),
    }, {
        // 医生的聊天
        path: "d-chat",
        component: () => import('@/views/doctorPage/d-chat.vue'),
    }, {
        // 医生开化验单
        path: "d-lab",
        component: () => import('@/views/doctorPage/d-lab.vue'),
    }, {
        // 医生开影像检查单
        path: "d-image",
        component: () => import('@/views/doctorPage/d-image.vue'),
    }, {
        // 兽医处方笺（国标，可打印）
        path: "d-rx",
        component: () => import('@/views/doctorPage/d-rx.vue'),
    }, {
        // 病历打印（国标病历 / 彩色详情版）
        path: "d-recordPrint",
        component: () => import('@/views/doctorPage/d-recordPrint.vue'),
    }, {
        // 医技结果录入：统一入口，内部分检验/影像两个页签
        path: "z-examResult",
        component: () => import('@/views/backstage/z-examResult.vue'),
    }, {
        // 保留原路径兼容（重定向到统一入口）
        path: "z-labResult",
        redirect: "/doctorPage/z-examResult?tab=lab"
    }, {
        // 保留原路径兼容（重定向到统一入口）
        path: "z-imageResult",
        redirect: "/doctorPage/z-examResult?tab=image"
    }, {
        // 挂号列表（前台同款表格视图，医生查登记明细直达，无需进后台）
        path: "y-appointmentList",
        component: () => import('@/views/backstage/y-appointmentList.vue'),
    }, {
        // 手术麻醉（手术记录）
        path: "z-surgery",
        component: () => import('@/views/backstage/z-surgery.vue'),
    }, {
        // 住院管理（入院登记）
        path: "z-hospital",
        component: () => import('@/views/backstage/z-hospital.vue'),
    }, {
        // 疫苗驱虫（免疫接种）
        path: "z-immune",
        component: () => import('@/views/backstage/z-immune.vue'),
    }, {
        // 随访记录
        path: "z-follow",
        component: () => import('@/views/backstage/z-follow.vue'),
    },
]