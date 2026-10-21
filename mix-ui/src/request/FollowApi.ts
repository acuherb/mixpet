import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {FollowUp, FollowUpDto} from "@/model/entiy/FollowUp";

// 创建随访计划
export const reqAddFollow = (data: FollowUp): Promise<Res> =>
    reqApi.post<string, Res>("follow", data)
// 随访管理分页
export const reqFollowPage = (params: { numPage: number; pageSize: number; status?: boolean | null; seaName?: string }): Promise<Page<FollowUpDto>> =>
    reqApi<string, Page<FollowUpDto>>("follow/page", {params})
// 某病历的随访记录
export const reqFollowByRecord = (recordId: number): Promise<FollowUp[]> =>
    reqApi<string, FollowUp[]>("follow/record/" + recordId)
// 完成随访
export const reqFollowDone = (followId: number, result: string): Promise<Res> =>
    reqApi.put<string, Res>("follow/done/" + followId, {}, {params: {result}})
