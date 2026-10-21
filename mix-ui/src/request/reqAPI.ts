import axios from "axios";
import {ElMessage} from "element-plus";


//创建axios实例
let reqApi = axios.create({
    baseURL: "/api/",
    timeout: 5000,
    withCredentials: true
})

//请求拦截器
reqApi.interceptors.request.use(config => {
    return config;
});
//响应拦截器
reqApi.interceptors.response.use(resp => {
    const data = resp?.data
    if (data && typeof data === "object" && data["code"] != undefined && data["code"] != null) {
        const code = data["code"]
        if (code === 401) {
            // 未登录：去登录页（主页为公开页，其接口均已放行，不会触发这里）
            location.href = "/#/login"
        } else if (code === 403) {
            ElMessage.warning("未持有操作权限")
        } else if (code === 1) {
            ElMessage.success(data["msg"])
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