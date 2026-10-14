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
    /** 报告文件(图片/PDF)路径，可选 */
    reportFile?: string | null;
    createTime?: string | null;
}