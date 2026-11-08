import {createRouter, createWebHashHistory, RouteRecordRaw} from "vue-router"
import {reception} from "@/router/reception";
import {backstage} from "@/router/backstage";
import {doctorPage} from "@/router/doctorPage";
import {reqGetLogin, reqLoginUser} from "@/request/PowerApi";
import {reqGetLoginD} from "@/request/DoctorApi";
import {RoleEnum} from "@/model/VO/LoginVo";
import {ElMessage} from "element-plus";

const routes: RouteRecordRaw[] = [
    {
        // 网站主页（未登录也可浏览：公告/医生/领养/寄养）
        path: '/',
        component: () => import('@/views/reception/Index.vue')
    }, {
        path: '/login',
        component: () => import('@/views/login.vue')
    }, {
        path: '/findPwd',
        component: () => import('@/views/findPwd.vue')
    }, {
        path: '/register',
        component: () => import('@/views/register.vue')
    }, {
        path: '/reception/noticeList',
        component: () => import('@/views/reception/noticeList.vue')
    }, {
        path: '/changePwd',
        component: () => import('@/views/changePwd.vue')
    }, {
        path: '/reception',
        children: [...reception],
    }, {
        path: '/backstage',
        component: () => import('@/views/backstage/index.vue'),
        children: [...backstage],
    }, {
        path: '/doctorPage',
        children: [...doctorPage],
    }, {
        path: '/:pathMatch(.*)',
        name: 'not-found',
        component: () => import('@/views/page404.vue')
    }
]
export const router = createRouter({
    history: createWebHashHistory(),
    routes: routes
})

/** 首次改密检查标记：只有检查真正通过后才写入，避免一次失败就永久跳过 */
const PWD_CHECK_KEY = "mixpet_pwd_check_done"

router.beforeEach(async (to, from) => {
    const path = to.path

    // 诊疗流内联：深页在抽屉里 router.push 到其它医生端业务页时，重写为 d-flow/inline，
    // 患者全程不离开这条河。排除经典视图、流自身、以及「独立页打开」的显式放行标记。
    if (from.path.startsWith('/doctorPage/d-flow/')
        && path.startsWith('/doctorPage/')
        && !path.startsWith('/doctorPage/d-flow')
        && path !== '/doctorPage/d-home'
        && !to.query.__flowStandalone) {
        return {
            path: '/doctorPage/d-flow/inline',
            query: {...to.query, __flowTarget: path}
        }
    }

    // 后台 / 医生端：必须登录且为员工角色（此前完全依赖后端 403，URL 直达会渲染空壳）
    if (path.startsWith("/backstage") || path.startsWith("/doctorPage")) {
        try {
            const role = await reqGetLogin()
            if (role !== RoleEnum.DOCTOR) return "/login"
        } catch {
            return "/login"
        }
        const minLv = Number(to.meta?.minAuthLv ?? 0)
        if (minLv > 0) {
            try {
                const doctor = await reqGetLoginD()
                if (!doctor || Number((doctor as any).authLv ?? 0) < minLv) {
                    ElMessage.warning("该页面需要更高权限")
                    return "/backstage"
                }
            } catch {
                return "/login"
            }
        }
        return true
    }

    // 顾客中心 / 聊天：必须是顾客本人
    if (path === "/reception/clientCenter"
        || path.startsWith("/reception/clientCenter/")
        || path.startsWith("/reception/chat")) {
        try {
            const role = await reqGetLogin()
            if (role !== RoleEnum.CLIENT) return "/login"
        } catch {
            return "/login"
        }
    }

    // 顾客端首改密：标记只在检查成功后写入
    if (!path.startsWith("/reception") || path === "/changePwd") return true
    if (sessionStorage.getItem(PWD_CHECK_KEY)) return true
    try {
        const role = await reqGetLogin()
        if (role !== RoleEnum.CLIENT) return true
        const user = await reqLoginUser()
        if (user && user.clientPwdChanged === false) {
            sessionStorage.removeItem(PWD_CHECK_KEY)
            return "/changePwd"
        }
        sessionStorage.setItem(PWD_CHECK_KEY, "1")
    } catch {
        sessionStorage.removeItem(PWD_CHECK_KEY)
        // 未登录等异常由 axios 401 拦截器处理
    }
    return true
})

export function clearPwdCheckFlag() {
    sessionStorage.removeItem(PWD_CHECK_KEY)
}

export default router
