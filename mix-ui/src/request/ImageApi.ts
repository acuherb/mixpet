import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {ImageOrder} from "@/model/entiy/ImageOrder";
import {ImageTest} from "@/model/entiy/ImageTest";
import {Requisition} from "@/model/entiy/Requisition";
import reqApi from "@/request/reqAPI";

// 影像项目字典 ----------------------------------------------------------
// 全部分组列表
export const reqImgGroup = (): Promise<ImageTest[]> =>
    reqApi<string, ImageTest[]>("imageTest/group")
// 字典分页
export const reqImageTestPage = (params: { itemName: string; groupName: string; numPage: number; pageSize: number }): Promise<Page<ImageTest>> =>
    reqApi<string, Page<ImageTest>>("imageTest/page", {params})
// 单个影像项（录报告时取报告模板）
export const reqImageTestOne = (itemId: number): Promise<ImageTest> =>
    reqApi<string, ImageTest>("imageTest/one/" + itemId)
// 新增/修改
export const reqAddImageTest = (data: ImageTest): Promise<Res> =>
    reqApi.put<string, Res>("imageTest", data)
export const reqUpdateImageTest = (data: ImageTest): Promise<Res> =>
    reqApi.put<string, Res>("imageTest", data)
// 删除（被引用则停用）
export const reqDelImageTest = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("imageTest/item/" + id)

// 影像申请单 ------------------------------------------------------------
// 医生开单
export const reqCreateImageOrder = (data: { recordId: number; itemId: number; examPart: string; note: string }): Promise<Res> =>
    reqApi.post<string, Res>("imageOrder", data)
// 报告录入
export const reqSaveImageReport = (data: { orderId: number; result: string; reportFile?: string }): Promise<Res> =>
    reqApi.put<string, Res>("imageOrder/report", data)
// 作废申请单（未出报告）
export const reqImageCancel = (orderId: number): Promise<Res> =>
    reqApi.put<string, Res>("imageOrder/cancel/" + orderId)
// 申请单分页
export const reqImageOrderPage = (params: { numPage: number; pageSize: number; status?: boolean | null }): Promise<Page<ImageOrder>> =>
    reqApi<string, Page<ImageOrder>>("imageOrder/page", {params})
// 某病历的申请单
export const reqImageByRecord = (recordId: number): Promise<ImageOrder[]> =>
    reqApi<string, ImageOrder[]>("imageOrder/record/" + recordId)
// 影像申请单（国标，可打印）
export const reqImageRequisition = (orderId: number): Promise<Requisition> =>
    reqApi<string, Requisition>("imageOrder/requisition/" + orderId)