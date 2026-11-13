package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.QuarantineCert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检疫单 Mapper（cert_no 唯一索引，编号生成靠冲突重试）
 */
@Mapper
public interface QuarantineCertMapper extends BaseMapper<QuarantineCert> {
}
