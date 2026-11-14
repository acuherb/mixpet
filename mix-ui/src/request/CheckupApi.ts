import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {CheckupRecord, CheckupSaveVo, CheckupTemplate} from "@/model/VO/Checkup";

/** 启用中的模板（接诊下拉） */
export const reqCheckupTemplates = (): Promise<CheckupTemplate[]> =>
    reqApi<string, CheckupTemplate[]>("checkup/template")
/** 管理端模板分页 */
export const reqCheckupTemplatePage = (params: {
    numPage: number; pageSize: number; seaName?: string
}): Promise<Page<CheckupTemplate>> =>
    reqApi<string, Page<CheckupTemplate>>("checkup/template/page", {params})
export const reqCheckupTemplateOne = (id: number): Promise<CheckupTemplate> =>
    reqApi<string, CheckupTemplate>("checkup/template/" + id)
export const reqSaveCheckupTemplate = (data: CheckupTemplate): Promise<Res> =>
    reqApi.post<string, Res>("checkup/template", data)
export const reqUpdateCheckupTemplate = (data: CheckupTemplate): Promise<Res> =>
    reqApi.put<string, Res>("checkup/template", data)
export const reqDelCheckupTemplate = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("checkup/template/" + id)

/** 保存一次体检：返回体是 Result.data(checkupId)，调用方取 res.data */
export const reqSaveCheckup = (data: CheckupSaveVo): Promise<Res> =>
    reqApi.post<string, Res>("checkup/record", data)
/** 单份报告（顾客只能读自己宠物的，越权返回 null） */
export const reqCheckupOne = (id: number): Promise<CheckupRecord> =>
    reqApi<string, CheckupRecord>("checkup/record/" + id)
/** 某宠物的历史报告 */
export const reqCheckupByPet = (petId: number): Promise<CheckupRecord[]> =>
    reqApi<string, CheckupRecord[]>("checkup/record/pet/" + petId)
/** 某次就诊关联的体检报告 */
export const reqCheckupByRecord = (recordId: number): Promise<CheckupRecord[]> =>
    reqApi<string, CheckupRecord[]>("checkup/record/byRecord/" + recordId)
export const reqDelCheckup = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("checkup/record/" + id)
