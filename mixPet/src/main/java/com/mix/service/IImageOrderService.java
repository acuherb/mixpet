package com.mix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.dto.ImageOrderDto;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.ImageOrder;
import com.mix.model.vo.ImageOrderVo;
import com.mix.model.vo.ImageReportVo;
import com.mix.model.vo.RequisitionVo;

import java.util.List;

/**
 * 影像申请单 Service
 */
public interface IImageOrderService extends IService<ImageOrder> {
    /** 医生开单（自动计费入账单） */
    Result createOrder(ImageOrderVo vo, Doctor doctor);

    /** 影像报告录入 */
    Result saveReport(ImageReportVo vo);

    /** 申请单分页 */
    IPage<ImageOrderDto> selectPage(Boolean status, int numPage, int pageSize);

    /** 某病历的申请单 */
    List<ImageOrderDto> selectByRecord(Integer recordId);

    /** 影像申请单（国标，可打印）：聚合申请单+病历+宠物+医生 */
    RequisitionVo selectRequisition(Integer orderId);

    /** 作废申请单（未出报告）：冲减未付账单的影像费 */
    Result cancel(Integer orderId);
}