import reqApi from "@/request/reqAPI";
import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {Supplier} from "@/model/entiy/Supplier";

// 启用中的供应商（采购入库下拉用）
export const reqSupplierList = (): Promise<Supplier[]> =>
    reqApi<string, Supplier[]>("supplier")
// 供应商分页
export const reqSupplierPage = (params: { numPage: number; pageSize: number; seaName?: string }): Promise<Page<Supplier>> =>
    reqApi<string, Page<Supplier>>("supplier/page", {params})
// 新增供应商
export const reqAddSupplier = (data: Supplier): Promise<Res> =>
    reqApi.post<string, Res>("supplier", data)
// 修改供应商
export const reqUpdateSupplier = (data: Supplier): Promise<Res> =>
    reqApi.put<string, Res>("supplier", data)
// 删除供应商
export const reqDelSupplier = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("supplier/" + id)
