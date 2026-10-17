package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.AiAdvice;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 辅助诊疗留痕 Mapper
 */
@Mapper
public interface AiAdviceMapper extends BaseMapper<AiAdvice> {
}
