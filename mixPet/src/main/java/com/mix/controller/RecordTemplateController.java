package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.entity.RecordTemplate;
import com.mix.security.model.RoleConst;
import com.mix.service.IRecordTemplateService;
import com.mix.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电子病历模板
 */
@RestController
@RequestMapping("/recordTemplate")
public class RecordTemplateController {
    private final IRecordTemplateService templateService;

    @Autowired
    public RecordTemplateController(IRecordTemplateService templateService) {
        this.templateService = templateService;
    }

    /** 启用中的模板（接诊下拉用，医生/护士） */
    @GetMapping
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE})
    public List<RecordTemplate> listActive() {
        return templateService.listActive();
    }

    @GetMapping("/page")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public IPage<RecordTemplate> getPage(int numPage, int pageSize, String seaName) {
        return templateService.selectPage(seaName, numPage, pageSize);
    }

    @PostMapping
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result save(@RequestBody RecordTemplate t) {
        if (StrUtil.isWhite(t.getName())) return Result.error("模板名称不能为空");
        if (t.getActive() == null) t.setActive(true);
        return Result.choice("添加", templateService.save(t));
    }

    @PutMapping
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result update(@RequestBody RecordTemplate t) {
        if (t.getTemplateId() == null) return Result.error("缺少模板id");
        if (StrUtil.isWhite(t.getName())) return Result.error("模板名称不能为空");
        return Result.choice("修改", templateService.updateById(t));
    }

    @DeleteMapping("/{id}")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除", templateService.removeById(id));
    }
}
