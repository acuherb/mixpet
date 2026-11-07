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
    }, {
        // 收费/发药：医生闭环的最后一环，不再跳到 /backstage 另一个壳
        path: "y-billList",
        component: () => import('@/views/backstage/y-billList.vue'),
    }, {
        // 处方笺档案（调档/补打/作废）
        path: "z-rxArchive",
        component: () => import('@/views/backstage/z-rxArchive.vue'),
    }, {
        // 检验/影像报告单打印
        path: "z-examReport",
        component: () => import('@/views/backstage/z-examReport.vue'),
    }, {
        // 检验/影像申请单打印
        path: "z-requisition",
        component: () => import('@/views/backstage/z-requisition.vue'),
    }, {
        // 临床配置：检验项 / 影像项字典、病历模板
        path: "z-labTest",
        component: () => import('@/views/backstage/z-labTest.vue'),
    }, {
        path: "z-imageTest",
        component: () => import('@/views/backstage/z-imageTest.vue'),
    }, {
        path: "z-template",
        component: () => import('@/views/backstage/z-template.vue'),
    }, {
        // 手术知情同意书 / 随访复诊提醒
        path: "z-consent",
        component: () => import('@/views/backstage/z-consent.vue'),
    }, {
        path: "z-remind",
        component: () => import('@/views/backstage/z-remind.vue'),
    }, {
        // 检验参考区间维护（按物种/性别/月龄分层 + 危急阈值）
        path: "z-refRange",
        component: () => import('@/views/backstage/z-refRange.vue'),
    }, {
        // 诊断编码字典（SOAP 的 A：结构化编码 + 体系）
        path: "z-diagnosis",
        component: () => import('@/views/backstage/z-diagnosis.vue'),
    },
]