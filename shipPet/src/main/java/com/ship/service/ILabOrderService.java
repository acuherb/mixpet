package com.ship.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.common.Result;
import com.ship.model.dto.LabOrderDto;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.LabOrder;
import com.ship.model.vo.LabOrderVo;
import com.ship.model.vo.LabResultVo;
import com.ship.model.vo.RequisitionVo;

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
}