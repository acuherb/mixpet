package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.dto.BillDto;
import com.ship.security.model.RoleConst;
import com.ship.service.IBillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

/**
 * 收费管理：账单列表与收款
 */
@RestController
@RequestMapping("/bill")
public class BillController {
    private final IBillService billService;

    @Autowired
    public BillController(IBillService billService) {
        this.billService = billService;
    }

    /**
     * 收费列表分页
     *
     * @param payStatus 0未付 1已付，-1 或不传查全部
     */
    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<BillDto> getPage(int numPage, int pageSize, String seaName, Integer payStatus) {
        return billService.selectByPage(seaName, payStatus == null ? -1 : payStatus, numPage, pageSize);
    }

    /**
     * 收款：余额支付扣客户余额并扣药品库存发药，现金只发药
     *
     * @param payType false/0=余额（默认），true/1=现金
     */
    @PutMapping("/pay/{billId}")
    @Secured(RoleConst.NURSE)
    public Result pay(@PathVariable Integer billId, Boolean payType) {
        return billService.pay(billId, payType != null && payType);
    }
}
