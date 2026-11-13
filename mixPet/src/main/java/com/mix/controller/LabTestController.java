package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.entity.LabTest;
import com.mix.model.vo.LabPanelSaveVo;
import com.mix.model.vo.LabPanelVo;
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
    public IPage<LabTest> getPage(int numPage, int pageSize, String testName, String testGroup, Integer isPanel) {
        return labTestService.selectByPage(testName, testGroup, isPanel, numPage, pageSize);
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

    /** 套组列表（含成员单项），医生开单选择套组用；非临床动作按在职员工基线放行 */
    @GetMapping("/panels")
    @Secured(RoleConst.NURSE)
    public List<LabPanelVo> listPanels() {
        return labTestService.listPanelsWithMembers();
    }

    /** 新增套组（含成员） */
    @PostMapping("/panel")
    @Secured(RoleConst.MANAGER)
    public Result savePanel(@RequestBody LabPanelSaveVo vo) {
        return Result.choice("添加套组", labTestService.savePanel(vo));
    }

    /** 修改套组（含成员） */
    @PutMapping("/panel")
    @Secured(RoleConst.MANAGER)
    public Result updatePanel(@RequestBody LabPanelSaveVo vo) {
        return Result.choice("修改套组", labTestService.updatePanel(vo));
    }

    /** 删除套组（同步删成员；被引用则套组头停用） */
    @DeleteMapping("/panel/{id}")
    @Secured(RoleConst.MANAGER)
    public Result deletePanel(@PathVariable Integer id) {
        return Result.choice("删除套组", labTestService.deletePanel(id));
    }
}