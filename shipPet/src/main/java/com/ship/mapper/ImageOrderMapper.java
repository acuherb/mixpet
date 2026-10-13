package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.model.dto.ImageOrderDto;
import com.ship.model.entity.ImageOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 影像申请单 Mapper
 */
@Mapper
public interface ImageOrderMapper extends BaseMapper<ImageOrder> {
    /** 申请单分页联查 */
    IPage<ImageOrderDto> selectImageOrderPage(IPage<ImageOrderDto> page, @Param("status") Boolean status);

    /** 某病历的申请单 */
    java.util.List<ImageOrderDto> selectByRecord(@Param("recordId") Integer recordId);
}