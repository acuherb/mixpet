import {Page} from "@/model/DO/Page";
import reqApi from "@/request/reqAPI";
import {Res} from "@/request/Res";
import {HospitalDto} from "@/model/DO/HospitalDto";
import {HospitalVo} from "@/model/VO/HospitalVo";
import {HospitalFeeVo} from "@/model/VO/HospitalFeeVo";
import {HospitalMonitor} from "@/model/entiy/HospitalMonitor";

// 入院登记
export const reqSaveHospital = (data: HospitalVo): Promise<Res> =>
    reqApi.post<string, Res>("hospital", data)
// 出院结算（计住院费入账单；days 可手改结算天数）
export const reqDischargeHospital = (id: number, days?: number): Promise<Res> =>
    reqApi.put<string, Res>("hospital/discharge/" + id, {}, {params: days ? {days} : {}})
// 收取押金（累加）
export const reqAddHospitalDeposit = (id: number, amount: number): Promise<Res> =>
    reqApi.post<string, Res>("hospital/deposit/" + id, {}, {params: {amount}})
// 费用预览（结算弹窗明细）
export const reqHospitalFee = (id: number): Promise<HospitalFeeVo> =>
    reqApi<string, HospitalFeeVo>("hospital/fee/" + id)
// 住院列表分页
export const reqHospitalPage = (params: { numPage: number; pageSize: number; status?: boolean | null }): Promise<Page<HospitalDto>> =>
    reqApi<string, Page<HospitalDto>>("hospital/page", {params})
// 某病历的住院记录
export const reqHospitalByRecord = (recordId: number): Promise<HospitalDto[]> =>
    reqApi<string, HospitalDto[]>("hospital/record/" + recordId)
// 添加住院监护记录
export const reqAddHospitalMonitor = (data: HospitalMonitor): Promise<Res> =>
    reqApi.post<string, Res>("hospital/monitor", data)
// 某住院单的监护记录
export const reqHospitalMonitorList = (hospitalId: number): Promise<HospitalMonitor[]> =>
    reqApi<string, HospitalMonitor[]>("hospital/monitor/" + hospitalId)