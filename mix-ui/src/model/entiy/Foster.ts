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
    /** 结算金额（登记时=预估；接回时按天数×日均结算） */
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
    createTime?: string | null;
    updateTime?: string | null;
}

/** 寄养状态 */
export const fosterStatusText = (s: number | null | undefined): string =>
    s === 2 ? '已接回' : s === 1 ? '寄养中' : '待入托'

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
