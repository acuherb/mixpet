import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {DoctorSchedule} from "@/model/entiy/DoctorSchedule";

// 某医生排班（编辑回显）
export const reqScheduleByDoctor = (doctorId: number): Promise<DoctorSchedule[]> =>
    reqApi<string, DoctorSchedule[]>("schedule/doctor/" + doctorId)
// 排班总览
export const reqScheduleAll = (): Promise<DoctorSchedule[]> =>
    reqApi<string, DoctorSchedule[]>("schedule/all")
// 保存某医生一周排班（覆盖式）
export const reqSaveSchedule = (list: DoctorSchedule[]): Promise<Res> =>
    reqApi.post<string, Res>("schedule", list)
