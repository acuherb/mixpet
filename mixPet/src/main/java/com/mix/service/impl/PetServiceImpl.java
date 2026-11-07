package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.PhotoEnum;
import com.mix.common.Result;
import com.mix.common.BizException;
import com.mix.mapper.PetMapper;
import com.mix.mapper.PetTransferMapper;
import com.mix.model.dto.PetDto;
import com.mix.model.entity.Pet;
import com.mix.model.entity.PetTransfer;
import com.mix.model.dto.NameDo;
import com.mix.service.IPetService;
import com.mix.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 宠物信息表，外键用户表 服务实现类
 * </p>
 *
 * @author MixJade
 * @since 2022-12-22
 */
@Service
public class PetServiceImpl extends ServiceImpl<PetMapper, Pet> implements IPetService {

    private final PetTransferMapper petTransferMapper;

    @Autowired
    public PetServiceImpl(PetTransferMapper petTransferMapper) {
        this.petTransferMapper = petTransferMapper;
    }

    @Override
    public boolean deleteById(Integer petId) {
        return this.lambdaUpdate()
                .eq(Pet::getPetId, petId)
                .set(Pet::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public boolean deleteByIds(List<Integer> idGroup) {
        return this.lambdaUpdate()
                .in(Pet::getPetId, idGroup)
                .set(Pet::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public IPage<PetDto> selectByPage(String petName, String clientName, int numPage, int pageSize) {
        return baseMapper.selectPetPage(new Page<>(numPage, pageSize), petName, clientName);
    }

    @Override
    public IPage<Pet> selectFour(int numPage, int pageSize) {
        return baseMapper.selectFour(new Page<>(numPage, pageSize));
    }

    @Override
    public boolean updatePet(Pet pet) {
        return baseMapper.updatePet(pet);
    }

    @Override
    public List<NameDo> selectName() {
        return baseMapper.selectName();
    }

    @Override
    public List<NameDo> selectByClient(Integer clientId, Integer petId) {
        return baseMapper.selectByClient(clientId, petId);
    }

    @Override
    public List<NameDo> selectNoClient() {
        return baseMapper.selectNoClient();
    }

    @Override
    public boolean addPet(Pet pet) {
        if (StrUtil.isWhite(pet.getPetPhoto())) pet.setPetPhoto(PhotoEnum.PET.getPhotoName());
        if (StrUtil.isWhite(pet.getPetInfo())) pet.setPetInfo("暂无近况");
        if (StrUtil.isWhite(pet.getPetStatus())) pet.setPetStatus("健康");
        return save(pet);
    }

    @Override
    public java.util.Map<String, Long> countRefById(Integer id) {
        return baseMapper.countRefById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteOrDeactivate(Integer petId) {
        Pet pet = getById(petId);
        if (pet == null) throw new BizException("宠物不存在");
        Map<String, Long> m = baseMapper.countRefById(petId);
        long total = m.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            this.lambdaUpdate()
                    .eq(Pet::getPetId, petId)
                    .set(Pet::getIsDel, System.currentTimeMillis())
                    .update();
            return Result.success("已彻底删除");
        } else {
            this.lambdaUpdate()
                    .eq(Pet::getPetId, petId)
                    .set(Pet::getActive, false)
                    .update();
            return Result.success("有 " + total + " 条历史记录，已改为停用（保留档案）");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteBatchSmart(List<Integer> ids) {
        int deleted = 0, deactivated = 0;
        for (Integer id : ids) {
            Pet pet = getById(id);
            if (pet == null) continue;
            Map<String, Long> m = baseMapper.countRefById(id);
            long total = m.values().stream().mapToLong(Long::longValue).sum();
            if (total == 0) {
                this.lambdaUpdate()
                        .eq(Pet::getPetId, id)
                        .set(Pet::getIsDel, System.currentTimeMillis())
                        .update();
                deleted++;
            } else {
                this.lambdaUpdate()
                        .eq(Pet::getPetId, id)
                        .set(Pet::getActive, false)
                        .update();
                deactivated++;
            }
        }
        return Result.success("彻底删除 " + deleted + " 只，停用 " + deactivated + " 只");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result intake(Integer petId, String reason) {
        Pet pet = getById(petId);
        if (pet == null) throw new BizException("宠物不存在");
        if (pet.getClientId() == null) throw new BizException("该宠物已是无主状态，无需入栏");
        Integer fromClientId = pet.getClientId();
        // MP 的 updateById 默认跳过 null 字段，清空主人必须显式 set null
        this.lambdaUpdate()
                .eq(Pet::getPetId, petId)
                .set(Pet::getClientId, null)
                .update();
        // 写一条入栏记录（有主 → 无主）
        PetTransfer t = new PetTransfer();
        t.setPetId(petId);
        t.setFromClientId(fromClientId);
        t.setToClientId(null);
        t.setTransferDate(LocalDateTime.now());
        String r = (reason == null || reason.isBlank()) ? "主人托管" : reason;
        t.setReason("入栏：" + r);
        petTransferMapper.insert(t);
        return Result.success("已入栏，宠物变为无主");
    }

}
