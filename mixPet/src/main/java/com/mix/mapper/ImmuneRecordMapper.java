package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.model.dto.ImmuneRecordDto;
import com.mix.model.entity.ImmuneRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 接种档案 Mapper
 */
@Mapper
public interface ImmuneRecordMapper extends BaseMapper<ImmuneRecord> {
    /** 接种档案分页（可按宠物名、是否临期筛选） */
    IPage<ImmuneRecordDto> selectPage(IPage<ImmuneRecordDto> page,
                                      @Param("petName") String petName,
                                      @Param("dueSoon") Boolean dueSoon);

    /** 某宠物的接种档案 */
    List<ImmuneRecordDto> selectByPet(@Param("petId") Integer petId);

    /** 接种提醒：下一针在30天内的档案 */
    List<ImmuneRecordDto> selectDueVaccines();
}