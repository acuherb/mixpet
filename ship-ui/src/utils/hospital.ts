import {ref} from 'vue'
import {reqHospitalSetting} from "@/request/SettingApi";
import {emptyHospital, HospitalSetting} from "@/model/entiy/HospitalSetting";

/**
 * 医院信息全局单例（贴面场景：页脚/首页/title 缓存一次，App 启动加载）
 * 注意：打印页（window.open/独立渲染）不用此缓存，自行拉接口保证取到最新值
 */
export const hospital = ref<HospitalSetting>(emptyHospital())

let loading: Promise<void> | null = null

/** 加载医院信息（已加载则跳过；force=true 强制刷新） */
export const loadHospital = (force = false): Promise<void> => {
    if (loading && !force) return loading
    loading = reqHospitalSetting().then(s => {
        if (s) hospital.value = s
    }).catch(() => { /* 接口异常时保持默认值，不阻断页面 */ })
    return loading
}
