import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {BillDto} from "@/model/DO/BillDto";
import {Bill} from "@/model/entiy/Bill";
import {Res} from "@/request/Res";

// 收费列表（payStatus: 0未付 1已付 -1全部）
export const reqBillPage = (params: { numPage: number; pageSize: number; seaName?: string; payStatus?: number }): Promise<Page<BillDto>> =>
    reqApi<string, Page<BillDto>>("bill/page", {params})
// 顾客查自己的账单（待缴费提醒；实际收款到前台）
export const reqClientBill = (): Promise<Bill[]> =>
    reqApi<string, Bill[]>("bill/self")
// 收款（payType: false余额 true现金）
export const reqBillPay = (billId: number, payType: boolean): Promise<Res> =>
    reqApi.put<string, Res>(`bill/pay/${billId}`, {}, {params: {payType}})
// 发药核对（护士确认发药）
export const reqBillDispense = (billId: number, operator: string): Promise<Res> =>
    reqApi.post<string, Res>(`bill/dispense/${billId}`, {}, {params: {operator}})
// 退款冲正（退还余额、回滚库存）
export const reqBillRefund = (billId: number, operator: string): Promise<Res> =>
    reqApi.put<string, Res>(`bill/refund/${billId}`, {}, {params: {operator}})
