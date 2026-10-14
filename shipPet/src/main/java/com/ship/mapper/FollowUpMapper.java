package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ship.model.dto.FollowUpDto;
import com.ship.model.entity.FollowUp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 随访记录 Mapper
 */
@Mapper
public interface FollowUpMapper extends BaseMapper<FollowUp> {
    IPage<FollowUpDto> selectFollowPage(Page<FollowUpDto> page,
                                        @Param("status") Boolean status,
                                        @Param("seaName") String seaName);
}
