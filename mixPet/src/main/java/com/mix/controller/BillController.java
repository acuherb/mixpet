package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.annotation.AuditLog;
import com.mix.annotation.FlowChanged;
import com.mix.common.Result;
import com.mix.model.dto.BillDto;
import com.mix.model.entity.Bill;
import com.mix.model.entity.Client;
import com.mix.security.model.RoleConst;
import com.mix.service.IBillService;
import com.mix.util.UserUtil;
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
    @Secured(RoleConst.NURSE)
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
    @AuditLog(module = "bill", action = "pay", target = "账单收款")
    @FlowChanged(stage = "pay", text = "账单已收款，流转至随访")
    public Result pay(@PathVariable Integer billId, Boolean payType) {
        return billService.pay(billId, payType != null && payType);
    }

    /** 发药核对：护士确认发药（需先收款） */
    @PostMapping("/dispense/{billId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    @AuditLog(module = "bill", action = "dispense", target = "发药核对")
    public Result dispense(@PathVariable Integer billId, String operator) {
        return billService.dispense(billId, operator);
    }

    /** 退款冲正：退还余额、回滚库存并记台账；操作人取登录态，原因必填。
     *  prescriptionIds 为空=整单退（兼容旧行为）；传了=部分退（只退选中医嘱行，
     *  行标记 is_cancel 防重复退，小票/发药自动排除；覆盖全部未退行时等价整单退） */
    @PutMapping("/refund/{billId}")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    @AuditLog(module = "bill", action = "refund", target = "退款冲正")
    public Result refund(@PathVariable Integer billId,
                         @RequestBody java.util.Map<String, Object> body) {
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        @SuppressWarnings("unchecked")
        List<Integer> ids = body.get("prescriptionIds") instanceof List<?> l
                ? l.stream().map(o -> Integer.valueOf(String.valueOf(o))).toList()
                : null;
        return billService.refund(billId, reason, ids);
    }
}
