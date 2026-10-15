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
import com.ship.model.entity.Drug;
import com.ship.model.entity.Prescription;
import com.ship.service.IBillService;
import com.ship.service.IClientService;
import com.ship.service.IDrugService;
import com.ship.service.IMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result pay(Integer billId, Boolean payType) {
        Bill bill = this.getById(billId);
        if (bill == null) return Result.error("账单不存在");
        if (Boolean.TRUE.equals(bill.getPayStatus())) return Result.error("该账单已收款，请勿重复操作");
        // 余额支付：校验并扣减客户余额
        if (payType == null || !payType) { // false=余额（默认）
            Client client = clientService.getById(bill.getClientId());
            if (client == null) return Result.error("客户不存在");
            if (client.getClientBalance() == null
                    || client.getClientBalance().compareTo(bill.getTotalMoney()) < 0)
                return Result.error("客户余额不足（当前 " + client.getClientBalance() + " 元），请改用现金收款或先充值");
            // 用 WHERE 条件保证原子扣减：余额不足时不更新，防并发超扣成负数
            boolean deducted = clientService.lambdaUpdate()
                    .eq(Client::getClientId, client.getClientId())
                    .ge(Client::getClientBalance, bill.getTotalMoney())
                    .setSql("client_balance = client_balance - " + bill.getTotalMoney().toPlainString())
                    .update();
            if (!deducted) return Result.error("客户余额不足，请改用现金收款或先充值");
        }
        // 发药：校验库存并扣减
        List<Prescription> prescriptions = prescriptionMapper.selectByRecord(bill.getRecordId());
        for (Prescription p : prescriptions) {
            Drug drug = drugMapper.selectById(p.getDrugId());
            if (drug != null && (drug.getDrugStock() == null || drug.getDrugStock() < p.getQuantity()))
                return Result.error("「" + p.getDrugName() + "」库存不足（剩余 "
                        + (drug.getDrugStock() == null ? 0 : drug.getDrugStock()) + "），无法发药");
        }
        // 扣减库存加 WHERE drug_stock >= quantity 原子校验，防并发发药超扣；并记出入库台账
        for (Prescription p : prescriptions) {
            Drug drug = drugMapper.selectById(p.getDrugId());
            int before = (drug != null && drug.getDrugStock() != null) ? drug.getDrugStock() : 0;
            int affected = drugMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Drug>()
                            .eq(Drug::getDrugId, p.getDrugId())
                            .ge(Drug::getDrugStock, p.getQuantity())
                            .setSql("drug_stock = drug_stock - " + p.getQuantity()));
            if (affected == 0)
                return Result.error("「" + p.getDrugName() + "」库存不足，无法发药");
            drugService.logStockChange(p.getDrugId(), p.getDrugName(), 2, p.getQuantity(),
                    before, before - p.getQuantity(), "收款发药");
        }
        // 账单标记已付
        this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .set(Bill::getPayStatus, true)
                .set(Bill::getPayType, payType != null && payType)
                .update();
        // 会员积分累积（按等级积分率）
        memberService.earnPoints(bill.getClientId(), bill.getTotalMoney());
        return Result.success("收款成功" + (prescriptions.isEmpty() ? "" : "，处方药品已扣减库存"));
    }

    @Override
    public Result dispense(Integer billId, String operator) {
        Bill bill = this.getById(billId);
        if (bill == null) return Result.error("账单不存在");
        if (!Boolean.TRUE.equals(bill.getPayStatus())) return Result.error("该账单尚未收款，无法发药");
        if (Boolean.TRUE.equals(bill.getDispenseStatus())) return Result.error("该账单已发药，请勿重复核对");
        this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .set(Bill::getDispenseStatus, true)
                .set(Bill::getDispenseBy, operator)
                .set(Bill::getDispenseTime, LocalDateTime.now())
                .update();
        return Result.success("发药核对完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result refund(Integer billId, String operator) {
        Bill bill = this.getById(billId);
        if (bill == null) return Result.error("账单不存在");
        if (!Boolean.TRUE.equals(bill.getPayStatus())) return Result.error("该账单未收款，无需退款");
        if (Boolean.TRUE.equals(bill.getRefundStatus())) return Result.error("该账单已退款，请勿重复操作");
        boolean wasBalance = bill.getPayType() == null || !bill.getPayType();
        // 回滚库存 + 记台账（退款回库）；已作废处方未扣库存，不在 selectByRecord 中，无需回滚
        List<Prescription> prescriptions = prescriptionMapper.selectByRecord(bill.getRecordId());
        for (Prescription p : prescriptions) {
            Drug drug = drugMapper.selectById(p.getDrugId());
            int before = (drug != null && drug.getDrugStock() != null) ? drug.getDrugStock() : 0;
            drugMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Drug>()
                    .eq(Drug::getDrugId, p.getDrugId())
                    .setSql("drug_stock = drug_stock + " + p.getQuantity()));
            drugService.logStockChange(p.getDrugId(), p.getDrugName(), 3, p.getQuantity(),
                    before, before + p.getQuantity(), "退款回库");
        }
        // 余额支付才退还余额（现金收款无余额变动）
        if (wasBalance) {
            Client client = clientService.getById(bill.getClientId());
            if (client != null) {
                clientService.lambdaUpdate()
                        .eq(Client::getClientId, client.getClientId())
                        .setSql("client_balance = client_balance + " + bill.getTotalMoney().toPlainString())
                        .update();
            }
        }
        this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .set(Bill::getRefundStatus, true)
                .set(Bill::getRefundBy, operator)
                .set(Bill::getRefundTime, LocalDateTime.now())
                .update();
        return Result.success("退款成功"
                + (prescriptions.isEmpty() ? "" : "，库存已回滚")
                + (wasBalance ? "，余额已退还" : "（现金收款，请线下退现金）"));
    }
}
