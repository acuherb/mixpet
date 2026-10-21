package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.SurgeryDto;
import com.mix.model.entity.AnesthesiaRecord;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Surgery;
import com.mix.model.vo.SurgeryVo;

import java.util.List;

/**
 * 手术 Service
 */
public interface ISurgeryService extends IService<Surgery> {
    /** 新建手术单（排程，含计费） */
    Result createOrder(SurgeryVo vo, Doctor doctor);

    /** 分页 */
    IPage<SurgeryDto> selectPage(Integer status, int numPage, int pageSize);

    /** 某病历的手术单 */
    List<SurgeryDto> selectByRecord(Integer recordId);

    /** 手术单详情（含麻醉记录） */
    SurgeryDto selectDetail(Integer surgeryId);

    /** 更新手术单（术前/术中/术后/状态/时间） */
    Result update(Surgery surgery);

    /** 保存/更新麻醉记录（一手术一条） */
    Result saveAnesthesia(AnesthesiaRecord ar);
}
