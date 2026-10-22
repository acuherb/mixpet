/**
 * 入院登记请求
 */
export interface HospitalVo {
    recordId: number;
    roomNo: string;
    /** 日均住院费 = 床位+护理+输液（前端汇总传入，兼容旧字段） */
    dailyFee: number;
    /** 床位费/天 */
    bedFee?: number;
    /** 护理费/天 */
    nursingFee?: number;
    /** 输液费/天 */
    infusionFee?: number;
    note: string;
}