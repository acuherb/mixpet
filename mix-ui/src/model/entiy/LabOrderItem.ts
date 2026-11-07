/**
 * 化验单明细
 */
export interface LabOrderItem {
    itemId: number;
    orderId: number;
    testId: number | null;
    itemName: string;
    itemGroup: string | null;
    itemUnit: string | null;
    refRange: string | null;
    resultValue: string | null;
    /** 异常由服务端按参考区间复算，前端只做录入预检 */
    isAbnormal: boolean | null;
    /** 按物种/月龄/性别匹配的数值化参考区间与危急阈值 */
    refLow?: number | null;
    refHigh?: number | null;
    criticalLow?: number | null;
    criticalHigh?: number | null;
    criticalFlag?: boolean | null;
    resultTime: string | null;
}