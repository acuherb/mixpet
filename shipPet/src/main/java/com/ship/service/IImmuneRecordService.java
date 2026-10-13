package com.ship.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.common.Result;
import com.ship.model.dto.ImmuneRecordDto;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.ImmuneRecord;
import com.ship.model.vo.ImmuneVo;

import java.util.List;

/**
 * 接种档案 Service
 */
public interface IImmuneRecordService extends IService<ImmuneRecord> {
    /** 接种登记（自动计算下一针时间） */
    Result saveImmune(ImmuneVo vo, Doctor doctor);

    /** 档案分页 */
    IPage<ImmuneRecordDto> selectPage(String petName, Boolean dueSoon, int numPage, int pageSize);

    /** 某宠物的档案 */
    List<ImmuneRecordDto> selectByPet(Integer petId);
}