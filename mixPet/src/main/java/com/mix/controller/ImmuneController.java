package com.mix.controller;

import com.mix.common.Result;
import com.mix.mapper.PetMapper;
import com.mix.model.dto.ImmuneRecordDto;
import com.mix.model.entity.Client;
import com.mix.model.entity.Pet;
import com.mix.model.vo.ImmuneVo;
import com.mix.security.model.RoleConst;
import com.mix.service.IImmuneRecordService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 免疫接种：登记、档案查询、顾客查看
 */
@RestController
@RequestMapping("/immune")
public class ImmuneController {
    private final IImmuneRecordService immuneService;
    private final PetMapper petMapper;

    @Autowired
    public ImmuneController(IImmuneRecordService immuneService, PetMapper petMapper) {
        this.immuneService = immuneService;
        this.petMapper = petMapper;
    }

    /** 接种登记 */
    @PostMapping
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    public Result saveImmune(@RequestBody ImmuneVo vo) {
        var doctor = UserUtil.getUser();
        com.mix.model.entity.Doctor d = doctor instanceof com.mix.model.entity.Doctor
                ? (com.mix.model.entity.Doctor) doctor : null;
        return immuneService.saveImmune(vo, d);
    }

    /** 档案分页（员工端） */
    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.DOCTOR})
    public com.baomidou.mybatisplus.core.metadata.IPage<ImmuneRecordDto> getPage(String petName, Boolean dueSoon,
                                                                                 int numPage, int pageSize) {
        return immuneService.selectPage(petName, dueSoon, numPage, pageSize);
    }

    /** 某宠物的档案：顾客只能查本人名下宠物，员工不限 */
    @GetMapping("/pet/{petId}")
    public List<ImmuneRecordDto> selectByPet(@PathVariable Integer petId) {
        if (petId == null) return List.of();
        if (UserUtil.getUser() instanceof Client client) {
            Pet pet = petMapper.selectById(petId);
            if (pet == null || client.getClientId() == null
                    || !client.getClientId().equals(pet.getClientId())) return List.of();
        }
        return immuneService.selectByPet(petId);
    }
}
