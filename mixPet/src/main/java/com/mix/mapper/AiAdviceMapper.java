package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mix.model.entity.AiAdvice;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 辅助诊疗留痕 Mapper
 */
@Mapper
public interface AiAdviceMapper extends BaseMapper<AiAdvice> {
}
