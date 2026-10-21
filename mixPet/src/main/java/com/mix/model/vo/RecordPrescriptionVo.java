package com.mix.model.vo;

import com.mix.model.entity.Prescription;
import lombok.Data;

import java.util.List;

/**
 * 往已有病历追加处方
 */
@Data
public class RecordPrescriptionVo {
    private Integer recordId;
    private List<Prescription> prescriptions;
}
