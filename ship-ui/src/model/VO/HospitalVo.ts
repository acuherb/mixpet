/**
 * 入院登记请求
 */
export interface HospitalVo {
    recordId: number;
    roomNo: string;
    dailyFee: number;
    note: string;
}