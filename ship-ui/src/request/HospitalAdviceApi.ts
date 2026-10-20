import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {HospitalAdvice} from "@/model/entiy/HospitalAdvice";

// 下达医嘱
export const reqAddAdvice = (data: HospitalAdvice): Promise<Res> =>
    reqApi.post<string, Res>("hospitalAdvice", data)
// 批量下达（从本病历处方导入）
export const reqAddAdviceBatch = (list: HospitalAdvice[]): Promise<Res> =>
    reqApi.post<string, Res>("hospitalAdvice/batch", list)
// 停嘱（原因必填）
export const reqStopAdvice = (id: number, reason: string): Promise<Res> =>
    reqApi.put<string, Res>("hospitalAdvice/stop/" + id, {}, {params: {reason}})
// 标记完成
export const reqDoneAdvice = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("hospitalAdvice/done/" + id)
// 某次住院的医嘱列表
export const reqAdviceByHospital = (hospitalId: number): Promise<HospitalAdvice[]> =>
    reqApi<string, HospitalAdvice[]>("hospitalAdvice/hospital/" + hospitalId)
// 某病历的医嘱（跨住院）
export const reqAdviceByRecord = (recordId: number): Promise<HospitalAdvice[]> =>
    reqApi<string, HospitalAdvice[]>("hospitalAdvice/record/" + recordId)
