import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {BillDto} from "@/model/DO/BillDto";
import {Res} from "@/request/Res";

// 收费列表（payStatus: 0未付 1已付 -1全部）
export const reqBillPage = (params: { numPage: number; pageSize: number; seaName?: string; payStatus?: number }): Promise<Page<BillDto>> =>
    reqApi<string, Page<BillDto>>("bill/page", {params})
// 收款（payType: false余额 true现金）
export const reqBillPay = (billId: number, payType: boolean): Promise<Res> =>
    reqApi.put<string, Res>(`bill/pay/${billId}`, {}, {params: {payType}})
