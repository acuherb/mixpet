import axios from "axios";
import {ElMessage} from "element-plus";

let reqApi = axios.create({
    baseURL: "/api/",
    timeout: 5000,
    withCredentials: true
})

reqApi.interceptors.request.use(config => {
    return config;
});

reqApi.interceptors.response.use(resp => {
    const data = resp?.data
    if (data && typeof data === "object" && data["code"] != undefined && data["code"] != null) {
        const code = data["code"]
        const url = resp.config.url || ""
        if (code === 401) {
            location.href = "/#/login"
        } else if (code === 403) {
            ElMessage.warning("未持有操作权限")
        } else if (code === 1) {
            // 注意：曾有一条「data 是含 . 的字符串就直接返回字符串」的历史分支，
            // 而 Result.data 的唯一生产者是验证码接口（返回 JWT token，必然含 .）——
            // 它把 token 截胡成裸字符串，导致 register/findPwd/clientOne 拿到非 Res 对象，
            // res.code 为 undefined 走进错误分支（提示“发送失败”且 verifyToken 为空，
            // 注册提交报“请先获取验证码”）。已删除；需要裸字符串的接口（如公告详情）
            // 后端返回的不是 Result 包装，本就走不到这个分支。
            if (data["msg"] && !url.includes("regMail") && !url.includes("findMail")) {
                ElMessage.success(data["msg"])
            }
        } else if (code === 0) {
            ElMessage.error(data["msg"])
        }
    }
    return data;
}, error => {
    if (error.response) {
        const {status, data} = error.response
        ElMessage.error(`请求失败 (${status}): ${data?.msg || data?.message || '服务器内部错误'}`)
    } else if (error.code === 'ECONNABORTED') {
        ElMessage.error('请求超时，请检查后端服务')
    } else {
        ElMessage.error('无法连接后端服务，请确认服务已启动')
    }
    return Promise.reject(error);
});

export default reqApi;