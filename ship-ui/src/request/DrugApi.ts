import {Page} from "@/model/DO/Page";
import {InboundBatchVo, InboundLine} from "@/model/entiy/InboundBatch";
import reqApi from "@/request/reqAPI";
import {Drug} from "@/model/entiy/Drug";
import {Res} from "@/request/Res";

// 启用中的药品（医生开方下拉）
export const reqDrugList = (): Promise<Drug[]> =>
    reqApi<string, Drug[]>("drug")
// 启用中的疫苗（免疫登记下拉，即 drug_type=疫苗）
export const reqDrugVaccineList = (): Promise<Drug[]> =>
    reqApi<string, Drug[]>("drug/vaccine")
// 后台药品分页
export const reqDrugPage = (params: { numPage: number; pageSize: number; drugName?: string; drugType?: string }): Promise<Page<Drug>> =>
    reqApi<string, Page<Drug>>("drug/page", {params})
// 新增药品
export const reqAddDrug = (data: Drug): Promise<Res> =>
    reqApi.post<string, Res>("drug", data)
// 修改药品
export const reqUpdateDrug = (data: Drug): Promise<Res> =>
    reqApi.put<string, Res>("drug", data)
// 删除药品
export const reqDelDrug = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("drug/" + id)
// 批量删除药品
export const reqDelDrugBatch = (ids: number[]): Promise<Res> =>
    reqApi.delete<string, Res>("drug/batch/" + ids)
// 低库存预警列表
export const reqDrugWarning = (): Promise<Drug[]> =>
    reqApi<string, Drug[]>("drug/warning")
// 近效期预警列表（未来 days 天内）
export const reqDrugExpiring = (days: number): Promise<Drug[]> =>
    reqApi<string, Drug[]>("drug/expiring", {params: {days}})
// 入库（增加库存并记台账，可带供应商/批号）
export const reqDrugInbound = (data: { drugId: number; quantity: number; costPrice?: number; expireDate?: string; note?: string; supplierId?: number; batchNo?: string }): Promise<Res> =>
    reqApi.post<string, Res>("drug/inbound", {}, {params: data})
// 批量入库（逐条独立提交、部分成功，返回逐行结果）
export const reqDrugInboundBatch = (lines: InboundLine[]): Promise<InboundBatchVo> =>
    reqApi.post<string, InboundBatchVo>("drug/inbound/batch", lines)
// 出入库台账分页
export const reqDrugRecordPage = (params: { drugId?: number; numPage: number; pageSize: number }): Promise<Page<import("@/model/entiy/DrugRecord").DrugRecord>> =>
    reqApi<string, Page<import("@/model/entiy/DrugRecord").DrugRecord>>("drug/record/page", {params})
