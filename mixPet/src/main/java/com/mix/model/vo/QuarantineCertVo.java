package com.mix.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 检疫单视图：开具草稿 / 已开单据 / 打印取数共用。
 * 版式参考 PetSystem ReportPris_jianyi.mrt（A5 横向）。
 */
@Data
public class QuarantineCertVo {
    /** 单号（草稿为 null，保存后生成 JY-YYYYMMDD-XXXX） */
    private String certNo;
    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;

    private String ownerName;
    private String ownerTel;
    private String petName;
    /** 动物种类（犬/猫…） */
    private String species;
    private String breed;
    private String sex;
    /** 年龄描述，如 2岁3月 */
    private String ageDesc;
    private BigDecimal weight;

    /** 文字记录 / 检疫结论 */
    private String content;
    /** 检疫清单（由 itemsJson 解析） */
    private List<QuarantineItemVo> items;

    private String doctorName;
    private LocalDateTime createTime;

    /** 当前病历是否已有检疫单（编辑提示用） */
    private Boolean existed;
}
