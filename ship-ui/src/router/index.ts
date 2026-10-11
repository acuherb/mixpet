import {createRouter, createWebHashHistory, RouteRecordRaw} from "vue-router"
import {reception} from "@/router/reception";
import {backstage} from "@/router/backstage";
import {doctorPage} from "@/router/doctorPage";
import {reqGetLogin, reqLoginUser} from "@/request/PowerApi";
import {RoleEnum} from "@/model/VO/LoginVo";

const routes: RouteRecordRaw[] = [
    {
        path: '/',
        component: () => import('@/views/login.vue')
    }, {
        path: '/findPwd',
        component: () => import('@/views/findPwd.vue')
    }, {
        path: '/register',
        component: () => import('@/views/register.vue')
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
 * 一次会话只查一次；改密成功后重新生效检查。
 */
let pwdCheckDone = false
router.beforeEach(async (to) => {
    if (pwdCheckDone) return true
    // 只管顾客端；登录/注册/改密等页面和后台/医生端不拦截（后台医生端各自有登录检查）
    if (!to.path.startsWith("/reception")) return true
    pwdCheckDone = true
    try {
        const role = await reqGetLogin()
        if (role !== RoleEnum.CLIENT) return true
        const user = await reqLoginUser()
        if (user && user.clientPwdChanged === false) {
            pwdCheckDone = false // 未改密，等改完再查
            return "/changePwd"
        }
    } catch (e) {
        // 未登录等异常交给 axios 401 拦截器处理
    }
    return true
})

export default router
