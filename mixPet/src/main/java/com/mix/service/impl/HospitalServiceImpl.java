package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.Result;
import com.mix.common.BizException;
import com.mix.mapper.BillMapper;
import com.mix.mapper.HospitalMapper;
import com.mix.model.dto.HospitalDto;
import com.mix.model.entity.Bill;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.HospitalAdvice;
import com.mix.model.entity.HospitalRecord;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.vo.HospitalFeeVo;
import com.mix.model.vo.HospitalVo;
import com.mix.service.IHospitalAdviceService;
import com.mix.service.IHospitalService;
import com.mix.service.IMedicalRecordService;
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
    private final IHospitalAdviceService adviceService;

    @Autowired
    public HospitalServiceImpl(IMedicalRecordService recordService, BillMapper billMapper,
                               IHospitalAdviceService adviceService) {
        this.recordService = recordService;
        this.billMapper = billMapper;
        this.adviceService = adviceService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveIn(HospitalVo vo, Doctor doctor) {
        if (vo == null || vo.getRecordId() == null) throw new BizException("缺少病历信息");
        MedicalRecord record = recordService.getById(vo.getRecordId());
        if (record == null) throw new BizException("病历不存在");
        BigDecimal bed = vo.getBedFee() == null ? BigDecimal.ZERO : vo.getBedFee();
        BigDecimal nursing = vo.getNursingFee() == null ? BigDecimal.ZERO : vo.getNursingFee();
        BigDecimal infusion = vo.getInfusionFee() == null ? BigDecimal.ZERO : vo.getInfusionFee();
        BigDecimal daily = bed.add(nursing).add(infusion);
        if (daily.compareTo(BigDecimal.ZERO) < 0)
            throw new BizException("日均住院费不能小于0");
        // 该病历不能重复住院（简单防重）
        long cnt = this.lambdaQuery()
                .eq(HospitalRecord::getRecordId, vo.getRecordId())
                .eq(HospitalRecord::getStatus, false)
                .count();
        if (cnt > 0) throw new BizException("该病历已有未出院记录");
        HospitalRecord recordEn = new HospitalRecord();
        recordEn.setRecordId(record.getRecordId());
        recordEn.setPetId(record.getPetId());
        recordEn.setClientId(record.getClientId());
        recordEn.setDoctorId(doctor.getDoctorId());
        recordEn.setRoomNo(vo.getRoomNo());
        recordEn.setBedFee(bed);
        recordEn.setNursingFee(nursing);
        recordEn.setInfusionFee(infusion);
        recordEn.setDailyFee(daily);
        recordEn.setInTime(LocalDateTime.now());
        recordEn.setStatus(false);
        recordEn.setDays(0);
        recordEn.setDeposit(BigDecimal.ZERO);
        recordEn.setNote(vo.getNote());
        return Result.choice("入院登记", this.save(recordEn));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result discharge(Integer hospitalId, Integer daysOverride) {
        if (hospitalId == null) throw new BizException("缺少住院记录id");
        HospitalRecord recordEn = this.getById(hospitalId);
        if (recordEn == null) throw new BizException("住院记录不存在");
        if (Boolean.TRUE.equals(recordEn.getStatus())) throw new BizException("该记录已出院");
        // 天数：默认按入院至今跨夜数+1（进一结算，至少1天）；结算弹窗可手改（提前出院按实际）
        long diff = Duration.between(recordEn.getInTime(), LocalDateTime.now()).toDays();
        int days = daysOverride != null && daysOverride > 0 ? daysOverride : Math.max((int) diff + 1, 1);
        // 费用 = 天数 × (床位+护理+输液) + 住院医嘱临时费用（已停嘱未执行 status=3 的不计）
        BigDecimal perDay = nz(recordEn.getBedFee()).add(nz(recordEn.getNursingFee())).add(nz(recordEn.getInfusionFee()));
        BigDecimal stayFee = perDay.multiply(BigDecimal.valueOf(days));
        BigDecimal adviceFee = adviceService.lambdaQuery()
                .eq(HospitalAdvice::getHospitalId, hospitalId)
                .ne(HospitalAdvice::getStatus, 3)
                .list().stream()
                .map(a -> a.getFee() == null ? BigDecimal.ZERO : a.getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fee = stayFee.add(adviceFee);
        // 押金冲抵：收取押金时未进系统收入，出院时按净额入账，避免把押金重复计成营收
        BigDecimal deposit = nz(recordEn.getDeposit());
        BigDecimal used = deposit.min(fee);
        BigDecimal net = fee.subtract(used).max(BigDecimal.ZERO);
        // CAS 抢占出院状态：两个前台同时点结算只有一个能计费（WHERE status = 0）
        boolean claimed = this.lambdaUpdate()
                .eq(HospitalRecord::getHospitalId, hospitalId)
                .eq(HospitalRecord::getStatus, false)
                .set(HospitalRecord::getStatus, true)
                .set(HospitalRecord::getOutTime, LocalDateTime.now())
                .set(HospitalRecord::getDays, days)
                .update();
        if (!claimed) throw new BizException("该记录已出院，请勿重复结算");
        // 计费：押金已冲抵部分不再计入系统收入，只把净额并入统一账单
        if (net.signum() > 0) {
            Bill unpaid = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                    .eq(Bill::getRecordId, recordEn.getRecordId())
                    .eq(Bill::getPayStatus, false)
                    .last("limit 1"));
            if (unpaid != null) {
                billMapper.update(null, Wrappers.<Bill>lambdaUpdate()
                        .eq(Bill::getBillId, unpaid.getBillId())
                        .setSql("fee_hospital = COALESCE(fee_hospital,0) + " + net.toPlainString())
                        .setSql("total_money = COALESCE(total_money,0) + " + net.toPlainString()));
            } else {
                Bill bill = new Bill();
                bill.setClientId(recordEn.getClientId());
                bill.setPetId(recordEn.getPetId());
                bill.setRecordId(recordEn.getRecordId());
                bill.setFeeRegister(BigDecimal.ZERO);
                bill.setFeeTreat(BigDecimal.ZERO);
                bill.setFeeDrug(BigDecimal.ZERO);
                bill.setFeeLab(BigDecimal.ZERO);
                bill.setFeeImage(BigDecimal.ZERO);
                bill.setFeePackage(BigDecimal.ZERO);
                bill.setFeeSurgery(BigDecimal.ZERO);
                bill.setFeeImmune(BigDecimal.ZERO);
                bill.setFeeHospital(net);
                bill.setTotalMoney(net);
                bill.setPayStatus(false);
                bill.setPayType(false);
                // 补开的新账单继承本次就诊已有账单的会员折扣快照，避免会员被按原价扣款
                Bill prevBill = billMapper.selectOne(Wrappers.<Bill>lambdaQuery()
                        .eq(Bill::getRecordId, bill.getRecordId())
                        .last("limit 1"));
                if (prevBill != null && prevBill.getDiscount() != null)
                    bill.setDiscount(prevBill.getDiscount());
                billMapper.insert(bill);
            }
        }
        BigDecimal leftover = deposit.subtract(fee);
        String depositTip = used.signum() > 0
                ? "；押金 ￥" + deposit.toPlainString() + " 已冲抵 ￥" + used.toPlainString()
                + (net.signum() > 0 ? "，仍需补收 ￥" + net.toPlainString() : "，费用已由押金结清")
                + (leftover.signum() > 0 ? "，余款 ￥" + leftover.toPlainString() + " 请线下退还" : "")
                : "";
        return Result.success("出院结算成功：住院 " + days + " 天，住院费 ￥" + stayFee.toPlainString()
                + (adviceFee.signum() > 0 ? " + 医嘱费用 ￥" + adviceFee.toPlainString() : "")
                + "（原价 ￥" + fee.toPlainString() + "，已计入账单 ￥" + net.toPlainString() + "）" + depositTip);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addDeposit(Integer hospitalId, BigDecimal amount, Doctor operator) {
        if (hospitalId == null) throw new BizException("缺少住院记录id");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("收取金额必须大于0");
        HospitalRecord recordEn = this.getById(hospitalId);
        if (recordEn == null) throw new BizException("住院记录不存在");
        if (Boolean.TRUE.equals(recordEn.getStatus())) throw new BizException("该记录已出院，不能收取押金");
        // 原子累加，避免并发两次收押金丢失一次（读后写）
        boolean ok = this.lambdaUpdate()
                .eq(HospitalRecord::getHospitalId, hospitalId)
                .eq(HospitalRecord::getStatus, false)
                .setSql("deposit = COALESCE(deposit, 0) + " + amount.toPlainString())
                .set(HospitalRecord::getDepositBy, operator == null ? null : operator.getDoctorName())
                .set(HospitalRecord::getDepositTime, LocalDateTime.now())
                .update();
        if (!ok) throw new BizException("收取失败，住院记录状态可能已变更");
        HospitalRecord fresh = this.getById(hospitalId);
        BigDecimal now = fresh == null ? amount : nz(fresh.getDeposit());
        return Result.success("收取押金 ￥" + amount.toPlainString() + "，累计已收 ￥" + now.toPlainString());
    }

    @Override
    public HospitalFeeVo feePreview(Integer hospitalId) {
        HospitalRecord r = this.getById(hospitalId);
        HospitalFeeVo vo = new HospitalFeeVo();
        if (r == null) return vo;
        long diff = Duration.between(r.getInTime(), LocalDateTime.now()).toDays();
        int days = Math.max((int) diff + 1, 1);
        BigDecimal perDay = nz(r.getBedFee()).add(nz(r.getNursingFee())).add(nz(r.getInfusionFee()));
        BigDecimal stayFee = perDay.multiply(BigDecimal.valueOf(days));
        BigDecimal adviceFee = adviceService.lambdaQuery()
                .eq(HospitalAdvice::getHospitalId, hospitalId)
                .ne(HospitalAdvice::getStatus, 3)
                .list().stream()
                .map(a -> a.getFee() == null ? BigDecimal.ZERO : a.getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setHospitalId(hospitalId);
        vo.setDays(days);
        vo.setBedFee(nz(r.getBedFee()));
        vo.setNursingFee(nz(r.getNursingFee()));
        vo.setInfusionFee(nz(r.getInfusionFee()));
        vo.setPerDay(perDay);
        vo.setStayFee(stayFee);
        vo.setAdviceFee(adviceFee);
        vo.setTotalFee(stayFee.add(adviceFee));
        vo.setDeposit(nz(r.getDeposit()));
        vo.setRefund(vo.getDeposit().subtract(vo.getTotalFee()));
        return vo;
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