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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 账单 Service 实现
 */
@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements IBillService {
    private final IClientService clientService;
    private final DrugMapper drugMapper;
    private final PrescriptionMapper prescriptionMapper;

    @Autowired
    public BillServiceImpl(IClientService clientService, DrugMapper drugMapper, PrescriptionMapper prescriptionMapper) {
        this.clientService = clientService;
        this.drugMapper = drugMapper;
        this.prescriptionMapper = prescriptionMapper;
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
            clientService.lambdaUpdate()
                    .eq(Client::getClientId, client.getClientId())
                    .setSql("client_balance = client_balance - " + bill.getTotalMoney().toPlainString())
                    .update();
        }
        // 发药：校验库存并扣减
        List<Prescription> prescriptions = prescriptionMapper.selectByRecord(bill.getRecordId());
        for (Prescription p : prescriptions) {
            Drug drug = drugMapper.selectById(p.getDrugId());
            if (drug != null && (drug.getDrugStock() == null || drug.getDrugStock() < p.getQuantity()))
                return Result.error("「" + p.getDrugName() + "」库存不足（剩余 "
                        + (drug.getDrugStock() == null ? 0 : drug.getDrugStock()) + "），无法发药");
        }
        for (Prescription p : prescriptions) {
            drugMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Drug>()
                            .eq(Drug::getDrugId, p.getDrugId())
                            .setSql("drug_stock = drug_stock - " + p.getQuantity()));
        }
        // 账单标记已付
        this.lambdaUpdate()
                .eq(Bill::getBillId, billId)
                .set(Bill::getPayStatus, true)
                .set(Bill::getPayType, payType != null && payType)
                .update();
        return Result.success("收款成功" + (prescriptions.isEmpty() ? "" : "，处方药品已扣减库存"));
    }
}
