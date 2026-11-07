package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.BizException;
import com.mix.common.Result;
import com.mix.mapper.FosterMapper;
import com.mix.model.dto.FosterDto;
import com.mix.model.dto.FosterPetDto;
import com.mix.model.entity.Foster;
import com.mix.service.IBillService;
import com.mix.service.IFosterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class FosterServiceImpl extends ServiceImpl<FosterMapper, Foster> implements IFosterService {
    private final IBillService billService;

    public FosterServiceImpl(IBillService billService) {
        this.billService = billService;
    }

    @Override
    public boolean deleteById(Integer fosterId) {
        return this.lambdaUpdate()
                .eq(Foster::getFosterId, fosterId)
                .set(Foster::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public boolean deleteByIds(List<Integer> idGroup) {
        return this.lambdaUpdate()
                .in(Foster::getFosterId, idGroup)
                .set(Foster::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public IPage<FosterDto> selectByPage(String clientName, String petName, int numPage, int pageSize) {
        return baseMapper.selectFosterPage(new Page<>(numPage, pageSize), clientName, petName);
    }

    @Override
    public IPage<FosterPetDto> fosterPet(int pageSize) {
        return baseMapper.fosterPet(new Page<>(1, pageSize));
    }

    @Override
    public Integer haveFoster(Integer petId) {
        return Math.toIntExact(this.lambdaQuery()
                .eq(Foster::getPetId, petId)
                .ge(Foster::getFosterTerm, LocalDate.now())
                // 已接回结算的寄养单不再占用名额
                .and(w -> w.eq(Foster::getStatus, 1).or().isNull(Foster::getStatus))
                .count());
    }

    @Override
    public boolean save(Foster entity) {
        String nowDate = new SimpleDateFormat("yyMMdd").format(new Date());
        entity.setFosterCode(1 + nowDate + (baseMapper.getMaxId() + 1));
        if (entity.getStatus() == null) entity.setStatus(1);
        if (entity.getInDate() == null) entity.setInDate(LocalDate.now());
        return super.save(entity);
    }

    @Override
    public boolean updateFoster(Foster foster) {
        return baseMapper.updateFoster(foster);
    }

    /** 接回结算：服务端按天数×单价重算（不信任前端金额），CAS 防双点，费用并入统一账单 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result back(Integer fosterId, LocalDate backDate) {
        Foster f = this.getById(fosterId);
        if (f == null) throw new BizException("寄养单不存在");
        if (Integer.valueOf(2).equals(f.getStatus())) throw new BizException("该寄养单已结算接回");
        LocalDate in = f.getInDate() == null ? LocalDate.now() : f.getInDate();
        LocalDate out = backDate == null ? LocalDate.now() : backDate;
        long days = Math.max(1, ChronoUnit.DAYS.between(in, out) + 1);
        BigDecimal perDay = nz(f.getDailyFee()).add(nz(f.getCleanFee())).add(nz(f.getMealFee()));
        if (perDay.signum() <= 0) throw new BizException("寄养单价未配置，无法结算");
        BigDecimal money = perDay.multiply(BigDecimal.valueOf(days));

        boolean ok = this.lambdaUpdate()
                .eq(Foster::getFosterId, fosterId)
                .and(w -> w.eq(Foster::getStatus, 1).or().isNull(Foster::getStatus))
                .set(Foster::getStatus, 2)
                .set(Foster::getBackDate, out)
                .set(Foster::getFosterMoney, money.setScale(0, RoundingMode.HALF_UP).intValue())
                .update();
        if (!ok) throw new BizException("接回结算失败，状态可能已变更");
        billService.addCharge(f.getClientId(), f.getPetId(), null, money, "fee_package");
        return Result.success("接回成功：共 " + days + " 天，结算 ￥" + money.toPlainString()
                + "，费用已计入收银台账单，请到收费列表收款");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
