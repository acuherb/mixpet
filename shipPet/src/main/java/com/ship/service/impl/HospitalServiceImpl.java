package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.BillMapper;
import com.ship.mapper.HospitalMapper;
import com.ship.model.dto.HospitalDto;
import com.ship.model.entity.Bill;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.HospitalRecord;
import com.ship.model.entity.MedicalRecord;
import com.ship.model.vo.HospitalVo;
import com.ship.service.IHospitalService;
import com.ship.service.IMedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 住院 Service 实现
 */
@Service
public class HospitalServiceImpl extends ServiceImpl<HospitalMapper, HospitalRecord> implements IHospitalService {
    private final IMedicalRecordService recordService;
    private final BillMapper billMapper;

    @Autowired
    public HospitalServiceImpl(IMedicalRecordService recordService, BillMapper billMapper) {
        this.recordService = recordService;
        this.billMapper = billMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveIn(HospitalVo vo, Doctor doctor) {
        if (vo == null || vo.getRecordId() == null) return Result.error("缺少病历信息");
        MedicalRecord record = recordService.getById(vo.getRecordId());
        if (record == null) return Result.error("病历不存在");
        if (vo.getDailyFee() == null || vo.getDailyFee().compareTo(BigDecimal.ZERO) < 0)
            return Result.error("日均住院费不能小于0");
        // 该病历不能重复本月住院（简单防重）
        long cnt = this.lambdaQuery()
                .eq(HospitalRecord::getRecordId, vo.getRecordId())
                .eq(HospitalRecord::getStatus, false)
                .count();
        if (cnt > 0) return Result.error("该病历已有未出院记录");
        HospitalRecord recordEn = new HospitalRecord();
        recordEn.setRecordId(record.getRecordId());
        recordEn.setPetId(record.getPetId());
        recordEn.setClientId(record.getClientId());
        recordEn.setDoctorId(doctor.getDoctorId());
        recordEn.setRoomNo(vo.getRoomNo());
        recordEn.setDailyFee(vo.getDailyFee());
        recordEn.setInTime(LocalDateTime.now());
        recordEn.setStatus(false);
        recordEn.setDays(0);
        recordEn.setNote(vo.getNote());
        return Result.choice("入院登记", this.save(recordEn));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result discharge(Integer hospitalId) {
        if (hospitalId == null) return Result.error("缺少住院记录id");
        HospitalRecord recordEn = this.getById(hospitalId);
        if (recordEn == null) return Result.error("住院记录不存在");
        if (Boolean.TRUE.equals(recordEn.getStatus())) return Result.error("该记录已出院");
        // 按天数进一结算，至少1天
        long diff = Duration.between(recordEn.getInTime(), LocalDateTime.now()).toDays();
        int days = Math.max((int) diff + 1, 1);
        BigDecimal fee = recordEn.getDailyFee() == null ? BigDecimal.ZERO
                : recordEn.getDailyFee().multiply(BigDecimal.valueOf(days));
        // 计费：优先累加进未付账单，否则单独生成一张住院账单
        Bill unpaid = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getRecordId, recordEn.getRecordId())
                .eq(Bill::getPayStatus, false)
                .last("limit 1"));
        if (unpaid != null) {
            billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                    .eq(Bill::getBillId, unpaid.getBillId())
                    .setSql("fee_hospital = fee_hospital + " + fee.toPlainString())
                    .setSql("total_money = total_money + " + fee.toPlainString()));
        } else {
            Bill bill = new Bill();
            bill.setClientId(recordEn.getClientId());
            bill.setPetId(recordEn.getPetId());
            bill.setRecordId(recordEn.getRecordId());
            bill.setFeeRegister(BigDecimal.ZERO);
            bill.setFeeTreat(BigDecimal.ZERO);
            bill.setFeeDrug(BigDecimal.ZERO);
            bill.setFeeLab(BigDecimal.ZERO);
            bill.setFeePackage(BigDecimal.ZERO);
            bill.setFeeSurgery(BigDecimal.ZERO);
            bill.setFeeHospital(fee);
            bill.setTotalMoney(fee);
            bill.setPayStatus(false);
            bill.setPayType(false);
            billMapper.insert(bill);
        }
        this.lambdaUpdate()
                .eq(HospitalRecord::getHospitalId, hospitalId)
                .set(HospitalRecord::getStatus, true)
                .set(HospitalRecord::getOutTime, LocalDateTime.now())
                .set(HospitalRecord::getDays, days)
                .update();
        return Result.success("出院结算成功，住院 " + days + " 天，住院费已计入账单（￥" + fee.toPlainString() + "）");
    }

    @Override
    public IPage<HospitalDto> selectPage(Boolean status, int numPage, int pageSize) {
        return baseMapper.selectHospitalPage(new Page<>(numPage, pageSize), status);
    }

    @Override
    public List<HospitalDto> selectByPet(Integer petId) {
        return baseMapper.selectByPet(petId);
    }
}