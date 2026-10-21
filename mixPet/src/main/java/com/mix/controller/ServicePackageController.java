package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.entity.ServicePackage;
import com.mix.security.model.RoleConst;
import com.mix.service.IServicePackageService;
import com.mix.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 服务套餐字典管理
 */
@RestController
@RequestMapping("/package")
public class ServicePackageController {
    private final IServicePackageService packageService;

    @Autowired
    public ServicePackageController(IServicePackageService packageService) {
        this.packageService = packageService;
    }

    /** 启用中的套餐（接诊下拉用，登录即可） */
    @GetMapping
    public List<ServicePackage> listActive() {
        return packageService.listActive();
    }

    /** 分页 */
    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<ServicePackage> getPage(int numPage, int pageSize, String packageName, String category) {
        return packageService.selectByPage(packageName, category, numPage, pageSize);
    }

    /** 新增（可带 items 检查项目，主子表一个事务） */
    @PostMapping
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody ServicePackage p) {
        if (StrUtil.isWhite(p.getPackageName())) return Result.error("套餐名不能为空");
        if (p.getPrice() != null && p.getPrice().compareTo(BigDecimal.ZERO) < 0) return Result.error("价格不能为负数");
        if (p.getActive() == null) p.setActive(true);
        return Result.choice("添加", packageService.saveWithItems(p));
    }

    /** 修改（items 全量重写） */
    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody ServicePackage p) {
        if (p.getPackageId() == null) return Result.error("缺少套餐id");
        if (StrUtil.isWhite(p.getPackageName())) return Result.error("套餐名不能为空");
        if (p.getPrice() != null && p.getPrice().compareTo(BigDecimal.ZERO) < 0) return Result.error("价格不能为负数");
        return Result.choice("修改", packageService.saveWithItems(p));
    }

    /** 切换启用/停用（列表行内开关） */
    @PutMapping("/status/{id}")
    @Secured(RoleConst.MANAGER)
    public Result switchStatus(@PathVariable Integer id, Boolean active) {
        if (active == null) return Result.error("缺少状态参数");
        return Result.choice("状态切换", packageService.switchActive(id, active));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除", packageService.deleteById(id));
    }
}
