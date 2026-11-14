export interface Foster {
    fosterId: number;
    fosterCode: string;
    petId: number | null;
    clientId: number | null;
    /** 预计接回日期（到期时间） */
    fosterTerm: string;
    /** 入托日期 */
    inDate?: string;
    /** 实际接回日期 */
    backDate?: string | null;
    /** 寄养押金（登记时预收；结算不再覆盖） */
    fosterMoney: number;
    /** 笼位费/天 */
    dailyFee?: number;
    /** 清洁费/天 */
    cleanFee?: number;
    /** 餐费/天 */
    mealFee?: number;
    /** 0待入托 1寄养中 2已接回 */
    status?: number;
    fosterInfo: string;
    /** 结算明细（接回后才有值） */
    dayCount?: number | null;
    settleMoney?: number | null;
    depositUsed?: number | null;
    refundMoney?: number | null;
    chargeMoney?: number | null;
    adjustNote?: string | null;
    billId?: number | null;
    createTime?: string | null;
    updateTime?: string | null;
}

/** 寄养状态 */
export const fosterStatusText = (s: number | null | undefined): string =>
    s === 2 ? '已接回' : s === 1 ? '寄养中' : '待入托'

/** 寄养天数：含首尾各算一天，与后端 FosterServiceImpl.back() 完全一致 */
export const fosterDays = (inDate?: string | null, outDate?: string | null): number => {
    if (!inDate || !outDate) return 0
    const a = new Date(String(inDate).slice(0, 10)).getTime()
    const b = new Date(String(outDate).slice(0, 10)).getTime()
    if (isNaN(a) || isNaN(b) || b < a) return 0
    return Math.round((b - a) / 86400000) + 1
}

/**
 * 空数据
 */
export const exampleFoster = (): Foster => {
    return {
        fosterId: 0,
        fosterCode: "",
        petId: null,
        clientId: null,
        fosterTerm: "",
        inDate: "",
        backDate: null,
        fosterMoney: 0,
        dailyFee: 0,
        cleanFee: 0,
        mealFee: 0,
        status: 0,
        fosterInfo: ""
    }
}
