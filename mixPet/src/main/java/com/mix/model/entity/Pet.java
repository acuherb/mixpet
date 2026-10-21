package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.Serial;

/**
 * <p>
 * 宠物信息表，外键用户表
 * </p>
 *
 * @author MixJade
 * @since 2023-08-22
 */
public class Pet implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 宠物的id */
    @TableId(value = "pet_id", type = IdType.AUTO)
    private Integer petId;

    /** 宠物姓名 */
    private String petName;

    /** 宠物的品种 */
    private String petVariety;

    /** 宠物性别，0母1公 */
    private Boolean petSex;

    /** 宠物生日 */
    private LocalDate petBirthday;

    /** 生日是否估算 0精确 1估算 */
    private Boolean petBirthdayEstimated;

    /** 宠物状态 */
    private String petStatus;

    /** 宠物动态 */
    private String petInfo;

    /** 毛色 */
    private String petColor;

    /** 体重(kg) */
    private BigDecimal petWeight;

    /** 绝育 0否 1是 */
    private Boolean petSterilization;

    /** 芯片号 */
    private String petChip;

    /** 过敏史 */
    private String petAllergy;

    /** 既往病史 */
    private String petHistory;

    /** 是否在世 1是 0否 */
    private Boolean petAlive;

    /** 备注 */
    private String petNote;

    /** 宠物主人id */
    private Integer clientId;

    /** 宠物来源，0后台录入 1顾客自建 */
    private Integer petSource;

    public Integer getPetSource() { return petSource; }
    public void setPetSource(Integer petSource) { this.petSource = petSource; }

    /** 宠物照片名称 */
    private String petPhoto;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除，默认0，填充删除日期 */
    private Long isDel;

    /** 是否启用 0停用 1正常 */
    private Boolean active;

    public Integer getPetId() { return petId; }
    public void setPetId(Integer petId) { this.petId = petId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getPetVariety() { return petVariety; }
    public void setPetVariety(String petVariety) { this.petVariety = petVariety; }

    public Boolean getPetSex() { return petSex; }
    public void setPetSex(Boolean petSex) { this.petSex = petSex; }

    public LocalDate getPetBirthday() { return petBirthday; }
    public void setPetBirthday(LocalDate petBirthday) { this.petBirthday = petBirthday; }

    public Boolean getPetBirthdayEstimated() { return petBirthdayEstimated; }
    public void setPetBirthdayEstimated(Boolean petBirthdayEstimated) { this.petBirthdayEstimated = petBirthdayEstimated; }

    public String getPetStatus() { return petStatus; }
    public void setPetStatus(String petStatus) { this.petStatus = petStatus; }

    public String getPetInfo() { return petInfo; }
    public void setPetInfo(String petInfo) { this.petInfo = petInfo; }

    public String getPetColor() { return petColor; }
    public void setPetColor(String petColor) { this.petColor = petColor; }

    public BigDecimal getPetWeight() { return petWeight; }
    public void setPetWeight(BigDecimal petWeight) { this.petWeight = petWeight; }

    public Boolean getPetSterilization() { return petSterilization; }
    public void setPetSterilization(Boolean petSterilization) { this.petSterilization = petSterilization; }

    public String getPetChip() { return petChip; }
    public void setPetChip(String petChip) { this.petChip = petChip; }

    public String getPetAllergy() { return petAllergy; }
    public void setPetAllergy(String petAllergy) { this.petAllergy = petAllergy; }

    public String getPetHistory() { return petHistory; }
    public void setPetHistory(String petHistory) { this.petHistory = petHistory; }

    public Boolean getPetAlive() { return petAlive; }
    public void setPetAlive(Boolean petAlive) { this.petAlive = petAlive; }

    public String getPetNote() { return petNote; }
    public void setPetNote(String petNote) { this.petNote = petNote; }

    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }

    public String getPetPhoto() { return petPhoto; }
    public void setPetPhoto(String petPhoto) { this.petPhoto = petPhoto; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Long getIsDel() { return isDel; }
    public void setIsDel(Long isDel) { this.isDel = isDel; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "Pet{" +
                "petId=" + petId +
                ", petName='" + petName + '\'' +
                ", petVariety='" + petVariety + '\'' +
                ", petSex=" + petSex +
                ", petBirthday=" + petBirthday +
                ", petBirthdayEstimated=" + petBirthdayEstimated +
                ", petStatus='" + petStatus + '\'' +
                ", petInfo='" + petInfo + '\'' +
                ", petColor='" + petColor + '\'' +
                ", petWeight=" + petWeight +
                ", petSterilization=" + petSterilization +
                ", petChip='" + petChip + '\'' +
                ", petAllergy='" + petAllergy + '\'' +
                ", petHistory='" + petHistory + '\'' +
                ", petAlive=" + petAlive +
                ", clientId=" + clientId +
                ", petPhoto='" + petPhoto + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", isDel=" + isDel +
                '}';
    }
}
