package com.ship.model.dto;

import com.ship.model.entity.Doctor;

import java.util.List;

public class DoctorDto extends Doctor {
    /**
     * 主科室名（兼容旧逻辑，可空）
     */
    private String departmentName;

    /**
     * 医生关联的多科室id（新增/编辑用）
     */
    private List<Integer> departmentIds;

    /**
     * 医生关联的多科室名（列表/详情展示用）
     */
    private List<String> departmentNames;

    /**
     * 职称名称（联查 job 字典，展示用）
     */
    private String jobName;

    /**
     * 执业兽医资格证号（处方笺用）
     */
    private String doctorCert;

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public List<Integer> getDepartmentIds() {
        return departmentIds;
    }

    public void setDepartmentIds(List<Integer> departmentIds) {
        this.departmentIds = departmentIds;
    }

    public List<String> getDepartmentNames() {
        return departmentNames;
    }

    public void setDepartmentNames(List<String> departmentNames) {
        this.departmentNames = departmentNames;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getDoctorCert() {
        return doctorCert;
    }

    public void setDoctorCert(String doctorCert) {
        this.doctorCert = doctorCert;
    }
}