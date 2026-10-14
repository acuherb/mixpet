/**
 * 医生开单请求
 */
export interface LabOrderVo {
    recordId: number;
    orderNote: string;
    testIds: number[];
}

/**
 * 化验结果录入：单条明细
 */
export interface LabResultItemVo {
    itemId: number;
    resultValue: string;
    isAbnormal: boolean;
}

/**
 * 化验结果录入请求
 */
export interface LabResultVo {
    orderId: number;
    resultNote: string;
    /** 报告文件(图片/PDF)路径，可选 */
    reportFile?: string;
    items: LabResultItemVo[];
}