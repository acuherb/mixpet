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
    /** 结构化报告：所见/诊断分离 + DICOM 元数据 + 原图附件（JSON 数组字符串） */
    findings?: string | null;
    impression?: string | null;
    dicomUid?: string | null;
    dicomSeries?: string | null;
    imageFiles?: string | null;
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