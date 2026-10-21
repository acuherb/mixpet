package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.ImmuneRecordDto;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.ImmuneRecord;
import com.mix.model.vo.ImmuneVo;

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