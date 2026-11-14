package com.mix.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 新增一条寄养照护记录：文字 + 已上传好的照片文件名列表 */
public record FosterLogVo(
        Integer fosterId,
        LocalDate logDate,
        String mentalState,
        String appetite,
        String defecation,
        BigDecimal weight,
        String note,
        List<String> photos
) {
}
