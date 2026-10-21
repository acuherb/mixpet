package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.entity.SurgeryConsent;
import com.mix.model.vo.ConsentSheetVo;
import com.mix.model.vo.SurgeryConsentVo;

/**
 * 手术知情同意书 Service
 */
public interface ISurgeryConsentService extends IService<SurgeryConsent> {
    /** 签署/更新同意书（一手术一条；需同意并签字） */
    Result sign(SurgeryConsentVo vo, String operator);

    /** 某手术的同意书 */
    SurgeryConsent getBySurgery(Integer surgeryId);

    /** 可打印的同意书数据（聚合手术+宠物+主人+医生+病历+同意书） */
    ConsentSheetVo consentSheet(Integer surgeryId);
}
