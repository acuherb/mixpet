package com.ship.controller;

import com.ship.common.Result;
import com.ship.model.entity.Vaccine;
import com.ship.security.model.RoleConst;
import com.ship.service.IVaccineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 疫苗字典管理
 */
@RestController
@RequestMapping("/vaccine")
public class VaccineController {
    private final IVaccineService vaccineService;

    @Autowired
    public VaccineController(IVaccineService vaccineService) {
        this.vaccineService = vaccineService;
    }

    /** 全部疫苗列表（供接种登记选择） */
    @GetMapping("/list")
    public List<Vaccine> selectAll() {
        return vaccineService.lambdaQuery().orderByAsc(Vaccine::getVaccineName).list();
    }

    /** 分页 */
    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public com.baomidou.mybatisplus.core.metadata.IPage<Vaccine> getPage(String vaccineName, int numPage, int pageSize) {
        return vaccineService.lambdaQuery()
                .like(vaccineName != null && !vaccineName.isBlank(), Vaccine::getVaccineName, vaccineName)
                .orderByAsc(Vaccine::getVaccineName)
                .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(numPage, pageSize));
    }

    /** 新增/修改 */
    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result saveOrUpdate(@RequestBody Vaccine vaccine) {
        if (vaccine.getVaccineName() == null || vaccine.getVaccineName().isBlank()) return Result.error("请输入疫苗名称");
        return Result.choice("保存", vaccineService.saveOrUpdate(vaccine));
    }

    /** 删除（被引用则停用） */
    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public boolean deleteVaccine(@PathVariable Integer id) {
        return vaccineService.deleteVaccine(id);
    }
}