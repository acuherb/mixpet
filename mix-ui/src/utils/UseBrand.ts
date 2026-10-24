import {ref} from "vue";
import {reqHospitalSetting} from "@/request/SettingApi";
import {HospitalSetting, emptyHospital} from "@/model/entiy/HospitalSetting";

/**
 * 单据品牌信息（医院名 + 品牌色）
 * 处方笺/申请单/同意书/顾客端就诊资料共用：后台改「医院资料」即时生效
 */
export const useBrand = () => {
    const hosp = ref<HospitalSetting>(emptyHospital())
    const brand = ref<string>("#1E6FA8")

    const load = (): void => {
        reqHospitalSetting()
            .then(h => {
                if (h) hosp.value = h
                // 后台未配置品牌色时回落默认蓝（与系统 --brand 默认值一致）
                brand.value = (h && h.brandColor) || "#1E6FA8"
            })
            .catch(() => {
                hosp.value = emptyHospital()
                brand.value = "#1E6FA8"
            })
    }

    return {hosp, brand, load}
}
