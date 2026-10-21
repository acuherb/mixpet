package com.mix.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物病史-近期用药
 */
@Data
@AllArgsConstructor
public class PetHistoryRx {
    private String drugName;
    private String usageInfo;
    private LocalDateTime createTime;
}
