import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {Drug} from "@/model/entiy/Drug";
import {Res} from "@/request/Res";

// 启用中的药品（医生开方下拉）
export const reqDrugList = (): Promise<Drug[]> =>
    reqApi<string, Drug[]>("drug")
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
