package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.QuarantineCert;
import com.mix.model.vo.QuarantineCertVo;

/**
 * 检疫单 Service：按病历组装草稿 / 开具留痕 / 按单号补打取数
 */
public interface IQuarantineCertService extends IService<QuarantineCert> {

    /** 草稿：用病历+宠物+客户+医生+处方明细预填（已有单据则直接返回最近一张） */
    QuarantineCertVo draft(Integer recordId);

    /** 开具并保存（生成 JY-YYYYMMDD-XXXX 单号） */
    QuarantineCertVo issue(QuarantineCertVo vo);

    /** 按单号取（补打） */
    QuarantineCertVo selectByNo(String certNo);
}
