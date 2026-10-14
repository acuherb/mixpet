package com.ship.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 往已有病历追加套餐
 */
@Data
public class RecordPackageVo {
    private Integer recordId;
    private List<Integer> packageIds;
}
