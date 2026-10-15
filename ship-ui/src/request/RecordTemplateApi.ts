import reqApi from "@/request/reqAPI";
import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {RecordTemplate} from "@/model/entiy/RecordTemplate";

// 启用中的模板（接诊下拉）
export const reqRecordTemplateList = (): Promise<RecordTemplate[]> =>
    reqApi<string, RecordTemplate[]>("recordTemplate")
// 模板分页
export const reqRecordTemplatePage = (params: { numPage: number; pageSize: number; seaName?: string }): Promise<Page<RecordTemplate>> =>
    reqApi<string, Page<RecordTemplate>>("recordTemplate/page", {params})
// 新增模板
export const reqAddRecordTemplate = (data: RecordTemplate): Promise<Res> =>
    reqApi.post<string, Res>("recordTemplate", data)
// 修改模板
export const reqUpdateRecordTemplate = (data: RecordTemplate): Promise<Res> =>
    reqApi.put<string, Res>("recordTemplate", data)
// 删除模板
export const reqDelRecordTemplate = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("recordTemplate/" + id)
