package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.ClientBalanceLog;

import java.math.BigDecimal;

/**
 * 会员储值流水 Service：所有余额变动点必须调用 record 留痕（迁移对账地基）
 */
public interface IClientBalanceLogService extends IService<ClientBalanceLog> {

    /**
     * 记一条余额流水（调用方需保证与余额变更同事务）
     *
     * @param clientId      客户id
     * @param changeType    变动类型（RECHARGE/PAY/REFUND/BUY_CARD/POINTS_EXCHANGE/MIGRATION_OPEN）
     * @param signedAmount  变动金额（带符号：正加负减）
     * @param balanceBefore 变动前余额
     * @param balanceAfter  变动后余额
     * @param refType       关联单据类型（BILL/COUNT_CARD 等，可空）
     * @param refId         关联单据id（可空）
     * @param operator      操作人（登录态取）
     * @param note          备注（可空）
     */
    void record(Integer clientId, String changeType, BigDecimal signedAmount,
                BigDecimal balanceBefore, BigDecimal balanceAfter,
                String refType, Integer refId, String operator, String note);
}
