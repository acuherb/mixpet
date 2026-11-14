package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.entity.CheckupRecord;
import com.mix.model.entity.CheckupTemplate;
import com.mix.model.vo.CheckupSaveVo;
import com.mix.security.model.RoleConst;
import com.mix.service.ICheckupService;
import com.mix.util.StrUtil;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 结构化体检表：模板（管理端可编辑）+ 报告（医生接诊录入、顾客只读自己的）。
 * 老 mrt 版式打印页 z-mrtPrint 保留只读，两者并行。
 */
@RestController
@RequestMapping("/checkup")
public class CheckupController {

    private final ICheckupService checkupService;

    public CheckupController(ICheckupService checkupService) {
        this.checkupService = checkupService;
    }

    // ── 模板 ──────────────────────────────────────────────
    /** 启用中的模板：接诊页下拉 */
    @GetMapping("/template")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public List<CheckupTemplate> listTemplate() {
        return checkupService.listActive();
    }

    @GetMapping("/template/page")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public IPage<CheckupTemplate> templatePage(int numPage, int pageSize, String seaName) {
        return checkupService.selectTemplatePage(seaName, numPage, pageSize);
    }

    @GetMapping("/template/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public CheckupTemplate templateOne(@PathVariable Integer id) {
        return checkupService.getById(id);
    }

    @PostMapping("/template")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result saveTemplate(@RequestBody CheckupTemplate t) {
        if (StrUtil.isWhite(t.getName())) return Result.error("模板名称不能为空");
        if (StrUtil.isWhite(t.getSchemaJson())) return Result.error("模板内容不能为空");
        if (t.getActive() == null) t.setActive(true);
        if (t.getIsDel() == null) t.setIsDel(0L);
        if (t.getSortOrder() == null) t.setSortOrder(99);
        return Result.choice("添加", checkupService.save(t));
    }

    @PutMapping("/template")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result updateTemplate(@RequestBody CheckupTemplate t) {
        if (t.getTemplateId() == null) return Result.error("缺少模板id");
        if (StrUtil.isWhite(t.getName())) return Result.error("模板名称不能为空");
        if (StrUtil.isWhite(t.getSchemaJson())) return Result.error("模板内容不能为空");
        return Result.choice("修改", checkupService.updateById(t));
    }

    /** 逻辑删除（内置种子也允许停用，但不物理删，历史报告靠快照不受影响） */
    @DeleteMapping("/template/{id}")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result deleteTemplate(@PathVariable Integer id) {
        CheckupTemplate t = new CheckupTemplate();
        t.setTemplateId(id);
        t.setIsDel(1L);
        t.setActive(false);
        return Result.choice("删除", checkupService.updateById(t));
    }

    // ── 体检报告 ──────────────────────────────────────────
    /** 保存一次体检，返回 checkupId（Result.data 包一层，前端取 res.data） */
    @PostMapping("/record")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result saveRecord(@RequestBody CheckupSaveVo vo) {
        return Result.data(checkupService.saveCheckup(vo));
    }

    /** 单份报告：员工任意，顾客仅本人宠物 */
    @GetMapping("/record/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN, RoleConst.CLIENT})
    public CheckupRecord recordOne(@PathVariable Integer id) {
        return checkupService.getRecordForReader(id);
    }

    /** 某宠物的历史报告：就诊档案时间线 / 顾客端列表 */
    @GetMapping("/record/pet/{petId}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN, RoleConst.CLIENT})
    public List<CheckupRecord> listByPet(@PathVariable Integer petId) {
        return checkupService.listByPetForReader(petId);
    }

    /** 某次就诊关联的体检报告（接诊页回显） */
    @GetMapping("/record/byRecord/{recordId}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public List<CheckupRecord> listByRecord(@PathVariable Integer recordId) {
        return checkupService.listByRecord(recordId);
    }

    @DeleteMapping("/record/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public Result deleteRecord(@PathVariable Integer id) {
        return Result.choice("删除", checkupService.removeCheckup(id));
    }
}
