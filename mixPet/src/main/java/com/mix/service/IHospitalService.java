package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.HospitalDto;
import com.mix.model.entity.HospitalRecord;
import com.mix.model.entity.Doctor;
import com.mix.model.vo.HospitalFeeVo;
import com.mix.model.vo.HospitalVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 住院 Service
 */
public interface IHospitalService extends IService<HospitalRecord> {
    /** 入院登记（床位/护理/输液分项单价） */
    Result saveIn(HospitalVo vo, Doctor doctor);

    /** 出院结算：天数×(床位+护理+输液) + 医嘱临时费用计入账单；daysOverride 可手改天数 */
    Result discharge(Integer hospitalId, Integer daysOverride);

    /** 收取押金（累加，记录收取人/时间） */
    Result addDeposit(Integer hospitalId, BigDecimal amount, Doctor operator);

    /** 费用预览（结算弹窗打开时拉明细：分项小计/医嘱费/押金/应退） */
    HospitalFeeVo feePreview(Integer hospitalId);

    /** 住院分页 */
    IPage<HospitalDto> selectPage(Boolean status, int numPage, int pageSize);

    /** 某宠物的住院记录 */
    List<HospitalDto> selectByPet(Integer petId);
}