package com.mix.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mix.annotation.AuditLog;
import com.mix.common.Result;
import com.mix.mapper.DiagnosisDictMapper;
import com.mix.model.entity.DiagnosisDict;
import com.mix.security.model.RoleConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 诊断编码字典：接诊时下拉选码，管理层维护 */
@RestController
@RequestMapping("/diagnosis")
public class DiagnosisDictController {
    private final DiagnosisDictMapper dictMapper;

    @Autowired
    public DiagnosisDictController(DiagnosisDictMapper dictMapper) {
        this.dictMapper = dictMapper;
    }

    /** 启用中的编码（接诊向导下拉，支持按名称/编码模糊搜索） */
    @GetMapping
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public List<DiagnosisDict> list(String keyword) {
        LambdaQueryWrapper<DiagnosisDict> q = Wrappers.<DiagnosisDict>lambdaQuery()
                .eq(DiagnosisDict::getActive, true);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            q.and(w -> w.like(DiagnosisDict::getName, kw).or().like(DiagnosisDict::getCode, kw));
        }
        return dictMapper.selectList(q.orderByAsc(DiagnosisDict::getCode).last("limit 200"));
    }

    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<DiagnosisDict> page(int numPage, int pageSize, String keyword) {
        LambdaQueryWrapper<DiagnosisDict> q = Wrappers.lambdaQuery();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            q.and(w -> w.like(DiagnosisDict::getName, kw).or().like(DiagnosisDict::getCode, kw));
        }
        return dictMapper.selectPage(new Page<>(numPage, pageSize), q.orderByAsc(DiagnosisDict::getCode));
    }

    @PostMapping
    @AuditLog(module = "diagnosis", action = "add", target = "新增诊断编码")
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody DiagnosisDict dict) {
        if (dict.getCode() == null || dict.getCode().isBlank()
                || dict.getName() == null || dict.getName().isBlank())
            return Result.error("编码与名称必填");
        long exists = dictMapper.selectCount(Wrappers.<DiagnosisDict>lambdaQuery()
                .eq(DiagnosisDict::getCode, dict.getCode().trim()));
        if (exists > 0) return Result.error("该编码已存在");
        if (dict.getActive() == null) dict.setActive(true);
        return Result.choice("新增成功", dictMapper.insert(dict) > 0);
    }

    @PutMapping
    @AuditLog(module = "diagnosis", action = "update", target = "修改诊断编码")
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody DiagnosisDict dict) {
        if (dict.getDictId() == null) return Result.error("缺少字典id");
        return Result.choice("修改成功", dictMapper.updateById(dict) > 0);
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "diagnosis", action = "delete", target = "删除诊断编码")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        // 逻辑停用而非物理删除，避免历史病历里的编码变成孤儿
        DiagnosisDict d = new DiagnosisDict();
        d.setDictId(id);
        d.setActive(false);
        return Result.choice("已停用", dictMapper.updateById(d) > 0);
    }
}
