package com.mix.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.ClientBalanceLogMapper;
import com.mix.model.entity.ClientBalanceLog;
import com.mix.service.IClientBalanceLogService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 会员储值流水 Service 实现
 */
@Service
public class ClientBalanceLogServiceImpl extends ServiceImpl<ClientBalanceLogMapper, ClientBalanceLog>
        implements IClientBalanceLogService {

    @Override
    public void record(Integer clientId, String changeType, BigDecimal signedAmount,
                       BigDecimal balanceBefore, BigDecimal balanceAfter,
                       String refType, Integer refId, String operator, String note) {
        if (clientId == null || signedAmount == null) return;
        ClientBalanceLog log = new ClientBalanceLog();
        log.setClientId(clientId);
        log.setChangeType(changeType);
        log.setAmount(signedAmount);
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(balanceAfter);
        log.setRefType(refType);
        log.setRefId(refId);
        log.setOperator(operator);
        log.setNote(note);
        this.save(log);
    }
}
