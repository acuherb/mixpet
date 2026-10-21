package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.model.dto.SurgeryDto;
import com.mix.model.entity.Surgery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 手术单 Mapper
 */
@Mapper
public interface SurgeryMapper extends BaseMapper<Surgery> {
    /** 分页联查（宠物/客户/医生名） */
    IPage<SurgeryDto> selectSurgeryPage(IPage<SurgeryDto> page, @Param("status") Integer status);

    /** 某病历的手术单 */
    List<SurgeryDto> selectByRecord(@Param("recordId") Integer recordId);

    /** 单个手术单（联查名） */
    SurgeryDto selectOneById(@Param("surgeryId") Integer surgeryId);
}
