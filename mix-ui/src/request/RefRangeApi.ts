import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {Page} from "@/model/DO/Page";

/** 检验参考区间（按检验项/物种/性别/月龄分层，含危急阈值） */
export interface LabRefRange {
    rangeId?: number;
    testId: number;
    species?: string | null;
    sex?: string | null;
    ageMinMonth?: number | null;
    ageMaxMonth?: number | null;
    refLow?: number | null;
    refHigh?: number | null;
    criticalLow?: number | null;
    criticalHigh?: number | null;
    refText?: string | null;
    active?: boolean | null;
}

// 按检验项查全部区间（维护页用）
export const reqRefRangeList = (testId?: number): Promise<LabRefRange[]> =>
    reqApi<string, LabRefRange[]>("refRange", {params: {testId: testId ?? ''}})
export const reqRefRangePage = (params: { numPage: number; pageSize: number; testId?: number; keyword?: string }): Promise<Page<LabRefRange>> =>
    reqApi<string, Page<LabRefRange>>("refRange/page", {params})
export const reqAddRefRange = (data: LabRefRange): Promise<Res> =>
    reqApi.post<string, Res>("refRange", data)
export const reqUpdateRefRange = (data: LabRefRange): Promise<Res> =>
    reqApi.put<string, Res>("refRange", data)
export const reqDelRefRange = (id: number): Promise<Res> =>
    reqApi.delete<string, Res>("refRange/" + id)
