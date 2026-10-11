package com.ship.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.model.entity.PetTransfer;
import com.ship.model.vo.TransferVo;

import java.util.List;

public interface IPetTransferService extends IService<PetTransfer> {
    /**
     * 执行宠物过户：写历史 + 改主人
     */
    boolean transfer(TransferVo vo);

    /**
     * 查某只宠物的过户历史
     */
    List<PetTransfer> history(Integer petId);
}
