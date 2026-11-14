package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.model.dto.AppointmentDto;
import com.mix.model.dto.AppointmentDto2;
import com.mix.model.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 挂号单表 Mapper 接口
 * </p>
 *
 * @author MixJade
 * @since 2022-12-22
 */
@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
    IPage<AppointmentDto> selectAppointmentPage(IPage<AppointmentDto> page, String seaName, int seaType);

    /**
     * 用户：根据医生查询订单
     */
    List<AppointmentDto> getDoctor(@Param("doctorId") Integer doctorId);

    /**
     * 用户：查询自己的订单
     */
    List<AppointmentDto> getClient(@Param("clientId") Integer clientId);

    /**
     * 医生：查询自己的订单
     */
    List<AppointmentDto2> getDoctorLog(@Param("doctorId") Integer doctorId);

    /**
     * 医生：查询 from（明日零点）起尚未接诊、未爽约的未来预约，按时间升序。
     * 独立于 getDoctorLog，避免待处理/历史单占满 300 条兜底额度时未来预约被截断。
     */
    List<AppointmentDto2> getFutureAppoint(@Param("doctorId") Integer doctorId,
                                           @Param("from") LocalDateTime from);
}
