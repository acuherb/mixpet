package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.entity.Drug;
import com.ship.security.model.RoleConst;
import com.ship.service.IDrugService;
import com.ship.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
}
