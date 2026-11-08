import axios from "axios";
import {ElMessage} from "element-plus";

let reqApi = axios.create({
    baseURL: "/api/",
    timeout: 5000,
    withCredentials: true
})

/** 写请求幂等：所有写操作带 Idempotency-Key，并在前端拦截短时间内的重复提交 */
const WRITE_METHODS = new Set(["post", "put", "delete", "patch"])
const recentWrites = new Map<string, number>()
const DEDUPE_MS = 1200

const reqKey = (config: any): string => [
    config.method, config.url,
    JSON.stringify(config.params ?? {}),
    JSON.stringify(config.data ?? {})
].join("|")

const newIdempotencyKey = (): string => {
    const c: any = globalThis.crypto
    return c?.randomUUID ? c.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

reqApi.interceptors.request.use(config => {
    const method = (config.method || "get").toLowerCase()
    if (!WRITE_METHODS.has(method)) return config
    config.headers.set("Idempotency-Key", newIdempotencyKey())
    const key = reqKey(config)
    const now = Date.now()
    if (now - (recentWrites.get(key) ?? 0) < DEDUPE_MS) {
        // 交给响应错误分支统一提示，不产生未捕获异常
        return Promise.reject({
            __duplicate: true,
            config,
            response: {data: {code: 0, msg: "请求已提交，请勿重复操作"}, config}
        })
    }
    recentWrites.set(key, now)
    if (recentWrites.size > 300) {
        for (const [k, t] of recentWrites) if (now - t > DEDUPE_MS * 2) recentWrites.delete(k)
    }
    return config;
});

reqApi.interceptors.response.use(resp => {
    const data = resp?.data
    if (data && typeof data === "object" && data["code"] != undefined && data["code"] != null) {
        const code = data["code"]
        const url = resp.config.url || ""
        if (code === 401) {
            // 登录态失效时清掉首改密标记，避免下次登录被旧标记绕过
            sessionStorage.removeItem("mixpet_pwd_check_done")
            location.href = "/#/login"
        } else if (code === 403) {
            ElMessage.warning("此功能需要相应权限，请联系管理员开通")
        } else if (code === 1) {
            if (data["msg"] && !url.includes("regMail") && !url.includes("findMail")) {
                ElMessage.success(data["msg"])
            }
        } else if (code === 0) {
            const msg = data["msg"] || ""
            // 后端强制首次改密：不能只靠前端 sessionStorage 守卫
            if (msg.includes("请先修改初始密码")) {
                sessionStorage.removeItem("mixpet_pwd_check_done")
                location.href = "/#/changePwd"
            }
            ElMessage.error(msg)
        }
    }
    return data;
}, error => {
    if (error?.__duplicate) {
        ElMessage.warning("请求已提交，请勿重复操作")
        // 返回普通业务体，调用方 .then 里按 code=0 处理，不会 unhandled rejection
        return Promise.resolve({code: 0, msg: "请求已提交，请勿重复操作"})
    }
    if (axios.isCancel(error)) return Promise.reject(error)
    if (error.response) {
        const {status, data} = error.response
        ElMessage.error(`请求失败 (${status}): ${data?.msg || data?.message || '服务器内部错误'}`)
    } else if (error.code === 'ECONNABORTED') {
        ElMessage.error('请求超时，请重试；若持续出现请联系管理员')
    } else {
        ElMessage.error('暂时无法连接服务，请检查网络或联系管理员')
    }
    return Promise.reject(error);
});

export default reqApi;
