/**
 * 批量入库单行与结果
 */
export interface InboundLine {
    drugId: number;
    quantity: number;
    batchNo?: string;
    expireDate?: string;
    note?: string;
}

export interface InboundBatchVo {
    /** 成功条数 */
    success: number;
    /** 失败条数 */
    failed: number;
    /** 失败原因（含行号，如「第3行：药品不存在」） */
    errors: string[];
    /** 失败行号（从1开始，按提交顺序） */
    failedRows: number[];
}
