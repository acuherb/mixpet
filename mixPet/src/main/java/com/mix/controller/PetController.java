package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.dto.PetDto;
import com.mix.model.entity.Client;
import com.mix.model.entity.Pet;
import com.mix.model.dto.NameDo;
import com.mix.security.model.RoleConst;
import com.mix.service.AvatarFileService;
import com.mix.service.IPetService;
import com.mix.service.IPetTransferService;
import com.mix.model.vo.TransferVo;
import com.mix.model.entity.PetTransfer;
import com.mix.util.StrUtil;
import com.mix.util.UserUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 宠物信息表，外键用户表 前端控制器
 * </p>
 *
 * @author MixJade
 * @since 2022-12-22
 */
@RestController
@RequestMapping("/pet")
@Validated
public class PetController {
    private final IPetService petService;
    private final IPetTransferService petTransferService;
    private final AvatarFileService avatarFileService;

    @Autowired
    public PetController(IPetService petService, IPetTransferService petTransferService,
                         AvatarFileService avatarFileService) {
        this.petService = petService;
        this.petTransferService = petTransferService;
        this.avatarFileService = avatarFileService;
    }

    /**
     * 添加寄养、挂号单时，查询主人对应的宠物
     */
    @GetMapping("/client")
    @Secured(RoleConst.NURSE)
    public List<NameDo> getByClient(Integer clientId, Integer petId) {
        return petService.selectByClient(clientId, petId);
    }

    /**
     * 用户个人中心：查询名下宠物
     *
     * @return 宠物列表
     */
    @GetMapping("/clientOne")
    public List<Pet> getByClientOne() {
        if (UserUtil.getUser() instanceof Client client) {
            return petService.lambdaQuery()
                    .eq(Pet::getClientId, client.getClientId())
                    .list();
        } else return null;
    }

