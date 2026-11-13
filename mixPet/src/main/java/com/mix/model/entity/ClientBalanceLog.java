package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员储值流水：余额每次变动一条，前/后余额成对落库，分毫不差可回放。
 * 对账口径：Σ(amount) + 迁移期初 = client.client_balance 当前值。
 */
@Data
@TableName("client_balance_log")
public class ClientBalanceLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;
    private Integer clientId;
    /** RECHARGE充值 / PAY收款扣款 / REFUND退款回充 / BUY_CARD购卡 / POINTS_EXCHANGE积分兑换 / MIGRATION_OPEN迁移期初 */
    private String changeType;
    /** 变动金额（带符号：正加负减） */
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    /** 关联单据类型（BILL/COUNT_CARD 等，可空） */
    private String refType;
    private Integer refId;
    /** 操作人（登录态取，不信任前端传参） */
    private String operator;
    private String note;
    private LocalDateTime createTime;
}
