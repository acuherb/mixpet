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
    revenueTrend: RevenueTrend[];
    doctorTop: NameCount[];
}