import {Page} from "@/model/DO/Page";
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
// 入库（增加库存并记台账）
export const reqDrugInbound = (data: { drugId: number; quantity: number; expireDate?: string; note?: string }): Promise<Res> =>
    reqApi.post<string, Res>("drug/inbound", {}, {params: data})
// 出入库台账分页
export const reqDrugRecordPage = (params: { drugId?: number; numPage: number; pageSize: number }): Promise<Page<import("@/model/entiy/DrugRecord").DrugRecord>> =>
    reqApi<string, Page<import("@/model/entiy/DrugRecord").DrugRecord>>("drug/record/page", {params})
