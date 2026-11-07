package com.mix.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mix.annotation.AuditLog;
import com.mix.common.Result;
import com.mix.mapper.LabRefRangeMapper;
import com.mix.model.entity.LabRefRange;
import com.mix.security.model.RoleConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 检验参考区间维护：按检验项 / 物种 / 性别 / 月龄分层的数值化参考区间与危急阈值。
 * 检验科在此核对本院仪器试剂的参考范围（系统不预置医学数值）。
 */
@RestController
@RequestMapping("/refRange")
public class RefRangeController {
    private final LabRefRangeMapper rangeMapper;

    @Autowired
    public RefRangeController(LabRefRangeMapper rangeMapper) {
        this.rangeMapper = rangeMapper;
    }

    /** 按检验项查全部区间（含停用），维护页用 */
    @GetMapping
    @Secured({RoleConst.MANAGER, RoleConst.DOCTOR, RoleConst.NURSE})
    public List<LabRefRange> list(Integer testId) {
        LambdaQueryWrapper<LabRefRange> q = Wrappers.lambdaQuery();
        if (testId != null) q.eq(LabRefRange::getTestId, testId);
        return rangeMapper.selectList(q.orderByAsc(LabRefRange::getTestId).orderByDesc(LabRefRange::getActive));
    }

    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<LabRefRange> page(int numPage, int pageSize, Integer testId, String keyword) {
        LambdaQueryWrapper<LabRefRange> q = Wrappers.lambdaQuery();
        if (testId != null) q.eq(LabRefRange::getTestId, testId);
        if (keyword != null && !keyword.isBlank()) q.like(LabRefRange::getRefText, keyword.trim());
        return rangeMapper.selectPage(new Page<>(numPage, pageSize), q.orderByAsc(LabRefRange::getTestId));
    }

    @PostMapping
    @AuditLog(module = "refRange", action = "add", target = "新增参考区间")
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody LabRefRange range) {
        if (range.getTestId() == null) return Result.error("请选择检验项");
        if (range.getActive() == null) range.setActive(true);
        return Result.choice("新增成功", rangeMapper.insert(range) > 0);
    }

    @PutMapping
    @AuditLog(module = "refRange", action = "update", target = "修改参考区间")
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody LabRefRange range) {
        if (range.getRangeId() == null) return Result.error("缺少区间id");
        return Result.choice("修改成功", rangeMapper.updateById(range) > 0);
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "refRange", action = "delete", target = "停用参考区间")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        // 逻辑停用，历史单据已快照，不受影响
        LabRefRange r = new LabRefRange();
        r.setRangeId(id);
        r.setActive(false);
        return Result.choice("已停用", rangeMapper.updateById(r) > 0);
    }
}
