package com.mix.model.vo;

import com.mix.model.dto.RecordDto;
import com.mix.model.entity.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 宠物就诊档案（全量聚合）
 * <p>
 * 与 {@link PetHealth}（一页式健康档案，近 50 次就诊）的区别：
 * 这里是「完整时间线」——病历不限条数，并带处方、手术、化验、影像、住院、免疫，
 * 前端按时间倒序合并成一棵树，供医生看病史 / 顾客看就诊记录 / 打印留存。
 */
@Data
public class PetArchive {
    private Pet pet;
    private Client client;
    /** 全部病历（按时间倒序） */
    private List<RecordDto> visits;
    /** recordId → 该次处方明细 */
    private Map<Integer, List<Prescription>> prescriptions;
    private List<ImmuneRecord> immune;
    private List<HospitalRecord> hospital;
    private List<Surgery> surgery;
    private List<LabOrder> lab;
    private List<ImageOrder> image;
    private Stat stat;

    @Data
    public static class Stat {
        private int visitCount;
        private String firstVisit;
        private String lastVisit;
        private String lastDiagnosis;
        /** 未来最近一次免疫/驱虫到期日 */
        private String nextImmune;
        /** 最近一次体重（取病历记录值） */
        private BigDecimal weight;
        /** 累计消费（收费单口径，老数据未迁则为 null） */
        private BigDecimal totalFee;
    }
}
