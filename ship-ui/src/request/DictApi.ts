import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {SysDict} from "@/model/entiy/SysDict";

// 某类型字典值（各模块下拉用）
export const reqDictByType = (type: string): Promise<SysDict[]> =>
    reqApi<string, SysDict[]>("dict/type/" + type)
// 新增字典项
export const reqAddDict = (data: SysDict): Promise<Res> =>
    reqApi.post<string, Res>("dict", data)
// 修改字典项
export const reqUpdateDict = (data: SysDict): Promise<Res> =>
    reqApi.put<string, Res>("dict", data)
// 启停
export const reqDictToggle = (id: number, active: boolean): Promise<Res> =>
    reqApi.put<string, Res>("dict/toggle/" + id, {}, {params: {active}})
// 删除
export const reqDelDict = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("dict/" + id)
