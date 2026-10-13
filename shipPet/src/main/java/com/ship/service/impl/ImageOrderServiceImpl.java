package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.BillMapper;
import com.ship.mapper.DoctorMapper;
import com.ship.mapper.ImageOrderMapper;
import com.ship.mapper.ImageTestMapper;
import com.ship.mapper.PetMapper;
import com.ship.model.dto.ImageOrderDto;
import com.ship.model.entity.Bill;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.ImageOrder;
import com.ship.model.entity.ImageTest;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.entity.Pet;
import com.ship.model.vo.ImageOrderVo;
import com.ship.model.vo.ImageReportVo;
import com.ship.model.vo.RequisitionVo;
import com.ship.service.IImageOrderService;
import com.ship.service.IMedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 影像申请单 Service 实现
 */
@Service
public class ImageOrderServiceImpl extends ServiceImpl<ImageOrderMapper, ImageOrder> implements IImageOrderService {
    private final IMedicalRecordService recordService;
    private final ImageTestMapper testMapper;
    private final BillMapper billMapper;
    private final PetMapper petMapper;
    private final DoctorMapper doctorMapper;

    @Autowired
    public ImageOrderServiceImpl(IMedicalRecordService recordService, ImageTestMapper testMapper, BillMapper billMapper,
                                 PetMapper petMapper, DoctorMapper doctorMapper) {
        this.recordService = recordService;
        this.testMapper = testMapper;
        this.billMapper = billMapper;
        this.petMapper = petMapper;
        this.doctorMapper = doctorMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(ImageOrderVo vo, Doctor doctor) {
        if (vo == null || vo.getRecordId() == null) return Result.error("缺少病历信息");
        if (vo.getItemId() == null) return Result.error("请选择影像项目");
        MedicalRecord record = recordService.getById(vo.getRecordId());
        if (record == null) return Result.error("病历不存在");
        ImageTest test = testMapper.selectById(vo.getItemId());
        if (test == null || test.getIsDel() != null && test.getIsDel() == 1) return Result.error("影像项目不存在或已停用");

        ImageOrder order = new ImageOrder();
        order.setRecordId(record.getRecordId());
        order.setPetId(record.getPetId());
        order.setClientId(record.getClientId());
        order.setDoctorId(doctor.getDoctorId());
        order.setItemId(test.getItemId());
        order.setItemName(test.getItemName());
        order.setGroupName(test.getGroupName());
        order.setFee(test.getPrice());
        order.setExamPart(vo.getExamPart());
        order.setStatus(false);
        order.setNote(vo.getNote());
        boolean ok = this.save(order);
        if (!ok) return Result.error("开单失败");

        // 计费入账单
        Bill unpaid = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getRecordId, record.getRecordId())
                .eq(Bill::getPayStatus, false)
                .last("limit 1"));
        if (unpaid != null) {
            billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                    .eq(Bill::getBillId, unpaid.getBillId())
                    .setSql("fee_image = fee_image + " + order.getFee().toPlainString())
                    .setSql("total_money = total_money + " + order.getFee().toPlainString()));
        } else {
            Bill bill = new Bill();
            bill.setClientId(record.getClientId());
            bill.setPetId(record.getPetId());
            bill.setRecordId(record.getRecordId());
            bill.setFeeRegister(java.math.BigDecimal.ZERO);
            bill.setFeeTreat(java.math.BigDecimal.ZERO);
            bill.setFeeDrug(java.math.BigDecimal.ZERO);
            bill.setFeeLab(java.math.BigDecimal.ZERO);
            bill.setFeeHospital(java.math.BigDecimal.ZERO);
            bill.setFeeImage(order.getFee());
            bill.setTotalMoney(order.getFee());
            bill.setPayStatus(false);
            bill.setPayType(false);
            billMapper.insert(bill);
        }
        return Result.success("影像开单成功，" + test.getItemName() + " ￥" + order.getFee().toPlainString() + " 已计入账单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveReport(ImageReportVo vo) {
        if (vo == null || vo.getOrderId() == null) return Result.error("缺少申请单id");
        ImageOrder order = this.getById(vo.getOrderId());
        if (order == null) return Result.error("申请单不存在");
        if (Boolean.TRUE.equals(order.getStatus())) return Result.error("该单已出报告");
        if (vo.getResult() == null || vo.getResult().isBlank()) return Result.error("请填写影像报告内容");
        this.lambdaUpdate()
                .eq(ImageOrder::getOrderId, vo.getOrderId())
                .set(ImageOrder::getResult, vo.getResult())
                .set(ImageOrder::getReportTime, LocalDateTime.now())
                .set(ImageOrder::getStatus, true)
                .update();
        return Result.success("报告已提交");
    }

    @Override
    public IPage<ImageOrderDto> selectPage(Boolean status, int numPage, int pageSize) {
        return baseMapper.selectImageOrderPage(new Page<>(numPage, pageSize), status);
    }

    @Override
    public List<ImageOrderDto> selectByRecord(Integer recordId) {
        return baseMapper.selectByRecord(recordId);
    }

    @Override
    public RequisitionVo selectRequisition(Integer orderId) {
        ImageOrder order = this.getById(orderId);
        if (order == null) return null;
        RequisitionVo vo = new RequisitionVo();
        vo.setType("影像申请单");
        vo.setOrderId(orderId);
        vo.setOrderNo("No." + orderId);
        vo.setCreateTime(order.getCreateTime());
        vo.setFee(order.getFee());
        vo.setExamPart(order.getExamPart());
        vo.setRemark(order.getNote());
        ImageOrderDto dto = selectByRecord(order.getRecordId()).stream()
                .filter(d -> d.getOrderId().equals(orderId)).findFirst().orElse(null);
        if (dto != null) {
            vo.setClientName(dto.getClientName());
            vo.setPetName(dto.getPetName());
            vo.setDoctorName(dto.getDoctorName());
            vo.setDiagnosis(dto.getDiagnosis());
        }
        RequisitionVo.ReqItem item = new RequisitionVo.ReqItem();
        item.setName(order.getItemName());
        item.setGroup(order.getGroupName());
        vo.setItems(List.of(item));
        fillHeader(vo, order.getRecordId(), order.getPetId(), order.getDoctorId());
        return vo;
    }

    /** 补头部：诊断 + 宠物详情 + 医生资格证号 */
    private void fillHeader(RequisitionVo vo, Integer recordId, Integer petId, Integer doctorId) {
        MedicalRecord record = recordService.getById(recordId);
        if (record != null) vo.setDiagnosis(record.getDiagnosis());
        Pet pet = petMapper.selectById(petId);
        if (pet != null) {
            vo.setPetVariety(pet.getPetVariety());
            vo.setPetSex(pet.getPetSex());
            vo.setPetBirthday(pet.getPetBirthday());
            vo.setPetWeight(pet.getPetWeight());
            vo.setPetChip(pet.getPetChip());
        }
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor != null) vo.setDoctorCert(doctor.getDoctorCert());
    }
}