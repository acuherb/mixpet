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
// 发药核对（发药人取当前登录人，后端留痕）
export const reqBillDispense = (billId: number): Promise<Res> =>
    reqApi.post<string, Res>(`bill/dispense/${billId}`)
// 退款冲正（退还余额、回滚库存；原因必填，操作人取登录人）
// prescriptionIds 为空=整单退；传了=部分退（只退选中医嘱行）
export const reqBillRefund = (billId: number, reason: string, prescriptionIds?: number[]): Promise<Res> =>
    reqApi.put<string, Res>(`bill/refund/${billId}`, {reason, prescriptionIds: prescriptionIds ?? []})
