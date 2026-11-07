package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.BillDto;
import com.mix.model.entity.Bill;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账单 Service
 */
public interface IBillService extends IService<Bill> {
    IPage<BillDto> selectByPage(String seaName, Integer payStatus, int numPage, int pageSize);

    /** 收款：余额支付则扣客户余额，发药扣库存，账单标记已付（事务） */
    Result pay(Integer billId, Boolean payType);

    /** 发药核对：护士确认发药，记录核对人与时间（需先收款） */
    Result dispense(Integer billId, String operator);

    /** 退款冲正：退还余额(余额支付时)、回滚已扣库存并记台账，标记已退（需已收款且未退） */
    /**
     * 退款冲正：退还余额、回滚库存并记台账；操作人取登录态，原因必填。
     * prescriptionIds 为空=整单退；传了=部分退（只退选中医嘱行，行标记 is_cancel 防重复退）
     */
    Result refund(Integer billId, String reason, List<Integer> prescriptionIds);

    /**
     * 统一计费入口：寄养、领养、住院等非接诊费用并入统一账单。
     * 优先累加进同一客户/宠物的未付账单，否则新建（含折扣快照）；调用方保证事务。
     *
     * @param feeColumn 费用分项列名（白名单，如 fee_package）
     * @return 账单 id
     */
    Integer addCharge(Integer clientId, Integer petId, Integer recordId,
                      BigDecimal amount, String feeColumn);
}
