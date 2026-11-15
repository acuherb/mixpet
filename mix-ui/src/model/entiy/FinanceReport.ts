/**
 * 经营财务报表
 */
export interface FinanceReport {
    startDate: string | null;
    endDate: string | null;
    totalRevenue: number;
    /** 优惠合计（会员折扣让利） */
    discountMoney: number;
    /** 收款方式构成（折后实收、已扣退款） */
    payBalance: number;
    payCash: number;
    payWechat: number;
    payBalanceCount: number;
    payCashCount: number;
    payWechatCount: number;
    orderCount: number;
    avgOrder: number;
    refundCount: number;
    refundAmount: number;
    feeRegister: number;
    feeTreat: number;
    feeDrug: number;
    feeLab: number;
    feeImage: number;
    feePackage: number;
    feeSurgery: number;
    feeHospital: number;
    drugCost: number;
    drugProfit: number;
    trend: { date: string; amount: number }[];
    byDepartment: { name: string; amount: number }[];
    byDoctor: { name: string; amount: number }[];
}
