package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.HospitalAdvice;

import java.util.List;

/**
 * 住院医嘱单服务
 */
public interface IHospitalAdviceService extends IService<HospitalAdvice> {

    /**
     * 下达医嘱（doctor 从登录态取；处方导入时 drugId/itemName/dose 等由调用方组装）
     */
    boolean addAdvice(HospitalAdvice advice);

    /**
     * 批量下达（从本病历处方导入，一条事务）
     */
    boolean addBatch(List<HospitalAdvice> list);

    /** 停嘱（原因必填；已停嘱/已完成不可再停） */
    String stop(Integer adviceId, String reason);

    /** 标记完成 */
    String done(Integer adviceId);

    /** 某次住院的医嘱列表（按下达时间倒序） */
    List<HospitalAdvice> listByHospital(Integer hospitalId);

    /** 某病历的医嘱（跨住院；顾客端看自己的） */
    List<HospitalAdvice> listByRecord(Integer recordId);
}
