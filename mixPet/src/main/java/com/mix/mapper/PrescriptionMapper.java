package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 处方明细 Mapper
 */
@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {
    /** 某病历的全部处方明细 */
    @Select("SELECT * FROM prescription WHERE record_id = #{recordId} AND is_del = 0 AND is_cancel = 0 ORDER BY prescription_id")
    List<Prescription> selectByRecord(Integer recordId);
}
