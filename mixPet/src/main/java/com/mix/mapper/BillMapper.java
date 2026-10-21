package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.model.dto.BillDto;
import com.mix.model.entity.Bill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账单 Mapper
 */
@Mapper
public interface BillMapper extends BaseMapper<Bill> {
    /** 收费列表分页（联查宠物/客户/医生名、客户余额） */
    IPage<BillDto> selectBillPage(IPage<BillDto> page, String seaName, Integer payStatus);
}
