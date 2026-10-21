package com.mix.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 辅助诊疗入参上下文（由 RecordFullVo 提取，脱敏后交给 Provider）
 */
@Data
public class AiContext {
    private Integer recordId;
    private String hospitalName;
    private String doctorName;
    private String petName;
    private String species;
    private String breed;
    private String petSex;
    private String petAge;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private BigDecimal weight;
    private BigDecimal temperature;
    private Integer heartRate;
    private Integer respiratoryRate;
    private String bloodPressure;
    private String physicalExam;
    private String diagnosis;
    /** 检验异常项（已过滤 is_abnormal=1） */
    private List<LabAbnormal> labAbnormal;
    /** 影像报告文本 */
    private List<String> imageReports;

    @Data
    public static class LabAbnormal {
        private String itemName;
        private String resultValue;
        private String unit;
        private String refRange;
    }
}
