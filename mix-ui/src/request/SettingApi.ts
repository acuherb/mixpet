import {HospitalSetting} from "@/model/entiy/HospitalSetting";
import {Res} from "@/request/Res";
import reqApi from "@/request/reqAPI";

// 医院信息（公开读取：打印页/页脚/title 用）
export const reqHospitalSetting = (): Promise<HospitalSetting> =>
    reqApi<string, HospitalSetting>("setting/hospital")
// 保存医院信息（仅管理员）
export const reqSaveHospitalSetting = (data: HospitalSetting): Promise<Res> =>
    reqApi.put<string, Res>("setting/hospital", data)
