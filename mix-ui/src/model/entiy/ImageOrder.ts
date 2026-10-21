/**
 * 影像申请单（联查）
 */
export interface ImageOrder {
    orderId: number;
    recordId: number;
    petId: number;
    clientId: number;
    doctorId: number;
    itemId: number;
    itemName: string;
    groupName: string;
    fee: number;
    examPart: string | null;
    result: string | null;
    reportTime: string | null;
    /** false待检查 true已出报告 */
    status: boolean;
    note: string | null;
    /** 报告文件(影像图/PDF)路径，可选 */
    reportFile?: string | null;
    createTime?: string | null;
    petName?: string;
    clientName?: string;
    doctorName?: string;
    diagnosis?: string;
}