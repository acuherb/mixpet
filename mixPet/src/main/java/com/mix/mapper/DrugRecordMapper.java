package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.DrugRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品出入库台账 Mapper
 */
@Mapper
public interface DrugRecordMapper extends BaseMapper<DrugRecord> {
}
