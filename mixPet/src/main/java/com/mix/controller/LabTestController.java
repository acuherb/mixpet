package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.entity.LabTest;
import com.mix.security.model.RoleConst;
import com.mix.service.ILabTestService;
import com.mix.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检验项字典管理
 */
@RestController
@RequestMapping("/labTest")
public class LabTestController {
    private final ILabTestService labTestService;

    @Autowired
    public LabTestController(ILabTestService labTestService) {
        this.labTestService = labTestService;
    }

    /** 启用中的检验项（开单勾选、字典下拉都要用；非临床动作，按在职员工基线放行） */
    @GetMapping
    @Secured(RoleConst.NURSE)
    public List<LabTest> listActive() {
        return labTestService.lambdaQuery()
                .eq(LabTest::getActive, true)
                .orderByAsc(LabTest::getTestGroup)
                .list();
    }

    /** 字典分页（后台管理） */
    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<LabTest> getPage(int numPage, int pageSize, String testName, String testGroup) {
        return labTestService.selectByPage(testName, testGroup, numPage, pageSize);
    }

    @PostMapping
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody LabTest test) {
        if (StrUtil.isWhite(test.getTestName())) return Result.error("检验项名不能为空");
        if (test.getTestPrice() != null && test.getTestPrice().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("单价不能为负数");
        if (test.getActive() == null) test.setActive(true);
        return Result.choice("添加", labTestService.save(test));
    }

    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody LabTest test) {
        if (test.getTestId() == null) return Result.error("缺少检验项id");
        if (StrUtil.isWhite(test.getTestName())) return Result.error("检验项名不能为空");
        if (test.getTestPrice() != null && test.getTestPrice().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("单价不能为负数");
        return Result.choice("修改", labTestService.updateById(test));
    }

    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除", labTestService.deleteById(id));
    }
}