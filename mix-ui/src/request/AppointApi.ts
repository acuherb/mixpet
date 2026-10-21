import reqApi from "@/request/reqAPI";
import {AppointDto} from "@/model/DO/AppointDto";
import {YAppointList} from "@/model/VO/BackQuery";
import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {Appoint} from "@/model/entiy/Appoint";
// 用户查看一个医生的挂号
export const reqDoctorOneA = (doctorId: number): Promise<AppointDto[]> =>
    reqApi<string, AppointDto[]>("appointment/doctor?doctorId=" + doctorId)
// 用户个人中心的挂号
export const reqClientAppoint = (): Promise<AppointDto[]> =>
    reqApi<string, AppointDto[]>("appointment/client")
// 后台挂号列表
export const reqAppointList = (page: YAppointList): Promise<Page<AppointDto>> =>
    reqApi<string, Page<AppointDto>>("appointment/page", {params: page})
// 管理员新增挂号
export const reqAddAppoint = (data: Appoint): Promise<Res> =>
    reqApi.post<string, Res>("appointment", data)
// 顾客自己预约挂号
export const reqAddSelfAppoint = (data: Appoint): Promise<Res> =>
    reqApi.post<string, Res>("appointment/self", data)
// 顾客改期自己的待就诊预约（仅改就诊时间）
export const reqRescheduleSelfAppoint = (id: number, appointmentDate: string): Promise<Res> =>
    reqApi.put<string, Res>("appointment/self/" + id, {appointmentDate})
// 顾客取消自己的待就诊预约
export const reqCancelSelfAppoint = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("appointment/self/cancel/" + id)
// 医生标记爽约（预约时间已过未到店，队列不再占位）
export const reqMissAppoint = (id: number): Promise<Res> =>
    reqApi.put<string, Res>("appointment/miss/" + id)
// 顾客查某医生当日已约数（时段余号，date=YYYY-MM-DD）
export const reqDayCount = (doctorId: number, date: string): Promise<number> =>
    reqApi<string, number>("appointment/self/dayCount", {params: {doctorId, date}})
// 管理员修改挂号
export const reqUpdateAppoint = (data: Appoint): Promise<Res> =>
    reqApi.put<string, Res>("appointment", data)
// 管理员删除挂号
export const reqDelAppoint = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("appointment/" + id)
// 管理员批量删除挂号
export const reqDelAppointBatch = (ids: number[]): Promise<Res> =>
    reqApi.delete<string, Res>("appointment/batch/" + ids)
