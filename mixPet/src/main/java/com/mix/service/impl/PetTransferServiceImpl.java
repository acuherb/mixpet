package com.mix.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.PetMapper;
import com.mix.mapper.PetTransferMapper;
import com.mix.model.entity.Pet;
import com.mix.model.entity.PetTransfer;
import com.mix.model.vo.TransferVo;
import com.mix.service.IPetTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PetTransferServiceImpl extends ServiceImpl<PetTransferMapper, PetTransfer> implements IPetTransferService {

    private static final Logger log = LoggerFactory.getLogger(PetTransferServiceImpl.class);

    private final PetMapper petMapper;

    @Autowired
    public PetTransferServiceImpl(PetMapper petMapper) {
        this.petMapper = petMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transfer(TransferVo vo) {
        log.info("过户请求：{}", vo);
        Pet pet = petMapper.selectById(vo.petId());
        if (pet == null) {
            log.warn("宠物不存在：petId={}", vo.petId());
            return false;
        }

        PetTransfer t = new PetTransfer();
        t.setPetId(vo.petId());
        t.setFromClientId(pet.getClientId());
        t.setToClientId(vo.toClientId());
        t.setTransferDate(vo.transferDate() == null ? LocalDateTime.now() : vo.transferDate());
        t.setReason(vo.reason());
        save(t);

        pet.setClientId(vo.toClientId());
        petMapper.updateById(pet);
        return true;
    }

    @Override
    public List<PetTransfer> history(Integer petId) {
        return this.lambdaQuery()
                .eq(PetTransfer::getPetId, petId)
                .orderByDesc(PetTransfer::getTransferDate)
                .list();
    }
}
