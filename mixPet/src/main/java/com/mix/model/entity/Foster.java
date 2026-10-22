package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.Serial;

/**
 * <p>
 * 寄养表
 * </p>
 *
 * @author MixJade
 * @since 2023-08-22
 */
public class Foster implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 寄养表的id
     */
    @TableId(value = "foster_id", type = IdType.AUTO)
    private Integer fosterId;

    /**
     * 订单编号
     */
    private String fosterCode;

    /**
     * 寄养宠物的id
     */
    private Integer petId;

    /**
     * 寄养人id
     */
    private Integer clientId;

    /**
     * 寄养到期时间（预计接回日期）
     */
    private LocalDate fosterTerm;

    /**
     * 入托日期
     */
    private LocalDate inDate;

    /**
     * 实际接回日期（接回结算时写）
     */
    private LocalDate backDate;

    /**
     * 寄养押金（登记时预估；接回时按 天数×(笼位+清洁+餐费) 结算）
     */
    private Integer fosterMoney;

    /**
     * 笼位费/天
     */
    private java.math.BigDecimal dailyFee;

    /**
     * 清洁费/天
     */
    private java.math.BigDecimal cleanFee;

    /**
     * 餐费/天
     */
    private java.math.BigDecimal mealFee;

    /**
     * 状态：0待入托 1寄养中 2已接回
     */
    private Integer status;

    /**
     * 订单备注
     */
    private String fosterInfo;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除，默认0，填充删除日期
     */
    private Long isDel;

    public Integer getFosterId() {
        return fosterId;
    }

    public void setFosterId(Integer fosterId) {
        this.fosterId = fosterId;
    }

    public String getFosterCode() {
        return fosterCode;
    }

    public void setFosterCode(String fosterCode) {
        this.fosterCode = fosterCode;
    }

    public Integer getPetId() {
        return petId;
    }

    public void setPetId(Integer petId) {
        this.petId = petId;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public LocalDate getFosterTerm() {
        return fosterTerm;
    }

    public void setFosterTerm(LocalDate fosterTerm) {
        this.fosterTerm = fosterTerm;
    }

    public LocalDate getInDate() {
        return inDate;
    }

    public void setInDate(LocalDate inDate) {
        this.inDate = inDate;
    }

    public LocalDate getBackDate() {
        return backDate;
    }

    public void setBackDate(LocalDate backDate) {
        this.backDate = backDate;
    }

    public Integer getFosterMoney() {
        return fosterMoney;
    }

    public void setFosterMoney(Integer fosterMoney) {
        this.fosterMoney = fosterMoney;
    }

    public java.math.BigDecimal getDailyFee() {
        return dailyFee;
    }

    public void setDailyFee(java.math.BigDecimal dailyFee) {
        this.dailyFee = dailyFee;
    }

    public java.math.BigDecimal getCleanFee() {
        return cleanFee;
    }

    public void setCleanFee(java.math.BigDecimal cleanFee) {
        this.cleanFee = cleanFee;
    }

    public java.math.BigDecimal getMealFee() {
        return mealFee;
    }

    public void setMealFee(java.math.BigDecimal mealFee) {
        this.mealFee = mealFee;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getFosterInfo() {
        return fosterInfo;
    }

    public void setFosterInfo(String fosterInfo) {
        this.fosterInfo = fosterInfo;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getIsDel() {
        return isDel;
    }

    public void setIsDel(Long isDel) {
        this.isDel = isDel;
    }

    @Override
    public String toString() {
        return "Foster{" +
                "fosterId = " + fosterId +
                ", fosterCode = " + fosterCode +
                ", petId = " + petId +
                ", clientId = " + clientId +
                ", fosterTerm = " + fosterTerm +
                ", inDate = " + inDate +
                ", backDate = " + backDate +
                ", fosterMoney = " + fosterMoney +
                ", dailyFee = " + dailyFee +
                ", cleanFee = " + cleanFee +
                ", mealFee = " + mealFee +
                ", status = " + status +
                ", fosterInfo = " + fosterInfo +
                ", createTime = " + createTime +
                ", updateTime = " + updateTime +
                ", isDel = " + isDel +
                "}";
    }
}
