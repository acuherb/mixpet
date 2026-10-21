package com.mix.controller;

import com.mix.model.entity.Client;
import com.mix.model.entity.Pet;
import com.mix.model.vo.PetHealth;
import com.mix.service.IPetHealthService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 一页式宠物健康档案
 */
@RestController
@RequestMapping("/pet/health")
public class PetHealthController {
    private final IPetHealthService petHealthService;
    private final com.mix.mapper.PetMapper petMapper;

    @Autowired
    public PetHealthController(IPetHealthService petHealthService, com.mix.mapper.PetMapper petMapper) {
        this.petHealthService = petHealthService;
        this.petMapper = petMapper;
    }

    /** 宠物健康档案：顾客只能看自己名下的宠物 */
    @GetMapping("/{petId}")
    public PetHealth get(@PathVariable Integer petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) return null;
        if (UserUtil.getUser() instanceof Client client
                && !client.getClientId().equals(pet.getClientId())) return null;
        return petHealthService.get(petId);
    }
}
