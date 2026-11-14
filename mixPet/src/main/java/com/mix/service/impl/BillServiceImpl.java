package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.BizException;
import com.mix.common.Result;
import com.mix.mapper.BillMapper;
import com.mix.mapper.DrugMapper;
import com.mix.mapper.PrescriptionMapper;
import com.mix.model.dto.BillDto;
import com.mix.model.entity.Bill;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Drug;
import com.mix.model.entity.Prescription;
import com.mix.service.IBillService;
import com.mix.service.IClientBalanceLogService;
import com.mix.service.IClientService;
import com.mix.service.IDrugService;
import com.mix.service.IMemberService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单 Service 实现。
 * 资金相关写操作统一「先条件抢占状态（CAS）→ 再改钱/库存」，失败抛 BizException 触发事务回滚，
 * 避免双点/重试造成重复扣款、重复发药、重复退款与半截数据。
 */
@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements IBillService {
    private final IClientService clientService;
    private final DrugMapper drugMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final IDrugService drugService;
    private final IMemberService memberService;
    private final IClientBalanceLogService balanceLogService;

    @Autowired
    public BillServiceImpl(IClientService clientService, DrugMapper drugMapper,
                           PrescriptionMapper prescriptionMapper, IDrugService drugService,
                           IMemberService memberService, IClientBalanceLogService balanceLogService) {
        this.clientService = clientService;
        this.drugMapper = drugMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.drugService = drugService;
        this.memberService = memberService;
        this.balanceLogService = balanceLogService;
    }

    @Override
    public IPage<BillDto> selectByPage(String seaName, Integer payStatus, int numPage, int pageSize) {
        return baseMapper.selectBillPage(new Page<>(numPage, pageSize), seaName, payStatus);
    }

    /** 费用分项列名白名单：禁止把前端字符串直接拼进 SQL */
    private static final java.util.Set<String> FEE_COLUMNS = java.util.Set.of(
            "fee_register", "fee_treat", "fee_drug", "fee_lab", "fee_hospital",
            "fee_image", "fee_package", "fee_surgery", "fee_immune", "fee_foster");

    @Override
    public Integer addCharge(Integer clientId, Integer petId, Integer recordId,
                             BigDecimal amount, String feeColumn) {
        return doAddCharge(clientId, petId, recordId, amount, feeColumn, null);
    }

    @Override
    public Integer addChargeForFoster(Integer clientId, Integer petId,
                                      BigDecimal amount, Integer fosterId) {
        return doAddCharge(clientId, petId, null, amount, "fee_foster", fosterId);
    }

    private Integer doAddCharge(Integer clientId, Integer petId, Integer recordId,
                                BigDecimal amount, String feeColumn, Integer fosterId) {
        if (clientId == null || petId == null) throw new BizException("缺少客户或宠物信息");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BizException("计费金额必须大于0");
        if (!FEE_COLUMNS.contains(feeColumn)) throw new BizException("非法费用分项");

        // 未付账单定位：无病历号（寄养/领养）时必须用 isNull，否则 record_id = null 恒 false 会永远新建
        List<Bill> candidates = this.lambdaQuery()
                .eq(Bill::getClientId, clientId)
                .eq(Bill::getPetId, petId)
                .eq(Bill::getPayStatus, false)
                .eq(recordId != null, Bill::getRecordId, recordId)
                .isNull(recordId == null, Bill::getRecordId)
                .last("limit 1")
                .list();
        String amt = amount.toPlainString();
        if (!candidates.isEmpty()) {
            Bill unpaid = candidates.get(0);
            this.lambdaUpdate()
                    .eq(Bill::getBillId, unpaid.getBillId())
                    .eq(Bill::getPayStatus, false)
                    .setSql(feeColumn + " = COALESCE(" + feeColumn + ", 0) + " + amt)
                    .setSql("total_money = COALESCE(total_money, 0) + " + amt)
                    .setSql(fosterId != null, "foster_id = COALESCE(foster_id, " + fosterId + ")")
                    .update();
            return unpaid.getBillId();
        }

        Bill bill = new Bill();
        bill.setClientId(clientId);
        bill.setPetId(petId);
        bill.setRecordId(recordId);
        bill.setFeeRegister(BigDecimal.ZERO);
        bill.setFeeTreat(BigDecimal.ZERO);
        bill.setFeeDrug(BigDecimal.ZERO);
        bill.setFeeLab(BigDecimal.ZERO);
        bill.setFeeHospital(BigDecimal.ZERO);
        bill.setFeeImage(BigDecimal.ZERO);
        bill.setFeePackage(BigDecimal.ZERO);
        bill.setFeeSurgery(BigDecimal.ZERO);
        bill.setFeeImmune(BigDecimal.ZERO);
        bill.setFeeFoster(BigDecimal.ZERO);
        bill.setFosterId(fosterId);
        bill.setTotalMoney(amount);
        bill.setDiscount(memberService.discountOf(clientId));
        bill.setPayStatus(false);
        bill.setPayType(false);
        switch (feeColumn) {
            case "fee_register" -> bill.setFeeRegister(amount);
            case "fee_treat" -> bill.setFeeTreat(amount);
            case "fee_drug" -> bill.setFeeDrug(amount);
            case "fee_lab" -> bill.setFeeLab(amount);
            case "fee_hospital" -> bill.setFeeHospital(amount);
            case "fee_image" -> bill.setFeeImage(amount);
            case "fee_package" -> bill.setFeePackage(amount);
            case "fee_surgery" -> bill.setFeeSurgery(amount);
            case "fee_immune" -> bill.setFeeImmune(amount);
            case "fee_foster" -> bill.setFeeFoster(amount);
            default -> throw new BizException("非法费用分项");
        }
        this.save(bill);
        return bill.getBillId();
    }

    /** 无病历号的账单（寄养/领养）没有处方行 */
    private List<Prescription> prescriptionsOf(Bill bill) {
        return bill.getRecordId() == null
                ? List.of()
                : prescriptionMapper.selectByRecord(bill.getRecordId());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 折后应付（旧账单无折扣快照时按原价，保证兼容） */
    private BigDecimal payable(Bill bill) {
        return bill.getRealMoney() != null ? bill.getRealMoney() : bill.getTotalMoney();
    }

    /** 当前登录操作人姓名（不信任前端传参） */
    private String currentOperator() {
        return UserUtil.getUser() instanceof Doctor d ? d.getDoctorName() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result pay(Integer billId, Boolean payType) {
        Bill bill = this.getById(billId);
        if (bill == null) throw new BizException("账单不存在");
        if (Boolean.TRUE.equals(bill.getPayStatus())) throw new BizException("该账单已收款，请勿重复操作");
        BigDecimal due = payable(bill);

        // CAS 抢占：两个前台同时点收款时只有一个能继续（WHERE pay_status = 0）
        boolean claimed = this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .eq(Bill::getPayStatus, false)
                .set(Bill::getPayStatus, true)
                .set(Bill::getPayType, payType != null && payType)
                .set(Bill::getPayBy, currentOperator())
                .set(Bill::getPayTime, LocalDateTime.now())
                .update();
        if (!claimed) throw new BizException("该账单已收款，请勿重复操作");

        // 余额支付：条件原子扣减，防并发超扣成负数
        if (payType == null || !payType) {
            Client client = clientService.getById(bill.getClientId());
            if (client == null) throw new BizException("客户不存在");
            if (client.getClientBalance() == null || client.getClientBalance().compareTo(due) < 0)
                throw new BizException("客户余额不足（当前 " + client.getClientBalance() + " 元），请改用现金收款或先充值");
            boolean deducted = clientService.lambdaUpdate()
                    .eq(Client::getClientId, client.getClientId())
                    .ge(Client::getClientBalance, due)
                    .setSql("client_balance = client_balance - " + due.toPlainString())
                    .update();
            if (!deducted) throw new BizException("客户余额不足，请改用现金收款或先充值");
            BigDecimal balanceBefore = client.getClientBalance() == null ? BigDecimal.ZERO : client.getClientBalance();
            balanceLogService.record(client.getClientId(), "PAY", due.negate(), balanceBefore, balanceBefore.subtract(due),
                    "BILL", billId, currentOperator(), "账单收款扣款");
        }
        memberService.earnPoints(bill.getClientId(), due);
        long drugCount = prescriptionsOf(bill).size();
        return Result.success(drugCount > 0
                ? "收款成功，请到「发药核对」确认并扣减库存"
                : "收款成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result payByWeChat(Integer billId, String wxTransactionId) {
        Bill bill = this.getById(billId);
        if (bill == null) throw new BizException("账单不存在");
        BigDecimal due = bill.getRealMoney() != null ? bill.getRealMoney() : bill.getTotalMoney();
        // CAS 抢占：微信回调可能重复推送，幂等（已收款直接成功返回，不重复加积分）
        boolean claimed = this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .eq(Bill::getPayStatus, false)
                .set(Bill::getPayStatus, true)
                .set(Bill::getPayType, true) // 现金口径（不动客户余额）
                .set(Bill::getPayBy, "微信支付")
                .set(Bill::getPayTime, LocalDateTime.now())
                .set(Bill::getWxTransactionId, wxTransactionId)
                .update();
        if (!claimed) return Result.success("该账单已收款（重复回调忽略）");
        memberService.earnPoints(bill.getClientId(), due);
        return Result.success("微信收款成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result dispense(Integer billId, String operator) {
        operator = currentOperator();
        Bill bill = this.getById(billId);
        if (bill == null) throw new BizException("账单不存在");
        if (!Boolean.TRUE.equals(bill.getPayStatus())) throw new BizException("该账单尚未收款，无法发药");
        if (Boolean.TRUE.equals(bill.getDispenseStatus())) throw new BizException("该账单已发药，请勿重复核对");

        // 先抢占发药状态，再逐项扣库存；任一项失败抛异常，整体回滚（不会留下部分出库）
        boolean claimed = this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .eq(Bill::getDispenseStatus, false)
                .set(Bill::getDispenseStatus, true)
                .set(Bill::getDispenseBy, operator)
                .set(Bill::getDispenseTime, LocalDateTime.now())
                .update();
        if (!claimed) throw new BizException("该账单已发药，请勿重复核对");

        List<Prescription> prescriptions = prescriptionsOf(bill);
        for (Prescription p : prescriptions) {
            if (p.getDrugId() == null) continue;
            Drug drug = drugMapper.selectById(p.getDrugId());
            int before = (drug != null && drug.getDrugStock() != null) ? drug.getDrugStock() : 0;
            int affected = drugMapper.update(null,
                    Wrappers.<Drug>lambdaUpdate()
                            .eq(Drug::getDrugId, p.getDrugId())
                            .ge(Drug::getDrugStock, p.getQuantity())
                            .setSql("drug_stock = drug_stock - " + p.getQuantity()));
            if (affected == 0)
                throw new BizException("「" + p.getDrugName() + "」库存不足（剩余 " + before + "），无法发药");
            drugService.logStockChange(p.getDrugId(), p.getDrugName(), 2, p.getQuantity(),
                    before, before - p.getQuantity(), "发药核对出库");
        }
        return Result.success("发药核对完成" + (prescriptions.isEmpty() ? "" : "，药品库存已扣减"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result refund(Integer billId, String reason, List<Integer> prescriptionIds) {
        if (reason == null || reason.isBlank()) throw new BizException("请填写退款原因");
        String operator = currentOperator();
        Bill bill = this.getById(billId);
        if (bill == null) throw new BizException("账单不存在");
        if (!Boolean.TRUE.equals(bill.getPayStatus())) throw new BizException("该账单未收款，无需退款");
        if (Boolean.TRUE.equals(bill.getRefundStatus())) throw new BizException("该账单已退款，请勿重复操作");
        boolean wasBalance = bill.getPayType() == null || !bill.getPayType();

        List<Prescription> allActive = prescriptionsOf(bill);
        List<Prescription> picked;
        boolean partial;
        if (prescriptionIds != null && !prescriptionIds.isEmpty()) {
            picked = prescriptionMapper.selectBatchIds(prescriptionIds).stream()
                    .filter(p -> p.getRecordId().equals(bill.getRecordId()))
                    .filter(p -> !Boolean.TRUE.equals(p.getIsCancel()))
                    .toList();
            if (picked.isEmpty()) throw new BizException("所选处方行不存在、不属于本单，或已退款/已作废");
            partial = true;
        } else {
            picked = allActive;
            partial = false;
        }

        // 逐行 CAS 抢占 is_cancel：并发重复退款只有一个请求能成功
        for (Prescription p : picked) {
            int affected = prescriptionMapper.update(null, Wrappers.<Prescription>lambdaUpdate()
                    .eq(Prescription::getPrescriptionId, p.getPrescriptionId())
                    .eq(Prescription::getIsCancel, false)
                    .set(Prescription::getIsCancel, true));
            if (affected == 0) throw new BizException("所选处方行已退款/已作废，请刷新后重试");
        }

        // 金额口径：收款按 real_money（折后），退款必须同口径；整单退按剩余实收全退（含非药费用）
        BigDecimal real = bill.getRealMoney() != null ? bill.getRealMoney() : nz(bill.getTotalMoney());
        BigDecimal remaining = real.subtract(nz(bill.getRefundMoney()));
        BigDecimal discount = bill.getDiscount() == null ? BigDecimal.ONE : bill.getDiscount();
        BigDecimal originalFee = picked.stream()
                .map(p -> nz(p.getDrugPrice()).multiply(BigDecimal.valueOf(p.getQuantity() == null ? 0 : p.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean fullRefund = !partial;
        BigDecimal refundFee = fullRefund
                ? remaining
                : originalFee.multiply(discount).setScale(2, RoundingMode.HALF_UP);
        if (refundFee.compareTo(remaining) > 0) refundFee = remaining;
        if (refundFee.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("该账单无可退金额");

        // 回库（仅「已发药核对」才扣过库存）
        if (Boolean.TRUE.equals(bill.getDispenseStatus())) {
            for (Prescription p : picked) {
                if (p.getDrugId() == null) continue;
                Drug drug = drugMapper.selectById(p.getDrugId());
                int before = (drug != null && drug.getDrugStock() != null) ? drug.getDrugStock() : 0;
                drugMapper.update(null, Wrappers.<Drug>lambdaUpdate()
                        .eq(Drug::getDrugId, p.getDrugId())
                        .setSql("drug_stock = drug_stock + " + p.getQuantity()));
                drugService.logStockChange(p.getDrugId(), p.getDrugName(), 3, p.getQuantity(),
                        before, before + p.getQuantity(), "退款回库");
            }
        }

        // 账单原始金额保持可追溯，只累加「累计已退金额」，报表按 real_money - refund_money 计算
        this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .setSql("refund_money = refund_money + " + refundFee.toPlainString())
                .update();

        if (wasBalance) {
            Client refundClient = clientService.getById(bill.getClientId());
            BigDecimal balanceBefore = refundClient == null || refundClient.getClientBalance() == null
                    ? BigDecimal.ZERO : refundClient.getClientBalance();
            clientService.lambdaUpdate()
                    .eq(Client::getClientId, bill.getClientId())
                    .setSql("client_balance = client_balance + " + refundFee.toPlainString())
                    .update();
            balanceLogService.record(bill.getClientId(), "REFUND", refundFee, balanceBefore, balanceBefore.add(refundFee),
                    "BILL", billId, operator, "账单退款回充" + (fullRefund ? "（整单）" : "（部分）"));
        }
        memberService.deductPoints(bill.getClientId(), refundFee);

        if (fullRefund) {
            boolean claimed = this.lambdaUpdate()
                    .eq(Bill::getBillId, billId)
                    .eq(Bill::getRefundStatus, false)
                    .set(Bill::getRefundStatus, true)
                    .set(Bill::getRefundBy, operator)
                    .set(Bill::getRefundReason, reason)
                    .set(Bill::getRefundTime, LocalDateTime.now())
                    .update();
            if (!claimed) throw new BizException("该账单已退款，请勿重复操作");
        }
        return Result.success((fullRefund ? "退款成功：" : "部分退款成功：")
                + "退 ￥" + refundFee.toPlainString()
                + (Boolean.TRUE.equals(bill.getDispenseStatus()) ? "，库存已回滚" : "")
                + (wasBalance ? "，余额已退还" : "（现金收款，请线下退现金）")
                + "；累计已退 ￥" + nz(bill.getRefundMoney()).add(refundFee).toPlainString());
    }
}
