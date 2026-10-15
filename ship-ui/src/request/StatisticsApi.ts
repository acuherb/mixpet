import {StatOverview} from "@/model/entiy/StatOverview";
import {FinanceReport} from "@/model/entiy/FinanceReport";
import reqApi from "@/request/reqAPI";

// 经营看板
export const reqStatOverview = (): Promise<StatOverview> =>
    reqApi<string, StatOverview>("statistics/overview")
// 经营财务报表 [start, end] 闭区间 (YYYY-MM-DD)
export const reqFinanceReport = (start: string, end: string): Promise<FinanceReport> =>
    reqApi<string, FinanceReport>("report/finance", {params: {start, end}})