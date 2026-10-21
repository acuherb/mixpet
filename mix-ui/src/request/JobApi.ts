import {Page} from "@/model/DO/Page";
import {Res} from "@/request/Res";
import {Job} from "@/model/entiy/Job";
import {JobDto} from "@/model/DO/JobDto";
import {NameDo} from "@/model/DO/NameDo";
import reqApi from "@/request/reqAPI";

// 下拉用：全部职称（医生自选职称也用）
export const reqJobName = (): Promise<NameDo[]> =>
    reqApi<string, NameDo[]>("job")
// 后台职称分页（带医生数）
export const reqJobPage = (params: { numPage: number; pageSize: number; jobName?: string }): Promise<Page<JobDto>> =>
    reqApi<string, Page<JobDto>>("job/page", {params})
// 新增
export const reqAddJob = (data: Job): Promise<Res> =>
    reqApi.post<string, Res>("job", data)
// 修改
export const reqUpdateJob = (data: Job): Promise<Res> =>
    reqApi.put<string, Res>("job", data)
// 删除
export const reqDelJob = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("job/" + id)
// 批量删除
export const reqDelJobBatch = (ids: number[]): Promise<Res> =>
    reqApi.delete<string, Res>("job/batch/" + ids)
