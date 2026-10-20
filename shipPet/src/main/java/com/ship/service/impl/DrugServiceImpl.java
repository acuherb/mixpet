package com.ship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.DrugMapper;
import com.ship.mapper.DrugRecordMapper;
import com.ship.mapper.SupplierMapper;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.Drug;
import com.ship.model.entity.DrugRecord;
import com.ship.model.entity.Supplier;
import com.ship.service.IDrugService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.ship.model.vo.InboundBatchVo;
import com.ship.model.vo.InboundLine;
import java.util.ArrayList;
import java.util.List;

/**
 * 药品 Service 实现
 */
@Service
public class DrugServiceImpl extends ServiceImpl<DrugMapper, Drug> implements IDrugService {

    @Autowired
    private DrugRecordMapper drugRecordMapper;

    @Autowired
    private SupplierMapper supplierMapper;

    private String currentOperator() {
        Object u = UserUtil.getUser();
        if (u instanceof Doctor d) return d.getDoctorName();
        if (u instanceof Client c) return c.getClientName();
        return "系统";
    }

    @Override
    public boolean deleteById(Integer drugId) {
        return this.lambdaUpdate()
                .eq(Drug::getDrugId, drugId)
                .set(Drug::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public boolean deleteByIds(List<Integer> idGroup) {
        return this.lambdaUpdate()
                .in(Drug::getDrugId, idGroup)
                .set(Drug::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public IPage<Drug> selectByPage(String drugName, String drugType, int numPage, int pageSize) {
        return this.lambdaQuery()
                .and(drugName != null && !drugName.isEmpty(), w -> w
                        .like(Drug::getDrugName, drugName)
                        .or().like(Drug::getDrugSpec, drugName))
                .eq(drugType != null && !drugType.isEmpty(), Drug::getDrugType, drugType)
                .orderByAsc(Drug::getDrugName)
                .page(new Page<>(numPage, pageSize));
    }

    @Override
    public List<Drug> listActive() {
        // 开方下拉不含疫苗（疫苗走免疫登记，不开处方）
        return this.lambdaQuery().eq(Drug::getActive, true)
                .ne(Drug::getDrugType, "疫苗")
                .orderByAsc(Drug::getDrugName).list();
    }

    @Override
    public List<Drug> listVaccine() {
        return this.lambdaQuery().eq(Drug::getActive, true)
                .eq(Drug::getDrugType, "疫苗")
                .orderByAsc(Drug::getDrugName).list();
    }

    @Override
    public List<Drug> listWarning() {
        return this.lambdaQuery().eq(Drug::getActive, true).list().stream()
                .filter(d -> {
                    int stock = d.getDrugStock() == null ? 0 : d.getDrugStock();
                    int warn = d.getWarningStock() == null ? 0 : d.getWarningStock();
                    return warn > 0 && stock <= warn;
                })
                .toList();
    }

    @Override
    public Result inbound(Integer drugId, Integer quantity, BigDecimal costPrice, LocalDate expireDate, String note, Integer supplierId, String batchNo) {
        if (drugId == null || quantity == null || quantity <= 0) return Result.error("入库数量必须大于0");
        if (costPrice != null && costPrice.compareTo(BigDecimal.ZERO) < 0)
            return Result.error("成本价不能为负数");
        Drug drug = this.getById(drugId);
        if (drug == null) return Result.error("药品不存在");
        int before = drug.getDrugStock() == null ? 0 : drug.getDrugStock();
        int after = before + quantity;
        this.lambdaUpdate().eq(Drug::getDrugId, drugId).set(Drug::getDrugStock, after).update();
        // 录了成本价：刷新药品当前进货价（报表毛利按当前进货价估算，取最新入库成本）
        if (costPrice != null) {
            this.lambdaUpdate().eq(Drug::getDrugId, drugId).set(Drug::getCostPrice, costPrice).update();
        }
        if (expireDate != null)
            this.lambdaUpdate().eq(Drug::getDrugId, drugId).set(Drug::getExpireDate, expireDate).update();
        // 记台账（含供应商/批号/成本价，便于追溯采购来源与历史成本）
        String supplierName = null;
        if (supplierId != null) {
            Supplier s = supplierMapper.selectById(supplierId);
            if (s != null) supplierName = s.getSupplierName();
        }
        DrugRecord r = new DrugRecord();
        r.setDrugId(drugId);
        r.setDrugName(drug.getDrugName());
        r.setChangeType(1);
        r.setQuantity(quantity);
        r.setCostPrice(costPrice);
        r.setBeforeStock(before);
        r.setAfterStock(after);
        r.setOperator(currentOperator());
        r.setSupplierId(supplierId);
        r.setSupplierName(supplierName);
        r.setBatchNo(batchNo);
        r.setNote(note);
        drugRecordMapper.insert(r);
        return Result.success("入库成功，当前库存 " + after
                + (costPrice != null ? "，成本价 ￥" + costPrice.toPlainString() + " 已更新" : ""));
    }

    @Override
    public InboundBatchVo inboundBatch(List<InboundLine> lines) {
        if (lines == null || lines.isEmpty()) return new InboundBatchVo(0, 0, List.of(), List.of());
        List<String> errors = new ArrayList<>();
        List<Integer> failedRows = new ArrayList<>();
        int success = 0;
        // 逐条独立提交（部分成功语义：一条失败不影响其他行；失败行由前端保留重试）
        for (int i = 0; i < lines.size(); i++) {
            InboundLine line = lines.get(i);
            Result r = this.inbound(line.getDrugId(), line.getQuantity(), line.getCostPrice(), line.getExpireDate(),
                    line.getNote(), line.getSupplierId(), line.getBatchNo());
            if (r.code() != null && r.code() == 1) {
                success++;
            } else {
                failedRows.add(i + 1);
                errors.add("第" + (i + 1) + "行：" + (r.msg() == null ? "入库失败" : r.msg()));
            }
        }
        return new InboundBatchVo(success, failedRows.size(), errors, failedRows);
    }

    @Override
    public List<Drug> listExpiring(int days) {
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(Math.max(days, 0));
        return this.lambdaQuery()
                .eq(Drug::getActive, true)
                .isNotNull(Drug::getExpireDate)
                .ge(Drug::getExpireDate, today)
                .le(Drug::getExpireDate, horizon)
                .orderByAsc(Drug::getExpireDate)
                .list();
    }

    @Override
    public IPage<DrugRecord> stockRecordPage(Integer drugId, int numPage, int pageSize) {
        return drugRecordMapper.selectPage(new Page<>(numPage, pageSize),
                new LambdaQueryWrapper<DrugRecord>()
                        .eq(drugId != null, DrugRecord::getDrugId, drugId)
                        .orderByDesc(DrugRecord::getCreateTime));
    }

    @Override
    public void logStockChange(Integer drugId, String drugName, int changeType, int quantity, int before, int after, String note) {
        DrugRecord r = new DrugRecord();
        r.setDrugId(drugId);
        r.setDrugName(drugName);
        r.setChangeType(changeType);
        r.setQuantity(quantity);
        r.setBeforeStock(before);
        r.setAfterStock(after);
        r.setOperator(currentOperator());
        r.setNote(note);
        drugRecordMapper.insert(r);
    }
}
