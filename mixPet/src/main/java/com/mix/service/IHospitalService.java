package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.HospitalDto;
import com.mix.model.entity.HospitalRecord;
import com.mix.model.entity.Doctor;
import com.mix.model.vo.HospitalVo;

import java.util.List;

/**
 * 住院 Service
 */
public interface IHospitalService extends IService<HospitalRecord> {
    /** 入院登记 */
    Result saveIn(HospitalVo vo, Doctor doctor);

    /** 出院结算：按住院天数×日均费计入账单 */
    Result discharge(Integer hospitalId);

    /** 住院分页 */
    IPage<HospitalDto> selectPage(Boolean status, int numPage, int pageSize);

    /** 某宠物的住院记录 */
    List<HospitalDto> selectByPet(Integer petId);
}