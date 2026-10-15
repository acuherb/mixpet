/**
 * 经营看板聚合数据
 */
export interface RevenueTrend {
    date: string;
    amount: number;
}
export interface NameCount {
    name: string;
    count: number;
}
export interface StatOverview {
    todayRevenue: number;
    monthRevenue: number;
    pendingBills: number;
    inHospital: number;
    pendingLab: number;
    pendingImage: number;
    dueVaccine: number;
    /** 今日挂号数 */
    todayAppoint: number;
    /** 今日接诊(新建病历)数 */
    todayRecord: number;
    /** 客户总数 */
    clientTotal: number;
    /** 待发药账单数 */
    pendingDispense: number;
    /** 低库存药品数 */
    lowStock: number;
    /** 近效期药品数(90天内) */
    expiringDrug: number;
    revenueTrend: RevenueTrend[];
    doctorTop: NameCount[];
}