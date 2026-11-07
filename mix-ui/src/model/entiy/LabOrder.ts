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
    /** 标本类型与采样/接收时间（临床时间轴） */
    specimenType?: string | null;
    collectTime?: string | null;
    receiveTime?: string | null;
    /** 复核人 / 复核时间（与录入人分离） */
    reviewBy?: string | null;
    reviewAt?: string | null;
    /** 是否含危急值；结果修订次数 */
    criticalFlag?: boolean | null;
    resultRevision?: number | null;
    /** 危急值处置闭环 */
    criticalHandled?: boolean | null;
    criticalHandleBy?: string | null;
    criticalHandleAt?: string | null;
    criticalNotifyAt?: string | null;
}