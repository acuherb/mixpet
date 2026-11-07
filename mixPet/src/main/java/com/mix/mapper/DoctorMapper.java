package com.mix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.model.dto.DoctorDto;
import com.mix.model.entity.Doctor;
import com.mix.model.dto.NameDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 医生表，外键部门 Mapper 接口
 * </p>
 *
 * @author MixJade
 * @since 2023-01-02
 */
@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {
    IPage<DoctorDto> selectDoctorPage(IPage<DoctorDto> page, String doctorName, String departmentName, Boolean certNull);

    /**
     * 顾客端展示用分页，脱敏：不返回 phone/email/username 等敏感字段
     */
    IPage<DoctorDto> selectDoctorPagePublic(IPage<DoctorDto> page, String doctorName, String departmentName);

    DoctorDto selectOneById(@Param("doctorId") Integer doctorId);

    @Select("SELECT doctor_id as roleId,doctor_name as roleName FROM doctor WHERE is_del=0")
    List<NameDo> selectName();

    @Select("SELECT doctor_id as roleId, doctor_name as roleName FROM doctor WHERE is_del = '0' AND department_id=#{departmentId}")
    List<NameDo> selectNameByDepartment(Integer departmentId);

    List<Doctor> selectByDepartmentId(Integer departmentId);

    boolean updateSelf(Doctor doctor);

    /** 号源并发控制：锁住医生主键行，使同一医生的挂号/改期串行化 */
    @Select("SELECT doctor_id FROM doctor WHERE doctor_id = #{doctorId} FOR UPDATE")
    Integer lockDoctor(@Param("doctorId") Integer doctorId);
}