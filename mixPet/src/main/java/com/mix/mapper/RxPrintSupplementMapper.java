package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mix.model.entity.RxPrintSupplement;
import com.mix.model.vo.RxArchiveVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 处方笺打印补充 Mapper（print_no / prescription_id 均有唯一索引，编号生成靠冲突重试）
 */
@Mapper
public interface RxPrintSupplementMapper extends BaseMapper<RxPrintSupplement> {
    /**
     * 处方笺档案分页（管理端）：联处方/病历/客户/宠物/医生
     */
    Page<RxArchiveVo> selectRxArchive(Page<RxArchiveVo> page,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("doctorId") Integer doctorId,
                                      @Param("keyword") String keyword,
                                      @Param("printed") Boolean printed,
                                      @Param("status") String status);
}

