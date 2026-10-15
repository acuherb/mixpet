package com.ship.model.vo;

import com.ship.model.dto.RecordDto;
import com.ship.model.entity.HospitalRecord;
import com.ship.model.entity.ImmuneRecord;
import com.ship.model.entity.Pet;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 一页式宠物健康档案：聚合宠物信息 + 就诊史 + 免疫 + 住院 + 体重曲线
 */
@Data
public class PetHealth {
    private Pet pet;
    /** 就诊史（近50条） */
    private List<RecordDto> visits;
    /** 免疫记录 */
    private List<ImmuneRecord> immune;
    /** 住院记录 */
    private List<HospitalRecord> hospital;
    /** 体重曲线（有记录体重的就诊） */
    private List<WeightPoint> weightTrend;

    private int visitCount;
    private String lastVisitDate;
    private String lastDiagnosis;
    private String nextVaccineDate;

    @Data
    public static class WeightPoint {
        private LocalDate date;
        private BigDecimal weight;

        public WeightPoint(LocalDate date, BigDecimal weight) {
            this.date = date;
            this.weight = weight;
        }
    }
}
