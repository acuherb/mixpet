package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.entity.Drug;
import com.ship.model.entity.DrugRecord;
import com.ship.model.vo.InboundBatchVo;
import com.ship.model.vo.InboundLine;
import com.ship.security.model.RoleConst;
import com.ship.service.IDrugService;
import com.ship.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 药品管理
 */
@RestController
@RequestMapping("/drug")
public class DrugController {
    private final IDrugService drugService;

    @Autowired
    public DrugController(IDrugService drugService) {
        this.drugService = drugService;
    }

    /** 启用中的药品列表（医生开处方下拉用，不含疫苗） */
    @GetMapping
    @Secured(RoleConst.NURSE)
    public List<Drug> listActive() {
        return drugService.listActive();
    }

    /** 启用中的疫苗列表（免疫登记下拉用，即 drug_type=疫苗） */
    @GetMapping("/vaccine")
    @Secured(RoleConst.NURSE)
    public List<Drug> listVaccine() {
        return drugService.listVaccine();
    }

    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<Drug> getPage(int numPage, int pageSize, String drugName, String drugType) {
        return drugService.selectByPage(drugName, drugType, numPage, pageSize);
    }

    @PostMapping
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody Drug drug) {
        if (StrUtil.isWhite(drug.getDrugName())) return Result.error("药品名不能为空");
        if (drug.getDrugPrice() != null && drug.getDrugPrice().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("单价不能为负数");
        if (drug.getDrugStock() != null && drug.getDrugStock() < 0) return Result.error("库存不能为负数");
        if (drug.getDosePerKg() != null && drug.getDosePerKg().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("每kg单次用量不能为负数");
        if (drug.getDoseFixed() != null && drug.getDoseFixed().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("固定单次用量不能为负数");
        if (drug.getActive() == null) drug.setActive(true);
        return Result.choice("添加", drugService.save(drug));
    }

    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody Drug drug) {
        if (drug.getDrugId() == null) return Result.error("缺少药品id");
        if (StrUtil.isWhite(drug.getDrugName())) return Result.error("药品名不能为空");
        if (drug.getDrugPrice() != null && drug.getDrugPrice().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("单价不能为负数");
        if (drug.getDrugStock() != null && drug.getDrugStock() < 0) return Result.error("库存不能为负数");
        if (drug.getDosePerKg() != null && drug.getDosePerKg().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("每kg单次用量不能为负数");
        if (drug.getDoseFixed() != null && drug.getDoseFixed().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("固定单次用量不能为负数");
        return Result.choice("修改", drugService.updateById(drug));
    }

    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除", drugService.deleteById(id));
    }

    @DeleteMapping("/batch/{ids}")
    @Secured(RoleConst.MANAGER)
    public Result deleteGroup(@PathVariable List<Integer> ids) {
        return Result.choice("删除多个", drugService.deleteByIds(ids));
    }

    /** 低库存预警列表（stock <= warningStock） */
    @GetMapping("/warning")
    @Secured(RoleConst.MANAGER)
    public List<Drug> warning() {
        return drugService.listWarning();
    }

    /** 入库：增加库存并记台账（可带供应商/批号），可同时更新有效期 */
    @PostMapping("/inbound")
    @Secured(RoleConst.MANAGER)
    public Result inbound(Integer drugId, Integer quantity, LocalDate expireDate, String note, Integer supplierId, String batchNo) {
        return drugService.inbound(drugId, quantity, expireDate, note, supplierId, batchNo);
    }

    /**
     * 批量入库：逐条复用 inbound 逻辑独立提交（部分成功），
     * 返回成功数/失败数/失败原因(含行号)/失败行号(前端据此保留失败行)
     */
    @PostMapping("/inbound/batch")
    @Secured(RoleConst.MANAGER)
    public InboundBatchVo inboundBatch(@RequestBody List<InboundLine> lines) {
        return drugService.inboundBatch(lines);
    }

    /** 近效期预警列表（expireDate 在未来 days 天内，含当天） */
    @GetMapping("/expiring")
    @Secured(RoleConst.MANAGER)
    public List<Drug> expiring(int days) {
        return drugService.listExpiring(days);
    }

    /** 出入库台账分页 */
    @GetMapping("/record/page")
    @Secured(RoleConst.MANAGER)
    public IPage<DrugRecord> recordPage(Integer drugId, int numPage, int pageSize) {
        return drugService.stockRecordPage(drugId, numPage, pageSize);
    }
}
