package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.dto.BillDto;
import com.ship.model.entity.Bill;
import com.ship.model.entity.Client;
import com.ship.security.model.RoleConst;
import com.ship.service.IBillService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 顾客查自己的账单（结账提醒用：待缴费状态展示，实际收款仍需到前台）
     * 仅返回本人账单；不可操作（无写接口）
     */
    @GetMapping("/self")
    public List<Bill> getSelf() {
        if (UserUtil.getUser() instanceof Client client) {
            return billService.lambdaQuery()
                    .eq(Bill::getClientId, client.getClientId())
                    .eq(Bill::getIsDel, 0)
                    .orderByDesc(Bill::getBillId)
                    .list();
        }
        return List.of();
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

    /** 发药核对：护士确认发药（需先收款） */
    @PostMapping("/dispense/{billId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result dispense(@PathVariable Integer billId, String operator) {
        return billService.dispense(billId, operator);
    }

    /** 退款冲正：退还余额、回滚库存并记台账 */
    @PutMapping("/refund/{billId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result refund(@PathVariable Integer billId, String operator) {
        return billService.refund(billId, operator);
    }
}
