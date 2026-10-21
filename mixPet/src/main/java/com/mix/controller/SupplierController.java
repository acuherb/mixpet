package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.entity.Supplier;
import com.mix.security.model.RoleConst;
import com.mix.service.ISupplierService;
import com.mix.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商
 */
@RestController
@RequestMapping("/supplier")
public class SupplierController {
    private final ISupplierService supplierService;

    @Autowired
    public SupplierController(ISupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /** 启用中的供应商（采购入库下拉用） */
    @GetMapping
    @Secured(RoleConst.MANAGER)
    public List<Supplier> listActive() {
        return supplierService.listActive();
    }

    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<Supplier> getPage(int numPage, int pageSize, String seaName) {
        return supplierService.selectPage(seaName, numPage, pageSize);
    }

    @PostMapping
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody Supplier supplier) {
        if (StrUtil.isWhite(supplier.getSupplierName())) return Result.error("供应商名称不能为空");
        if (supplier.getActive() == null) supplier.setActive(true);
        return Result.choice("添加", supplierService.save(supplier));
    }

    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody Supplier supplier) {
        if (supplier.getSupplierId() == null) return Result.error("缺少供应商id");
        if (StrUtil.isWhite(supplier.getSupplierName())) return Result.error("供应商名称不能为空");
        return Result.choice("修改", supplierService.updateById(supplier));
    }

    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除", supplierService.removeById(id));
    }
}
