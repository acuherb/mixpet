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
    },
]