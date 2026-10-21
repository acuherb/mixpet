package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.entity.ImageTest;
import com.mix.security.model.RoleConst;
import com.mix.service.IImageTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 影像项目字典管理
 */
@RestController
@RequestMapping("/imageTest")
public class ImageTestController {
    private final IImageTestService testService;

    @Autowired
    public ImageTestController(IImageTestService testService) {
        this.testService = testService;
    }

    /** 全部分组列表（供开单页勾选） */
    @GetMapping("/group")
    public List<ImageTest> selectAll() {
        return testService.lambdaQuery().orderByAsc(ImageTest::getGroupName).list();
    }

    /** 字典分页联查 */
    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public com.baomidou.mybatisplus.core.metadata.IPage<ImageTest> getPage(String itemName, String groupName,
                                                                           int numPage, int pageSize) {
        return testService.lambdaQuery()
                .and(itemName != null && !itemName.isBlank(),
                        w -> w.like(ImageTest::getItemName, itemName))
                .eq(groupName != null && !groupName.isBlank(), ImageTest::getGroupName, groupName)
                .orderByAsc(ImageTest::getGroupName)
                .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(numPage, pageSize));
    }

    /** 新增/修改 */
    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result saveOrUpdate(@RequestBody ImageTest test) {
        if (test.getItemName() == null || test.getItemName().isBlank()) return Result.error("请输入项目名称");
        return Result.choice("保存", testService.saveOrUpdate(test));
    }

    /** 单个影像项（录报告时取报告模板，员工可查） */
    @GetMapping("/one/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public ImageTest one(@PathVariable Integer id) {
        return testService.getById(id);
    }

    /** 删除（被引用则停用） */
    @DeleteMapping("/item/{id}")
    @Secured(RoleConst.MANAGER)
    public boolean deleteTest(@PathVariable Integer id) {
        return testService.deleteTest(id);
    }
}