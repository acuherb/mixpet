package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.model.dto.FosterDto;
import com.mix.model.dto.FosterPetDto;
import com.mix.model.entity.Foster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 寄养表 Mapper 接口
 * </p>
 *
 * @author MixJade
 * @since 2023-01-12
 */
@Mapper
public interface FosterMapper extends BaseMapper<Foster> {
    IPage<FosterDto> selectFosterPage(IPage<FosterDto> page, String clientName, String petName);

    @Select("select max(foster_id) from foster")
    Integer getMaxId();

    IPage<FosterPetDto> fosterPet(IPage<FosterDto> page);

    /**
     * 更新寄养表，且不更新订单号
     *
     * @param foster 寄养信息
     * @return 修改成功
     */
    boolean updateFoster(Foster foster);

    /** 顾客「我的寄养」列表，强制按 client_id 过滤 */
    List<FosterDto> selfFoster(@Param("clientId") Integer clientId);

    /** 顾客单份寄养详情，越权返回 null */
    FosterDto selfOne(@Param("fosterId") Integer fosterId, @Param("clientId") Integer clientId);

    /** 员工单份寄养详情（打印结算单） */
    FosterDto staffOne(@Param("fosterId") Integer fosterId);
}
