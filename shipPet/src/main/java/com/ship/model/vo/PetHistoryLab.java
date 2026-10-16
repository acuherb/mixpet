package com.ship.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物病史-近期检验异常项
 */
@Data
@AllArgsConstructor
public class PetHistoryLab {
    private String itemName;
    private String resultValue;
    private String refRange;
    private String itemUnit;
    private LocalDateTime resultTime;
}
