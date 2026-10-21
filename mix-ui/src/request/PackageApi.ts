import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {ServicePackage} from "@/model/entiy/ServicePackage";
import reqApi from "@/request/reqAPI";

// 启用中的套餐（接诊下拉）
export const reqPackageList = (): Promise<ServicePackage[]> =>
    reqApi<string, ServicePackage[]>("package")
// 后台套餐分页
export const reqPackagePage = (params: { numPage: number; pageSize: number; packageName?: string; category?: string }): Promise<Page<ServicePackage>> =>
    reqApi<string, Page<ServicePackage>>("package/page", {params})
// 新增
export const reqAddPackage = (data: ServicePackage): Promise<Res> =>
    reqApi.post<string, Res>("package", data)
// 修改
export const reqUpdatePackage = (data: ServicePackage): Promise<Res> =>
    reqApi.put<string, Res>("package", data)
// 切换启用/停用（列表行内开关）
export const reqSwitchPackage = (id: number, active: boolean): Promise<Res> =>
    reqApi.put<string, Res>("package/status/" + id, {}, {params: {active}})
// 删除
export const reqDelPackage = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("package/" + id)
