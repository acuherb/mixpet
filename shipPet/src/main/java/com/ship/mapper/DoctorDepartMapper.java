package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ship.model.entity.DoctorDepart;
import com.ship.model.dto.NameDo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 医生 ⇄ 科室 关系 Mapper
 */
@Mapper
public interface DoctorDepartMapper extends BaseMapper<DoctorDepart> {

    /**
     * 删除某医生的全部科室关系
     */
    @Delete("DELETE FROM doctor_department WHERE doctor_id = #{doctorId}")
    int deleteByDoctorId(Integer doctorId);

    /**
     * 查询某医生的科室id列表
     */
    @Select("SELECT department_id FROM doctor_department WHERE doctor_id = #{doctorId}")
    List<Integer> selectDepartIdsByDoctorId(Integer doctorId);

    /**
     * 通过科室查医生（挂号选医生用）——返回姓名与id
     */
    @Select("SELECT d.doctor_id AS roleId, d.doctor_name AS roleName " +
            "FROM doctor_department dd " +
            "JOIN doctor d ON dd.doctor_id = d.doctor_id " +
            "WHERE dd.department_id = #{departmentId} AND d.is_del = 0")
    List<NameDo> selectDoctorNamesByDepartmentId(Integer departmentId);

    /**
     * 通过科室查医生（科室详情用）——返回医生实体
     */
    @Select("SELECT d.doctor_id, d.username, d.doctor_name, d.doctor_gender, d.doctor_age, d.doctor_tel, d.auth_lv " +
            "FROM doctor_department dd " +
            "JOIN doctor d ON dd.doctor_id = d.doctor_id " +
            "WHERE dd.department_id = #{departmentId} AND d.is_del = 0")
    List<com.ship.model.entity.Doctor> selectDoctorsByDepartmentId(Integer departmentId);

    /**
     * 查询某医生关联的全部科室名（医生详情/列表展示用）
     */
    @Select("SELECT dep.department_id AS roleId, dep.department_name AS roleName " +
            "FROM doctor_department dd " +
            "JOIN department dep ON dd.department_id = dep.department_id " +
            "WHERE dd.doctor_id = #{doctorId} AND dep.is_del = 0")
    List<NameDo> selectDepartNamesByDoctorId(Integer doctorId);

    /**
     * 统计每个科室的医生数（科室列表用）
     */
    @Select("SELECT department_id, COUNT(DISTINCT doctor_id) AS cnt FROM doctor_department GROUP BY department_id")
    List<com.ship.model.dto.DepartCountDto> selectDoctorCountByDepart();
}