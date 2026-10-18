package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 兽用处方笺打印补充 + 留痕（农业农村部第734号）
 * 处方正文仍以 prescription/medical_record 为准，本表只存打印时才需要的补充字段
 */
@Data
@TableName("rx_print_supplement")
public class RxPrintSupplement implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long prescriptionId;
    private Long recordId;
    /** 处方笺编号 RX-YYYYMMDD-XXXX（按天自增，作废占号不回收） */
    private String printNo;
    /** 毛色（优先 pet.pet_color，取不到补录，不回写 pet） */
    private String hairColor;
    /** 动物种类（犬/猫/兔…，pet_variety 是品种非种类，缺失补录） */
    private String species;
    /** 休药期（宠物处方固定"—"；食品动物从药品取，取不到补录） */
    private String withdrawalPeriod;
    /** 发药人（优先 bill.dispense_by 姓名） */
    private String dispenser;
    /** 联次份数 1=仅第三联(畜主留存) 3=三联全打 */
    private Integer copies;
    /** VALID 有效 / VOID 已作废 */
    private String status;
    private Long printedBy;
    private LocalDateTime printedAt;
    private Long voidBy;
    private LocalDateTime voidAt;
    private String voidReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
