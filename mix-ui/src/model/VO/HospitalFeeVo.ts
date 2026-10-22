/**
 * 住院费用预览/结算明细（出院结算弹窗用）
 */
export interface HospitalFeeVo {
    hospitalId: number;
    /** 结算天数（默认跨夜+1，可手改） */
    days: number;
    /** 床位费/天 */
    bedFee: number;
    /** 护理费/天 */
    nursingFee: number;
    /** 输液费/天 */
    infusionFee: number;
    /** 日均合计 */
    perDay: number;
    /** 住院天数小计 = perDay × days */
    stayFee: number;
    /** 医嘱临时费用合计（已停嘱未执行不计） */
    adviceFee: number;
    /** 费用总计 */
    totalFee: number;
    /** 已收押金 */
    deposit: number;
    /** 应退 = deposit - totalFee（负数=需补收） */
    refund: number;
}

export const exampleHospitalFee = (): HospitalFeeVo => ({
    hospitalId: 0,
    days: 0,
    bedFee: 0,
    nursingFee: 0,
    infusionFee: 0,
    perDay: 0,
    stayFee: 0,
    adviceFee: 0,
    totalFee: 0,
    deposit: 0,
    refund: 0
})
