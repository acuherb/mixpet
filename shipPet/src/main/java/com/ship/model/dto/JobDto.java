package com.ship.model.dto;

import com.ship.model.entity.Job;

/**
 * 职称字典（联查该职称下的医生数）
 */
public class JobDto extends Job {
    /** 该职称下的医生数量 */
    private Integer doctorNum;

    public Integer getDoctorNum() {
        return doctorNum;
    }

    public void setDoctorNum(Integer doctorNum) {
        this.doctorNum = doctorNum;
    }
}
