package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.entity.SysDict;
import com.mix.security.model.RoleConst;
import com.mix.service.ISysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通用字典/系统设置：集中管理各模块枚举
 */
@RestController
@RequestMapping("/dict")
public class SysDictController {
    private final ISysDictService dictService;

    @Autowired
    public SysDictController(ISysDictService dictService) {
        this.dictService = dictService;
    }

    /** 某类型字典值（登录用户均可，用于各模块下拉） */
    @GetMapping("/type/{type}")
    public List<SysDict> byType(@PathVariable String type) {
        return dictService.listByType(type);
    }

    @PostMapping
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody SysDict dict) {
        if (dict.getDictType() == null || dict.getDictType().isBlank()) return Result.error("缺少字典类型");
        if (dict.getDictKey() == null || dict.getDictKey().isBlank()) return Result.error("字典值不能为空");
        if (dict.getSort() == null) dict.setSort(0);
        if (dict.getActive() == null) dict.setActive(true);
        return Result.choice("添加", dictService.save(dict));
    }

    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody SysDict dict) {
        if (dict.getDictId() == null) return Result.error("缺少字典id");
        return Result.choice("修改", dictService.updateById(dict));
    }

    @PutMapping("/toggle/{id}")
    @Secured(RoleConst.MANAGER)
    public Result toggle(@PathVariable Integer id, boolean active) {
        return Result.choice(active ? "启用" : "停用",
                dictService.lambdaUpdate().eq(SysDict::getDictId, id).set(SysDict::getActive, active).update());
    }

    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除", dictService.removeById(id));
    }
}
