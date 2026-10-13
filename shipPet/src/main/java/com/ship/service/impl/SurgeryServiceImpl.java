package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.AnesthesiaRecordMapper;
import com.ship.mapper.BillMapper;
import com.ship.mapper.SurgeryMapper;
import com.ship.model.dto.SurgeryDto;
import com.ship.model.entity.*;
import com.ship.model.vo.SurgeryVo;
import com.ship.service.IMedicalRecordService;
import com.ship.service.ISurgeryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 手术 Service 实现
 */
@Service
public class SurgeryServiceImpl extends ServiceImpl<SurgeryMapper, Surgery> implements ISurgeryService {
    private final IMedicalRecordService recordService;
    private final BillMapper billMapper;
    private final AnesthesiaRecordMapper anesthesiaMapper;

    @Autowired
    public SurgeryServiceImpl(IMedicalRecordService recordService, BillMapper billMapper,
                              AnesthesiaRecordMapper anesthesiaMapper) {
        this.recordService = recordService;
        this.billMapper = billMapper;
        this.anesthesiaMapper = anesthesiaMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(SurgeryVo vo, Doctor doctor) {
        if (vo == null || vo.getRecordId() == null) return Result.error("缺少病历信息");
        MedicalRecord record = recordService.getById(vo.getRecordId());
        if (record == null) return Result.error("病历不存在");
        if (vo.getSurgeryName() == null || vo.getSurgeryName().isBlank()) return Result.error("请填写手术名称");
        BigDecimal fee = vo.getFee() == null ? BigDecimal.ZERO : vo.getFee();
        if (fee.compareTo(BigDecimal.ZERO) < 0) return Result.error("手术费不能为负数");
        Surgery s = new Surgery();
        s.setRecordId(record.getRecordId());
        s.setPetId(record.getPetId());
        s.setClientId(record.getClientId());
        s.setDoctorId(doctor == null ? null : doctor.getDoctorId());
        s.setSurgeryName(vo.getSurgeryName());
        s.setSurgeryType(vo.getSurgeryType());
        s.setAnesthesiaType(vo.getAnesthesiaType());
        s.setScheduledTime(vo.getScheduledTime());
        s.setSurgeon(vo.getSurgeon());
        s.setAssistant(vo.getAssistant());
        s.setFee(fee);
        s.setStatus(0);
        this.save(s);
        // 计费：累加进该病历未付账单，否则新建一张手术账单
        Bill unpaid = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getRecordId, record.getRecordId())
                .eq(Bill::getPayStatus, false)
                .last("limit 1"));
        if (unpaid != null) {
            billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                    .eq(Bill::getBillId, unpaid.getBillId())
                    .setSql("fee_surgery = fee_surgery + " + fee.toPlainString())
                    .setSql("total_money = total_money + " + fee.toPlainString()));
        } else {
            Bill bill = new Bill();
            bill.setClientId(record.getClientId());
            bill.setPetId(record.getPetId());
            bill.setRecordId(record.getRecordId());
            bill.setFeeRegister(BigDecimal.ZERO);
            bill.setFeeTreat(BigDecimal.ZERO);
            bill.setFeeDrug(BigDecimal.ZERO);
            bill.setFeeLab(BigDecimal.ZERO);
            bill.setFeeHospital(BigDecimal.ZERO);
            bill.setFeeImage(BigDecimal.ZERO);
            bill.setFeePackage(BigDecimal.ZERO);
            bill.setFeeSurgery(fee);
            bill.setTotalMoney(fee);
            bill.setPayStatus(false);
            bill.setPayType(false);
            billMapper.insert(bill);
        }
        return Result.success("手术安排成功，手术费已计入账单");
    }

    @Override
    public IPage<SurgeryDto> selectPage(Integer status, int numPage, int pageSize) {
        return baseMapper.selectSurgeryPage(new Page<>(numPage, pageSize), status);
    }

    @Override
    public List<SurgeryDto> selectByRecord(Integer recordId) {
        return baseMapper.selectByRecord(recordId);
    }

    @Override
    public SurgeryDto selectDetail(Integer surgeryId) {
        SurgeryDto dto = baseMapper.selectOneById(surgeryId);
        if (dto != null) {
            dto.setAnesthesia(anesthesiaMapper.selectOne(
                    Wrappers.<AnesthesiaRecord>lambdaQuery().eq(AnesthesiaRecord::getSurgeryId, surgeryId).last("limit 1")));
        }
        return dto;
    }

    @Override
    public Result update(Surgery surgery) {
        if (surgery == null || surgery.getSurgeryId() == null) return Result.error("缺少手术单id");
        return Result.choice("保存", this.updateById(surgery));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveAnesthesia(AnesthesiaRecord ar) {
        if (ar == null || ar.getSurgeryId() == null) return Result.error("缺少手术单id");
        AnesthesiaRecord exist = anesthesiaMapper.selectOne(
                Wrappers.<AnesthesiaRecord>lambdaQuery().eq(AnesthesiaRecord::getSurgeryId, ar.getSurgeryId()).last("limit 1"));
        if (exist != null) {
            ar.setAnesthesiaId(exist.getAnesthesiaId());
            return Result.choice("保存", anesthesiaMapper.updateById(ar) > 0);
        }
        return Result.choice("保存", anesthesiaMapper.insert(ar) > 0);
    }
}
