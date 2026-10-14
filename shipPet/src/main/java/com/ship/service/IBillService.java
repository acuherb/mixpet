package com.ship.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.common.Result;
import com.ship.model.dto.BillDto;
import com.ship.model.entity.Bill;

/**
 * 账单 Service
 */
public interface IBillService extends IService<Bill> {
    IPage<BillDto> selectByPage(String seaName, Integer payStatus, int numPage, int pageSize);

    /** 收款：余额支付则扣客户余额，发药扣库存，账单标记已付（事务） */
    Result pay(Integer billId, Boolean payType);

    /** 发药核对：护士确认发药，记录核对人与时间（需先收款） */
    Result dispense(Integer billId, String operator);
}
