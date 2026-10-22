import {createRouter, createWebHashHistory, RouteRecordRaw} from "vue-router"
import {reception} from "@/router/reception";
import {backstage} from "@/router/backstage";
import {doctorPage} from "@/router/doctorPage";
import {reqGetLogin, reqLoginUser} from "@/request/PowerApi";
import {RoleEnum} from "@/model/VO/LoginVo";

const routes: RouteRecordRaw[] = [
    {
        // 网站主页（未登录也可浏览：公告/医生/领养/寄养）
        path: '/',
        component: () => import('@/views/reception/Index.vue')
    }, {
        // 登录页
        path: '/login',
        component: () => import('@/views/login.vue')
    }, {
        path: '/findPwd',
        component: () => import('@/views/findPwd.vue')
    }, {
        path: '/register',
        component: () => import('@/views/register.vue')
    }, {
        // 顾客端公告列表（更多公告）
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
        path: '/:pathMatch(.*)', // 404路由
        name: 'not-found',
        component: () => import('@/views/page404.vue')
    }
]
export const router = createRouter({
    history: createWebHashHistory(),
    routes: routes
})

/**
 * 首次登录强制改密守卫：顾客未改初始密码时，访问任何顾客端页面都重定向到 /changePwd。
 * sessionStorage 存储标记，页面刷新后仍生效；登出时由 logout 接口（或前端自行）清除。
 */
const PWD_CHECK_KEY = "mixpet_pwd_check_done"
router.beforeEach(async (to) => {
    if (to.path === "/reception/clientCenter"
        || to.path.startsWith("/reception/clientCenter/")
        || to.path.startsWith("/reception/chat")) {
        try {
            const role = await reqGetLogin()
            if (role !== RoleEnum.CLIENT) return "/login"
        } catch {
            return "/login"
        }
    }
    if (sessionStorage.getItem(PWD_CHECK_KEY)) return true
    if (!to.path.startsWith("/reception")) return true
    sessionStorage.setItem(PWD_CHECK_KEY, "1")
    try {
        const role = await reqGetLogin()
        if (role !== RoleEnum.CLIENT) return true
        const user = await reqLoginUser()
        if (user && user.clientPwdChanged === false) {
            sessionStorage.removeItem(PWD_CHECK_KEY)
            return "/changePwd"
        }
    } catch {
        // 未登录等异常由 axios 401 拦截器处理
    }
    return true
})

export function clearPwdCheckFlag() {
    sessionStorage.removeItem(PWD_CHECK_KEY)
}

export default router