package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.BillMapper;
import com.ship.mapper.DrugMapper;
import com.ship.mapper.PrescriptionMapper;
import com.ship.model.dto.BillDto;
import com.ship.model.entity.Bill;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.Drug;
import com.ship.model.entity.Prescription;
import com.ship.service.IBillService;
import com.ship.service.IClientService;
import com.ship.service.IDrugService;
import com.ship.service.IMemberService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单 Service 实现
 */
@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements IBillService {
    private final IClientService clientService;
    private final DrugMapper drugMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final IDrugService drugService;
    private final IMemberService memberService;

    @Autowired
    public BillServiceImpl(IClientService clientService, DrugMapper drugMapper,
                           PrescriptionMapper prescriptionMapper, IDrugService drugService,
                           IMemberService memberService) {
        this.clientService = clientService;
        this.drugMapper = drugMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.drugService = drugService;
        this.memberService = memberService;
    }

    @Override
    public IPage<BillDto> selectByPage(String seaName, Integer payStatus, int numPage, int pageSize) {
        return baseMapper.selectBillPage(new Page<>(numPage, pageSize), seaName, payStatus);
    }

    /** 折后应付（旧账单无折扣快照时按原价，保证兼容） */
    private BigDecimal payable(Bill bill) {
        return bill.getRealMoney() != null ? bill.getRealMoney() : bill.getTotalMoney();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result pay(Integer billId, Boolean payType) {
        Bill bill = this.getById(billId);
        if (bill == null) return Result.error("账单不存在");
        if (Boolean.TRUE.equals(bill.getPayStatus())) return Result.error("该账单已收款，请勿重复操作");
        // 余额支付：校验并扣减客户余额（按折后应付）
        if (payType == null || !payType) { // false=余额（默认）
            Client client = clientService.getById(bill.getClientId());
            if (client == null) return Result.error("客户不存在");
            BigDecimal due = payable(bill);
            if (client.getClientBalance() == null
                    || client.getClientBalance().compareTo(due) < 0)
                return Result.error("客户余额不足（当前 " + client.getClientBalance() + " 元），请改用现金收款或先充值");
            // 用 WHERE 条件保证原子扣减：余额不足时不更新，防并发超扣成负数
            boolean deducted = clientService.lambdaUpdate()
                    .eq(Client::getClientId, client.getClientId())
                    .ge(Client::getClientBalance, due)
                    .setSql("client_balance = client_balance - " + due.toPlainString())
                    .update();
            if (!deducted) return Result.error("客户余额不足，请改用现金收款或先充值");
        }
        // 账单标记已付（库存扣减移至「发药核对」环节，避免收款即出库）
        // 收银者留痕：后台账号均为医生实体（auth_lv 分级），取当前登录人姓名
        String payBy = null;
        if (UserUtil.getUser() instanceof Doctor operator) payBy = operator.getDoctorName();
        this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .set(Bill::getPayStatus, true)
                .set(Bill::getPayType, payType != null && payType)
                .set(Bill::getPayBy, payBy)
                .update();
        // 会员积分累积（按折后实收 × 等级积分率）
        memberService.earnPoints(bill.getClientId(), payable(bill));
        // 是否有需发药药品（提示走「发药核对」出库）
        long drugCount = prescriptionMapper.selectByRecord(bill.getRecordId()).size();
        return Result.success(drugCount > 0
                ? "收款成功，请到「发药核对」确认并扣减库存"
                : "收款成功");
    }

    /** 当前登录操作人姓名（发药/退款留痕，不信任前端传参——后台账号均为 Doctor 实体） */
    private String currentOperator() {
        return UserUtil.getUser() instanceof Doctor d ? d.getDoctorName() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result dispense(Integer billId, String operator) {
        // 操作人一律取当前登录人，忽略前端传参（防伪造他人名义发药）
        operator = currentOperator();
        Bill bill = this.getById(billId);
        if (bill == null) return Result.error("账单不存在");
        if (!Boolean.TRUE.equals(bill.getPayStatus())) return Result.error("该账单尚未收款，无法发药");
        if (Boolean.TRUE.equals(bill.getDispenseStatus())) return Result.error("该账单已发药，请勿重复核对");
        // 发药核对：此刻才扣减库存（加 WHERE drug_stock >= quantity 原子校验防并发超扣），并记出入库台账
        List<Prescription> prescriptions = prescriptionMapper.selectByRecord(bill.getRecordId());
        for (Prescription p : prescriptions) {
            if (p.getDrugId() == null) continue;
            Drug drug = drugMapper.selectById(p.getDrugId());
            int before = (drug != null && drug.getDrugStock() != null) ? drug.getDrugStock() : 0;
            int affected = drugMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Drug>()
                            .eq(Drug::getDrugId, p.getDrugId())
                            .ge(Drug::getDrugStock, p.getQuantity())
                            .setSql("drug_stock = drug_stock - " + p.getQuantity()));
            if (affected == 0)
                return Result.error("「" + p.getDrugName() + "」库存不足（剩余 " + before + "），无法发药");
            drugService.logStockChange(p.getDrugId(), p.getDrugName(), 2, p.getQuantity(),
                    before, before - p.getQuantity(), "发药核对出库");
        }
        this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .set(Bill::getDispenseStatus, true)
                .set(Bill::getDispenseBy, operator)
                .set(Bill::getDispenseTime, LocalDateTime.now())
                .update();
        return Result.success("发药核对完成" + (prescriptions.isEmpty() ? "" : "，药品库存已扣减"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result refund(Integer billId, String reason) {
        if (reason == null || reason.isBlank()) return Result.error("请填写退款原因");
        // 操作人一律取当前登录人，忽略前端传参（防伪造他人名义退款）
        String operator = currentOperator();
        Bill bill = this.getById(billId);
        if (bill == null) return Result.error("账单不存在");
        if (!Boolean.TRUE.equals(bill.getPayStatus())) return Result.error("该账单未收款，无需退款");
        if (Boolean.TRUE.equals(bill.getRefundStatus())) return Result.error("该账单已退款，请勿重复操作");
        boolean wasBalance = bill.getPayType() == null || !bill.getPayType();
        // 回滚库存 + 记台账（仅「已发药核对」才扣过库存，需回库；已作废/未发药处方未扣，无需回滚）
        if (Boolean.TRUE.equals(bill.getDispenseStatus())) {
            List<Prescription> prescriptions = prescriptionMapper.selectByRecord(bill.getRecordId());
            for (Prescription p : prescriptions) {
                if (p.getDrugId() == null) continue;
                Drug drug = drugMapper.selectById(p.getDrugId());
                int before = (drug != null && drug.getDrugStock() != null) ? drug.getDrugStock() : 0;
                drugMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Drug>()
                        .eq(Drug::getDrugId, p.getDrugId())
                        .setSql("drug_stock = drug_stock + " + p.getQuantity()));
                drugService.logStockChange(p.getDrugId(), p.getDrugName(), 3, p.getQuantity(),
                        before, before + p.getQuantity(), "退款回库");
            }
        }
        // 余额支付才退还余额（现金收款无余额变动）；退还金额=折后实收
        if (wasBalance) {
            Client client = clientService.getById(bill.getClientId());
            if (client != null) {
                clientService.lambdaUpdate()
                        .eq(Client::getClientId, client.getClientId())
                        .setSql("client_balance = client_balance + " + payable(bill).toPlainString())
                        .update();
            }
        }
        // 冲销收款时累积的会员积分（按同等折后实收反算，不足时不扣为负）
        memberService.deductPoints(bill.getClientId(), payable(bill));
        this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .set(Bill::getRefundStatus, true)
                .set(Bill::getRefundBy, operator)
                .set(Bill::getRefundReason, reason)
                .set(Bill::getRefundTime, LocalDateTime.now())
                .update();
        return Result.success("退款成功"
                + (Boolean.TRUE.equals(bill.getDispenseStatus()) ? "，库存已回滚" : "")
                + (wasBalance ? "，余额已退还" : "（现金收款，请线下退现金）"));
    }
}