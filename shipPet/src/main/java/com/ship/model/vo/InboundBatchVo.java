package com.ship.model.vo;

import java.util.List;

/**
 * 批量入库结果：逐条独立提交（部分成功语义）
 *
 * @param success    成功条数
 * @param failed     失败条数
 * @param errors     失败原因（含行号，如「第3行：药品不存在」）
 * @param failedRows 失败行号（从1开始，供前端保留失败行、移除成功行）
 */
public record InboundBatchVo(int success, int failed, List<String> errors, List<Integer> failedRows) {
}
