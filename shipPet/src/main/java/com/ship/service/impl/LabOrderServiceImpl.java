package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.BillMapper;
import com.ship.mapper.DoctorMapper;
import com.ship.mapper.LabOrderItemMapper;
import com.ship.mapper.LabOrderMapper;
import com.ship.mapper.LabTestMapper;
import com.ship.mapper.PetMapper;
import com.ship.model.dto.LabOrderDto;
import com.ship.model.entity.*;
import com.ship.model.vo.LabOrderVo;
import com.ship.model.vo.LabResultVo;
import com.ship.model.vo.RequisitionVo;
import com.ship.service.ILabOrderService;
import com.ship.service.IMedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 化验单 Service 实现
 */
@Service
public class LabOrderServiceImpl extends ServiceImpl<LabOrderMapper, LabOrder> implements ILabOrderService {
    private final IMedicalRecordService recordService;
    private final LabTestMapper labTestMapper;
    private final LabOrderItemMapper labOrderItemMapper;
    private final BillMapper billMapper;
    private final PetMapper petMapper;
    private final DoctorMapper doctorMapper;

    @Autowired
    public LabOrderServiceImpl(IMedicalRecordService recordService, LabTestMapper labTestMapper,
                               LabOrderItemMapper labOrderItemMapper, BillMapper billMapper,
                               PetMapper petMapper, DoctorMapper doctorMapper) {
        this.recordService = recordService;
        this.labTestMapper = labTestMapper;
        this.labOrderItemMapper = labOrderItemMapper;
        this.billMapper = billMapper;
        this.petMapper = petMapper;
        this.doctorMapper = doctorMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(LabOrderVo vo, Doctor doctor) {
        if (vo == null || vo.getRecordId() == null) return Result.error("缺少病历信息");
        MedicalRecord record = recordService.getById(vo.getRecordId());
        if (record == null) return Result.error("病历不存在");
        if (vo.getTestIds() == null || vo.getTestIds().isEmpty()) return Result.error("请至少选择一项检验");
        // 校验检验项并累计费用（价格以字典为准）
        BigDecimal fee = BigDecimal.ZERO;
        List<LabOrderItem> items = new ArrayList<>();
        for (Integer testId : vo.getTestIds()) {
            LabTest test = labTestMapper.selectById(testId);
            if (test == null || Boolean.FALSE.equals(test.getActive()))
                return Result.error("检验项不存在或已停用");
            LabOrderItem item = new LabOrderItem();
            item.setTestId(test.getTestId());
            item.setItemName(test.getTestName());
            item.setItemGroup(test.getTestGroup());
            item.setItemUnit(test.getTestUnit());
            item.setRefRange(test.getRefRange());
            items.add(item);
            if (test.getTestPrice() != null) fee = fee.add(test.getTestPrice());
        }
        // 归属以病历为准，不信任前端
        LabOrder order = new LabOrder();
        order.setRecordId(record.getRecordId());
        order.setPetId(record.getPetId());
        order.setClientId(record.getClientId());
        order.setDoctorId(doctor.getDoctorId());
        order.setOrderNote(vo.getOrderNote());
        order.setFee(fee);
        order.setStatus(false);
        this.save(order);
        for (LabOrderItem item : items) {
            item.setOrderId(order.getOrderId());
            labOrderItemMapper.insert(item);
        }
        // 计费：优先累加进还未支付的账单，否则单独生成一张化验账单
        Bill unpaid = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getRecordId, record.getRecordId())
                .eq(Bill::getPayStatus, false)
                .last("limit 1"));
        if (unpaid != null) {
            billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                    .eq(Bill::getBillId, unpaid.getBillId())
                    .setSql("fee_lab = fee_lab + " + fee.toPlainString())
                    .setSql("total_money = total_money + " + fee.toPlainString()));
        } else {
            Bill bill = new Bill();
            bill.setClientId(record.getClientId());
            bill.setPetId(record.getPetId());
            bill.setRecordId(record.getRecordId());
            bill.setFeeRegister(BigDecimal.ZERO);
            bill.setFeeTreat(BigDecimal.ZERO);
            bill.setFeeDrug(BigDecimal.ZERO);
            bill.setFeeLab(fee);
            bill.setTotalMoney(fee);
            bill.setPayStatus(false);
            bill.setPayType(false);
            billMapper.insert(bill);
        }
        return Result.success("开单成功，化验费已计入账单（￥" + fee.toPlainString() + "）");
    }

    @Override
    public IPage<LabOrderDto> selectPage(Integer status, int numPage, int pageSize) {
        return baseMapper.selectLabOrderPage(new Page<>(numPage, pageSize), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveResult(LabResultVo vo, String operator) {
        if (vo == null || vo.getOrderId() == null) return Result.error("缺少化验单信息");
        LabOrder order = this.getById(vo.getOrderId());
        if (order == null) return Result.error("化验单不存在");
        if (Boolean.TRUE.equals(order.getStatus())) return Result.error("该化验单已出结果，请勿重复录入");
        if (vo.getItems() != null) {
            for (LabResultVo.LabResultItemVo r : vo.getItems()) {
                if (r == null || r.getItemId() == null) continue;
                labOrderItemMapper.update(null, Wrappers.<LabOrderItem>lambdaUpdate()
                        .eq(LabOrderItem::getItemId, r.getItemId())
                        .set(LabOrderItem::getResultValue, r.getResultValue())
                        .set(LabOrderItem::getIsAbnormal, Boolean.TRUE.equals(r.getIsAbnormal()))
                        .set(LabOrderItem::getResultTime, LocalDateTime.now()));
            }
        }
        this.lambdaUpdate()
                .eq(LabOrder::getOrderId, vo.getOrderId())
                .set(LabOrder::getStatus, true)
                .set(LabOrder::getResultNote, vo.getResultNote())
                .set(LabOrder::getResultBy, operator)
                .update();
        return Result.success("检验结果已保存");
    }

    @Override
    public List<LabOrderDto> selectByRecord(Integer recordId) {
        List<LabOrder> orders = this.lambdaQuery()
                .eq(LabOrder::getRecordId, recordId)
                .orderByDesc(LabOrder::getCreateTime)
                .list();
        List<LabOrderDto> result = new ArrayList<>();
        for (LabOrder o : orders) {
            LabOrderDto dto = new LabOrderDto();
            dto.setOrderId(o.getOrderId());
            dto.setRecordId(o.getRecordId());
            dto.setPetId(o.getPetId());
            dto.setClientId(o.getClientId());
            dto.setDoctorId(o.getDoctorId());
            dto.setOrderNote(o.getOrderNote());
            dto.setFee(o.getFee());
            dto.setStatus(o.getStatus());
            dto.setResultNote(o.getResultNote());
            dto.setResultBy(o.getResultBy());
            dto.setCreateTime(o.getCreateTime());
            dto.setItems(labOrderItemMapper.selectList(
                    Wrappers.<LabOrderItem>lambdaQuery().eq(LabOrderItem::getOrderId, o.getOrderId())));
            result.add(dto);
        }
        return result;
    }

    @Override
    public RequisitionVo selectRequisition(Integer orderId) {
        LabOrder order = this.getById(orderId);
        if (order == null) return null;
        RequisitionVo vo = new RequisitionVo();
        vo.setType("检验申请单");
        vo.setOrderId(orderId);
        vo.setOrderNo("No." + orderId);
        vo.setCreateTime(order.getCreateTime());
        vo.setFee(order.getFee());
        vo.setRemark(order.getOrderNote());
        LabOrderDto dto = selectByRecord(order.getRecordId()).stream()
                .filter(d -> d.getOrderId().equals(orderId)).findFirst().orElse(null);
        if (dto != null) {
            vo.setClientName(dto.getClientName());
            vo.setPetName(dto.getPetName());
            vo.setDoctorName(dto.getDoctorName());
            List<RequisitionVo.ReqItem> items = new ArrayList<>();
            if (dto.getItems() != null) {
                for (LabOrderItem it : dto.getItems()) {
                    RequisitionVo.ReqItem r = new RequisitionVo.ReqItem();
                    r.setName(it.getItemName());
                    r.setGroup(it.getItemGroup());
                    r.setRefRange(it.getRefRange());
                    r.setUnit(it.getItemUnit());
                    items.add(r);
                }
            }
            vo.setItems(items);
        }
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