package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.LabOrderDto;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.LabOrder;
import com.mix.model.vo.LabOrderVo;
import com.mix.model.vo.LabResultVo;
import com.mix.model.vo.RequisitionVo;

import java.util.List;

/**
 * 化验单 Service
 */
public interface ILabOrderService extends IService<LabOrder> {
    /** 医生开化验单（含计费） */
    Result createOrder(LabOrderVo vo, Doctor doctor);

    /** 化验单分页 */
    IPage<LabOrderDto> selectPage(Integer status, int numPage, int pageSize);

    /** 录入化验结果 */
    Result saveResult(LabResultVo vo, String operator);

    /** 某病历的化验单（含明细） */
    List<LabOrderDto> selectByRecord(Integer recordId);

    /** 检验申请单（国标，可打印）：聚合化验单+病历+宠物+医生 */
    RequisitionVo selectRequisition(Integer orderId);

    /** 作废化验单（未出结果）：冲减未付账单的检验费 */
    Result cancel(Integer orderId);
}