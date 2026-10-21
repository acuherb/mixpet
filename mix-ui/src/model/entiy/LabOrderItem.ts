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
    isAbnormal: boolean | null;
    resultTime: string | null;
}