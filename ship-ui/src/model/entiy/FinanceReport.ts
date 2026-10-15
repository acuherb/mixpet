/**
 * 经营财务报表
 */
export interface FinanceReport {
    startDate: string | null;
    endDate: string | null;
    totalRevenue: number;
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
