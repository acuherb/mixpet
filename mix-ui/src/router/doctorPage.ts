import {RouteRecordRaw} from "vue-router";

/**
 * 用户页面的路由
 * 父路径：/doctorPage
 */
export const doctorPage: RouteRecordRaw[] = [
    {
        // 诊疗流中枢（脊柱）：预约→接诊→开单→结果→结算→随访 一条可视化流水线，实时联动、自动流转
        // 父路由：深页（接诊/结果/收银/随访）作为子路由在流内抽屉就地处理，不跳页
        path: "d-flow",
        component: () => import('@/views/doctorPage/d-flow.vue'),
        children: [
            {path: 'appoint', component: () => import('@/views/doctorPage/d-appoint.vue')},
            {path: 'exam', component: () => import('@/views/backstage/z-examResult.vue')},
            {path: 'pay', component: () => import('@/views/backstage/y-billList.vue')},
            {path: 'follow', component: () => import('@/views/backstage/z-follow.vue')},
            // 通用内联：深页内部 router.push 到其它业务页时，由全局守卫重写到这里，不脱离抽屉
            {path: 'inline', component: () => import('@/views/doctorPage/FlowInline.vue')},
        ],
    }, {
        // 医生操作台（登录首屏）：面向人的三栏工作台——今日待办 / 当前患者 / 常用功能宫格。
        // 与 d-flow 对称，业务页在 inline 子路由的抽屉里就地处理，不跳页。
        path: "d-console",
        component: () => import('@/views/doctorPage/d-console.vue'),
        children: [
            {path: 'inline', component: () => import('@/views/doctorPage/FlowInline.vue')},
        ],
    }, {
        // 默认进入医生操作台（给人类用的可点击界面），全景看板从顶部一键切换
        path: "",
        redirect: 'd-console',
    }, {
        // 旧「经典工作台」已下线；旧书签统一落到医生操作台
        path: "d-home",
        redirect: 'd-console',
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