package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.model.dto.RecordDto;
import com.ship.model.entity.MedicalRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电子病历 Mapper
 */
@Mapper
public interface MedicalRecordMapper extends BaseMapper<MedicalRecord> {
    /** 宠物的就诊史（联查宠物/客户/医生/科室名与账单费用） */
    IPage<RecordDto> selectPetRecords(IPage<RecordDto> page, Integer petId);

    /** 单份病历详情（联查） */
    RecordDto selectRecordOne(Integer recordId);
}
