package com.mix.controller;

import com.mix.model.entity.Client;
import com.mix.model.entity.Pet;
import com.mix.model.vo.PetArchive;
import com.mix.service.IPetArchiveService;
import com.mix.security.model.RoleConst;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 宠物就诊档案（完整时间线：病历/处方/免疫/住院/手术/化验/影像）
 */
@RestController
@RequestMapping("/pet/archive")
public class PetArchiveController {

    private final IPetArchiveService petArchiveService;
    private final com.mix.mapper.PetMapper petMapper;

    @Autowired
    public PetArchiveController(IPetArchiveService petArchiveService,
                                com.mix.mapper.PetMapper petMapper) {
        this.petArchiveService = petArchiveService;
        this.petMapper = petMapper;
    }

    /** 顾客只能看自己名下的宠物；员工端不限。临时工(LOSER)不得遍历全院档案 */
    @GetMapping("/{petId}")
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER, RoleConst.ADMIN, RoleConst.CLIENT})
    public PetArchive get(@PathVariable Integer petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) return null;
        if (UserUtil.getUser() instanceof Client client
                && !client.getClientId().equals(pet.getClientId())) return null;
        return petArchiveService.get(petId);
    }
}
