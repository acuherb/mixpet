import {Page} from "@/model/DO/Page";
import {RxArchiveVo, RxPrintPrepareVo, RxPrintResultVo} from "@/model/entiy/RxSheet";
import {Res} from "@/request/Res";
import reqApi from "@/request/reqAPI";

// 处方笺打印预检（生成/复用编号 + 补录合并，返回全量打印数据）
export const reqRxPrintPrepare = (vo: RxPrintPrepareVo): Promise<RxPrintResultVo> =>
    reqApi.post<string, RxPrintResultVo>("record/rx/print-prepare", vo)
// 处方笺档案分页（管理端）
export const reqRxArchive = (params: {
    startDate?: string; endDate?: string; doctorId?: number | null;
    keyword?: string; printed?: boolean | null; status?: string; numPage: number; pageSize: number
}): Promise<Page<RxArchiveVo>> =>
    reqApi.get<string, Page<RxArchiveVo>>("record/rx/archive", {params})
// 作废处方笺编号（占号不回收）
export const reqRxVoid = (printNo: string, voidReason: string): Promise<Res> =>
    reqApi.post<string, Res>("record/rx/archive/void/" + printNo, {}, {params: {voidReason}})
