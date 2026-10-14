package com.ship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.DrugMapper;
import com.ship.mapper.DrugRecordMapper;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.Drug;
import com.ship.model.entity.DrugRecord;
import com.ship.service.IDrugService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 药品 Service 实现
 */
@Service
public class DrugServiceImpl extends ServiceImpl<DrugMapper, Drug> implements IDrugService {

    @Autowired
    private DrugRecordMapper drugRecordMapper;

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
    public Result inbound(Integer drugId, Integer quantity, LocalDate expireDate, String note) {
        if (drugId == null || quantity == null || quantity <= 0) return Result.error("入库数量必须大于0");
        Drug drug = this.getById(drugId);
        if (drug == null) return Result.error("药品不存在");
        int before = drug.getDrugStock() == null ? 0 : drug.getDrugStock();
        int after = before + quantity;
        this.lambdaUpdate().eq(Drug::getDrugId, drugId).set(Drug::getDrugStock, after).update();
        if (expireDate != null)
            this.lambdaUpdate().eq(Drug::getDrugId, drugId).set(Drug::getExpireDate, expireDate).update();
        logStockChange(drugId, drug.getDrugName(), 1, quantity, before, after, note);
        return Result.success("入库成功，当前库存 " + after);
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