    /**
     * 管理员：添加领养订单时，查询无主宠物
     *
     * @return 无主宠物的姓名与ID
     */
    @GetMapping("/noClient")
    @Secured({RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public List<NameDo> getNoClient() {
        return petService.selectNoClient();
    }

    /**
     * 挂号检索：输入 宠物名/主人姓名/手机号/宠物卡号 任意一个关键词，
     * 返回宠物+主人轻量信息；选中后前端用 /pet/archive/{petId} 带出完整档案。
     */
    @GetMapping("/search")
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER, RoleConst.ADMIN})
    public List<PetDto> searchReg(String kw) {
        return petService.searchReg(kw);
    }

    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER})
    public IPage<PetDto> getPage(int numPage, int pageSize, String petName, String clientName) {
        return petService.selectByPage(petName, clientName, numPage, pageSize);
    }

    /**
     * 用户前台查询待领养宠物
     *
     * @return 待领养宠物列表
     */
    @GetMapping("/four")
    public IPage<Pet> getFour(int numPage, int pageSize) {
        return petService.selectFour(numPage, pageSize);
    }

    /**
     * 用户查看待领养宠物的详情
     *
     * @param petId 待领养宠物的ID
     * @return 宠物信息
     */
    @GetMapping("/one")
    public Pet getOne(Integer petId) {
        Pet pet = petService.getById(petId);
        if (pet == null || (pet.getIsDel() != null && pet.getIsDel() == 1)) return null;
        // 顾客只可查看两种宠物：自己名下的，或院内公开的待领养宠物(clientId 为空)
        if (UserUtil.getUser() instanceof Client client) {
            boolean own = client.getClientId() != null && client.getClientId().equals(pet.getClientId());
            boolean adoptable = pet.getClientId() == null;
            if (!own && !adoptable) return null;
        }
        // 内部员工(医生/护士/管理员等)可查看任意宠物
        return pet;
    }

    @PostMapping
    @Secured(RoleConst.NURSE)
    public Result save(@RequestBody Pet pet) {
        if (StrUtil.isWhite(pet.getPetName())) return Result.error("姓名不能为空");
        if (StrUtil.isWhite(pet.getPetVariety())) return Result.error("品种不能为空");
        return Result.choice("添加", petService.addPet(pet));
    }

    /**
     * 顾客自己添加宠物：主人强制为登录人，来源标记为顾客自建，默认待审核（active=0），管理员审核通过后才可挂号
     */
    @PostMapping("/self")
    public Result saveSelf(@RequestBody Pet pet) {
        if (StrUtil.isWhite(pet.getPetName())) return Result.error("姓名不能为空");
        if (StrUtil.isWhite(pet.getPetVariety())) return Result.error("品种不能为空");
        if (UserUtil.getUser() instanceof Client client) {
            pet.setClientId(client.getClientId());
            pet.setPetSource(1);
            pet.setActive(false);
            return Result.choice("添加（待审核）", petService.addPet(pet));
        } else return Result.error("请先登录顾客账号");
    }

    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return petService.deleteOrDeactivate(id);
    }

    @DeleteMapping("/batch/{ids}")
    @Secured(RoleConst.MANAGER)
    public Result deleteGroup(@PathVariable List<Integer> ids) {
        return petService.deleteBatchSmart(ids);
    }

    @PutMapping
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER})
    public Result update(@RequestBody Pet pet) {
        if (StrUtil.isWhite(pet.getPetName())) return Result.error("姓名不能为空");
        if (StrUtil.isWhite(pet.getPetVariety())) return Result.error("品种不能为空");
        // 更换头像时删除旧头像文件
        Pet oldPet = petService.getById(pet.getPetId());
        if (oldPet != null) avatarFileService.removeOldAvatar(oldPet.getPetPhoto(), pet.getPetPhoto());
        return Result.choice("修改", petService.updatePet(pet));
    }

    /**
     * 检查宠物名下关联记录
     */
    @GetMapping("/ref/{id}")
    @Secured(RoleConst.MANAGER)
    public Result checkRef(@PathVariable Integer id) {
        java.util.Map<String, Long> m = petService.countRefById(id);
        long ap = m.getOrDefault("appointCount", 0L);
        long fo = m.getOrDefault("fosterCount", 0L);
        long ad = m.getOrDefault("adoptCount", 0L);
        long tr = m.getOrDefault("transferCount", 0L);
        long total = ap + fo + ad + tr;
        if (total == 0) return Result.success("可彻底删除");
        StringBuilder sb = new StringBuilder();
        if (ap > 0) sb.append(ap).append(" 条预约 ");
        if (fo > 0) sb.append(fo).append(" 条寄养 ");
        if (ad > 0) sb.append(ad).append(" 条领养单 ");
        if (tr > 0) sb.append(tr).append(" 条过户记录");
        return Result.error("该宠物有 " + sb.toString().trim() + "，不能彻底删除，将改为停用");
    }

    /**
     * 停用宠物
     */
    @PutMapping("/deactivate/{id}")
    @Secured(RoleConst.MANAGER)
    public Result deactivate(@PathVariable Integer id) {
        Pet db = petService.getById(id);
        if (db == null) return Result.error("宠物不存在");
        db.setActive(false);
        return Result.choice("停用", petService.updateById(db));
    }

    /**
     * 启用宠物
     */
    @PutMapping("/activate/{id}")
    @Secured(RoleConst.MANAGER)
    public Result activate(@PathVariable Integer id) {
        Pet db = petService.getById(id);
        if (db == null) return Result.error("宠物不存在");
        db.setActive(true);
        return Result.choice("启用", petService.updateById(db));
    }

    /**
     * 宠物过户
     */
    @PutMapping("/transfer")
    @Secured(RoleConst.MANAGER)
    public Result transfer(@RequestBody TransferVo vo) {
        if (vo.petId() == null) return Result.error("宠物不能为空");
        if (vo.toClientId() == null || vo.toClientId() == 0) return Result.error("请选择新主人");
        Pet dbPet = petService.getById(vo.petId());
        if (dbPet == null) return Result.error("宠物不存在");
        if (dbPet.getClientId() != null && dbPet.getClientId().equals(vo.toClientId()))
            return Result.error("新主人与当前主人相同");
        return Result.choice("过户", petTransferService.transfer(vo));
    }

    /**
     * 查询宠物的过户历史
     */
    @GetMapping("/transfer/{petId}")
    @Secured(RoleConst.NURSE)
    public List<PetTransfer> transferHistory(@PathVariable Integer petId) {
        return petTransferService.history(petId);
    }

    /**
     * 宠物入栏：有主变无主（主人遗弃/托管）
     */
    @PutMapping("/intake/{id}")
    @Secured(RoleConst.MANAGER)
    public Result intake(@PathVariable Integer id,
                         @RequestParam(required = false) String reason) {
        return petService.intake(id, reason);
    }
}