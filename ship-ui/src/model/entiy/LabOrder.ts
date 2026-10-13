/**
 * 化验单（单头）
 */
export interface LabOrder {
    orderId: number;
    recordId: number;
    petId: number;
    clientId: number;
    doctorId: number;
    orderNote: string | null;
    fee: number | null;
    /** false待检验 true已出结果 */
    status: boolean;
    resultNote: string | null;
    resultBy: string | null;
    createTime?: string | null;
}