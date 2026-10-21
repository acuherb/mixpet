package com.mix.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.Result;
import com.mix.mapper.AdoptMapper;
import com.mix.model.dto.AdoptDto;
import com.mix.model.entity.Adopt;
import com.mix.model.vo.AdoptVo;
import com.mix.model.entity.Pet;
import com.mix.model.entity.PetTransfer;
import com.mix.mapper.PetMapper;
import com.mix.mapper.PetTransferMapper;
import com.mix.service.IAdoptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;

/**
 * <p>
 * 领养宠物订单 服务实现类
 * </p>
 *
 * @author MixJade
 * @since 2023-01-12
 */
@Service
public class AdoptServiceImpl extends ServiceImpl<AdoptMapper, Adopt> implements IAdoptService {

    private final PetMapper petMapper;
    private final PetTransferMapper petTransferMapper;

    @Autowired
    public AdoptServiceImpl(PetMapper petMapper, PetTransferMapper petTransferMapper) {
        this.petMapper = petMapper;
        this.petTransferMapper = petTransferMapper;
    }

    @Override
    public boolean deleteById(Integer adoptId) {
        return this.lambdaUpdate()
                .eq(Adopt::getAdoptId, adoptId)
                .set(Adopt::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public boolean deleteByIds(List<Integer> idGroup) {
        return this.lambdaUpdate()
                .in(Adopt::getAdoptId, idGroup)
                .set(Adopt::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public IPage<AdoptDto> selectByPage(String clientName, String petName, int numPage, int pageSize) {
        return baseMapper.selectAdoptPage(new Page<>(numPage, pageSize), clientName, petName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result adoptAdopt(AdoptVo adoptVo) {
        // 查看是否有主
        if (baseMapper.petMaster(adoptVo.adoptId()) > 0) return Result.error("此物有主");
        // 无主则判断是否正常操作
        if (adoptVo.isAdopt() == null) return Result.error("你不对劲");
        // 正常操作则进行同意or拒绝
        if (adoptVo.isAdopt()) {
            String petInfo = "它有了温暖的家，感恩";
            // 1. 先查出领养单，拿到 petId 和 新主人 clientId
            Adopt adopt = this.getById(adoptVo.adoptId());
            if (adopt == null) return Result.error("领养单不存在");
            // 2. 原主人（应为 null）
            Pet pet = petMapper.selectById(adopt.getPetId());
            Integer fromClientId = pet == null ? null : pet.getClientId();
            // 3. 执行原 SQL（改 pet.client_id + 标记领养单）
            boolean ok = baseMapper.adoptAdopt(adoptVo.adoptId(), petInfo);
            if (!ok) return Result.choice("领养", false);
            // 4. 补写一条过户记录（从"无主"到新主人）
            PetTransfer transfer = new PetTransfer();
            transfer.setPetId(adopt.getPetId());
            transfer.setFromClientId(fromClientId);
            transfer.setToClientId(adopt.getClientId());
            transfer.setTransferDate(LocalDateTime.now());
            // 带上申请理由，方便过户记录里看到上下文
            String applyInfo = adopt.getAdoptInfo();
            transfer.setReason(
                    (applyInfo == null || applyInfo.isBlank())
                            ? "领养"
                            : "领养：" + applyInfo);
            petTransferMapper.insert(transfer);
            return Result.choice("领养", true);
        } else {
            String reason = adoptVo.refuseReason();
            if (reason == null || reason.isBlank()) reason = "未填写理由";
            return Result.choice("拒绝", baseMapper.refuse(adoptVo.adoptId(), reason));
        }
    }

    @Override
    public int sureInAdopt(Integer petId, Integer clientId) {
        return baseMapper.sureInAdopt(petId, clientId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result undoAdopt(Integer adoptId) {
        Adopt adopt = this.getById(adoptId);
        if (adopt == null) return Result.error("领养单不存在");
        if (adopt.getInAdopt() != 1) return Result.error("只有已通过的单子可以撤回");

        Pet pet = petMapper.selectById(adopt.getPetId());
        if (pet == null) return Result.error("宠物不存在");

        // 1. 宠物变回无主
        // 注意：MP 的 updateById 默认跳过 null 字段，必须用 UpdateWrapper 显式 set null
        petMapper.update(null, new LambdaUpdateWrapper<Pet>()
                .eq(Pet::getPetId, pet.getPetId())
                .set(Pet::getClientId, null));

        // 2. 领养单变回待审核
        this.lambdaUpdate()
                .eq(Adopt::getAdoptId, adoptId)
                .set(Adopt::getInAdopt, (byte) 2)
                .update();

        // 3. 写一条撤回记录
        PetTransfer t = new PetTransfer();
        t.setPetId(adopt.getPetId());
        t.setFromClientId(adopt.getClientId());
        t.setToClientId(null);
        t.setTransferDate(LocalDateTime.now());
        t.setReason("撤回领养");
        petTransferMapper.insert(t);

        return Result.choice("撤回", true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result returnPet(Integer petId, Integer clientId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null) return Result.error("宠物不存在");
        if (!clientId.equals(pet.getClientId())) return Result.error("该宠物不在您名下");

        // 1. 宠物变回无主（MP 的 updateById 默认跳过 null，必须显式 set null）
        petMapper.update(null, new LambdaUpdateWrapper<Pet>()
                .eq(Pet::getPetId, petId)
                .set(Pet::getClientId, null));

        // 2. 该顾客此宠物已通过的领养单标记为已退养（3）
        this.lambdaUpdate()
                .eq(Adopt::getPetId, petId)
                .eq(Adopt::getClientId, clientId)
                .eq(Adopt::getInAdopt, (byte) 1)
                .eq(Adopt::getIsDel, 0L)
                .set(Adopt::getInAdopt, (byte) 3)
                .update();

        // 3. 写一条退养记录
        PetTransfer t = new PetTransfer();
        t.setPetId(petId);
        t.setFromClientId(clientId);
        t.setToClientId(null);
        t.setTransferDate(LocalDateTime.now());
        t.setReason("顾客退养");
        petTransferMapper.insert(t);

        return Result.success("退养成功，宠物已回到待领养");
    }

    @Override
    public Result restartAdopt(Integer adoptId) {
        Adopt adopt = this.getById(adoptId);
        if (adopt == null) return Result.error("领养单不存在");
        if (adopt.getInAdopt() != 0) return Result.error("只有已拒绝的单子可以重启");
        return Result.choice("重启", baseMapper.restart(adoptId));
    }

}
