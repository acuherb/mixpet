import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {LabTest} from "@/model/entiy/LabTest";
import {LabOrderDto} from "@/model/DO/LabOrderDto";
import {LabOrderVo, LabResultVo} from "@/model/VO/LabOrderVo";
import {RecordDto} from "@/model/DO/RecordDto";
import {Requisition} from "@/model/entiy/Requisition";

// ---------- 检验项字典 ----------
// 启用中的检验项（医生开单勾选）
export const reqLabTestList = (): Promise<LabTest[]> =>
    reqApi<string, LabTest[]>("labTest")
// 后台字典分页
export const reqLabTestPage = (params: { numPage: number; pageSize: number; testName?: string; testGroup?: string }): Promise<Page<LabTest>> =>
    reqApi<string, Page<LabTest>>("labTest/page", {params})
// 新增检验项
export const reqAddLabTest = (data: LabTest): Promise<Res> =>
    reqApi.post<string, Res>("labTest", data)
// 修改检验项
export const reqUpdateLabTest = (data: LabTest): Promise<Res> =>
    reqApi.put<string, Res>("labTest", data)
// 删除检验项
export const reqDelLabTest = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("labTest/" + id)

// ---------- 化验单 ----------
// 医生开化验单（含计费）
export const reqCreateLabOrder = (data: LabOrderVo): Promise<Res> =>
    reqApi.post<string, Res>("lab/order", data)
// 化验单分页（录入端，status: 0待检验 1已出结果，可空=全部）
export const reqLabOrderPage = (params: { numPage: number; pageSize: number; status?: number | null }): Promise<Page<LabOrderDto>> =>
    reqApi<string, Page<LabOrderDto>>("lab/page", {params})
// 某病历的化验单（含明细）
export const reqLabByRecord = (recordId: number): Promise<LabOrderDto[]> =>
    reqApi<string, LabOrderDto[]>("lab/record/" + recordId)
// 某医生接诊病历（开单选病历）
export const reqDoctorRecords = (): Promise<RecordDto[]> =>
    reqApi<string, RecordDto[]>("lab/records")
// 录入化验结果（异常/危急由服务端复算，修订需带 reviseReason）
export const reqSaveLabResult = (data: LabResultVo): Promise<Res> =>
    reqApi.put<string, Res>("lab/result", data)
// 复核检验报告（录入人之外再签审核人）
export const reqReviewLabResult = (orderId: number): Promise<Res> =>
    reqApi.put<string, Res>("lab/result/review/" + orderId, {})
// 危急值处置闭环：标记已处置
export const reqHandleCritical = (orderId: number): Promise<Res> =>
    reqApi.put<string, Res>("lab/result/critical/" + orderId, {})
// 撤销复核（管理层）：复核锁解除后可再次修订
export const reqRevokeReview = (orderId: number): Promise<Res> =>
    reqApi.put<string, Res>("lab/result/revoke/" + orderId, {})
// 作废化验单（未出结果）
export const reqLabCancel = (orderId: number): Promise<Res> =>
    reqApi.put<string, Res>("lab/cancel/" + orderId)
// 检验申请单（国标，可打印）
export const reqLabRequisition = (orderId: number): Promise<Requisition> =>
    reqApi<string, Requisition>("lab/requisition/" + orderId)