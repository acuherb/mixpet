package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.model.dto.HospitalDto;
import com.mix.model.entity.HospitalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 住院记录 Mapper
 */
@Mapper
public interface HospitalMapper extends BaseMapper<HospitalRecord> {
    /** 住院分页联查 */
    IPage<HospitalDto> selectHospitalPage(IPage<HospitalDto> page, @Param("status") Boolean status);

    /** 某宠物的住院记录 */
    List<HospitalDto> selectByPet(@Param("petId") Integer petId);
}