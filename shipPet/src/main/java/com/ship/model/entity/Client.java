package com.ship.model.entity;

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
 * 用户表
 * </p>
 *
 * @author MixJade
 * @since 2023-08-22
 */
public class Client implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户id */
    @TableId(value = "client_id", type = IdType.AUTO)
    private Integer clientId;

    /** 用户账号 */
    private String clientUsername;

    /** 用户密码 */
    private String clientPassword;

    /** 用户姓名 */
    private String clientName;

    /** 用户性别 */
    private Boolean clientGender;

    /** 用户电话 */
    private String clientTel;

    /** 备用电话 */
    private String clientTel2;

    /** 用户生日 */
    private LocalDate clientBirthday;

    /** 生日是否估算 0精确 1估算 */
    private Boolean clientBirthdayEstimated;

    /** 邮箱 */
    private String clientEmail;

    /** 是否改过密码 0未改 1已改 */
    private Boolean clientPwdChanged;

    /** 地址 */
    private String clientAddress;

    /** 身份证 */
    private String clientIdCard;

    /** 会员等级 0普通 1银 2金 3钻石 */
    private Integer clientLevel;

    /** 积分 */
    private Integer clientPoints;

    /** 余额 */
    private BigDecimal clientBalance;

    /** 客户来源 */
    private String clientSource;

    /** 是否黑名单 0正常 1拉黑 */
    private Boolean clientBlacklist;

    /** 用户简介 */
    private String clientInfo;

    /** 用户照片名称 */
    private String clientPhoto;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除，默认0，填充删除日期 */
    private Long isDel;

    /** 是否启用 0停用 1正常 */
    private Boolean active;

    /** 审核状态：0待审核 1通过 2拒绝 */
    private Integer auditStatus;
    /** 注册审核拒绝原因（登录时提示顾客） */
    private String auditReason;

    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }

    public String getClientUsername() { return clientUsername; }
    public void setClientUsername(String clientUsername) { this.clientUsername = clientUsername; }

    public String getClientPassword() { return clientPassword; }
    public void setClientPassword(String clientPassword) { this.clientPassword = clientPassword; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public Boolean getClientGender() { return clientGender; }
    public void setClientGender(Boolean clientGender) { this.clientGender = clientGender; }

    public String getClientTel() { return clientTel; }
    public void setClientTel(String clientTel) { this.clientTel = clientTel; }

    public String getClientTel2() { return clientTel2; }
    public void setClientTel2(String clientTel2) { this.clientTel2 = clientTel2; }

    public LocalDate getClientBirthday() { return clientBirthday; }
    public void setClientBirthday(LocalDate clientBirthday) { this.clientBirthday = clientBirthday; }

    public Boolean getClientBirthdayEstimated() { return clientBirthdayEstimated; }
    public void setClientBirthdayEstimated(Boolean clientBirthdayEstimated) { this.clientBirthdayEstimated = clientBirthdayEstimated; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public Boolean getClientPwdChanged() { return clientPwdChanged; }
    public void setClientPwdChanged(Boolean clientPwdChanged) { this.clientPwdChanged = clientPwdChanged; }

    public String getClientAddress() { return clientAddress; }
    public void setClientAddress(String clientAddress) { this.clientAddress = clientAddress; }

    public String getClientIdCard() { return clientIdCard; }
    public void setClientIdCard(String clientIdCard) { this.clientIdCard = clientIdCard; }

    public Integer getClientLevel() { return clientLevel; }
    public void setClientLevel(Integer clientLevel) { this.clientLevel = clientLevel; }

    public Integer getClientPoints() { return clientPoints; }
    public void setClientPoints(Integer clientPoints) { this.clientPoints = clientPoints; }

    public BigDecimal getClientBalance() { return clientBalance; }
    public void setClientBalance(BigDecimal clientBalance) { this.clientBalance = clientBalance; }

    public String getClientSource() { return clientSource; }
    public void setClientSource(String clientSource) { this.clientSource = clientSource; }

    public Boolean getClientBlacklist() { return clientBlacklist; }
    public void setClientBlacklist(Boolean clientBlacklist) { this.clientBlacklist = clientBlacklist; }

    public String getClientInfo() { return clientInfo; }
    public void setClientInfo(String clientInfo) { this.clientInfo = clientInfo; }

    public String getClientPhoto() { return clientPhoto; }
    public void setClientPhoto(String clientPhoto) { this.clientPhoto = clientPhoto; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Long getIsDel() { return isDel; }
    public void setIsDel(Long isDel) { this.isDel = isDel; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }

    public String getAuditReason() { return auditReason; }
    public void setAuditReason(String auditReason) { this.auditReason = auditReason; }

    @Override
    public String toString() {
        return "Client{" +
                "clientId=" + clientId +
                ", clientUsername='" + clientUsername + '\'' +
                ", clientName='" + clientName + '\'' +
                ", clientGender=" + clientGender +
                ", clientTel='" + clientTel + '\'' +
                ", clientTel2='" + clientTel2 + '\'' +
                ", clientBirthday=" + clientBirthday +
                ", clientAddress='" + clientAddress + '\'' +
                ", clientIdCard='" + clientIdCard + '\'' +
                ", clientLevel=" + clientLevel +
                ", clientPoints=" + clientPoints +
                ", clientBalance=" + clientBalance +
                ", clientInfo='" + clientInfo + '\'' +
                ", clientPhoto='" + clientPhoto + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", isDel=" + isDel +
                ", auditStatus=" + auditStatus +
                ", auditReason=" + auditReason +
                '}';
    }
}
