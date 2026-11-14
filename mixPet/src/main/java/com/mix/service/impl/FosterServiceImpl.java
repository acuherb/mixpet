package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.BizException;
import com.mix.common.Result;
import com.mix.mapper.FosterLogMapper;
import com.mix.mapper.FosterMapper;
import com.mix.mapper.FosterPhotoMapper;
import com.mix.model.dto.FosterDto;
import com.mix.model.dto.FosterPetDto;
import com.mix.model.entity.Foster;
import com.mix.model.entity.FosterLog;
import com.mix.model.entity.FosterPhoto;
import com.mix.model.vo.FosterBackVo;
import com.mix.model.vo.FosterLogVo;
import com.mix.service.IBillService;
import com.mix.service.IClientNotificationService;
import com.mix.service.IFosterService;
import com.mix.util.StrUtil;
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
    private final IClientNotificationService notificationService;
    private final FosterLogMapper logMapper;
    private final FosterPhotoMapper photoMapper;

    public FosterServiceImpl(IBillService billService,
                             IClientNotificationService notificationService,
                             FosterLogMapper logMapper,
                             FosterPhotoMapper photoMapper) {
        this.billService = billService;
        this.notificationService = notificationService;
        this.logMapper = logMapper;
        this.photoMapper = photoMapper;
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
                .and(w -> w.eq(Foster::getStatus, 1).or().eq(Foster::getStatus, 0))
                .count());
    }

    @Override
    public boolean save(Foster entity) {
        // 三项单价必须至少有一项 > 0，否则这张单以后永远无法接回结算
        BigDecimal perDay = nz(entity.getDailyFee()).add(nz(entity.getCleanFee())).add(nz(entity.getMealFee()));
        if (perDay.signum() <= 0) throw new BizException("请配置寄养日单价（笼位/清洁/餐费至少一项）");
        String nowDate = new SimpleDateFormat("yyMMdd").format(new Date());
        entity.setFosterCode("1" + nowDate + (baseMapper.getMaxId() + 1));
        if (entity.getStatus() == null) entity.setStatus(0);
        if (entity.getInDate() == null) entity.setInDate(LocalDate.now());
        boolean ok = super.save(entity);
        if (ok) {
            notificationService.notifyReport(entity.getClientId(), "寄养登记成功",
                    "订单 " + entity.getFosterCode() + " 已登记，入托后可查看每日照护记录。",
                    "FOSTER_IN", "foster", entity.getFosterId());
        }
        return ok;
    }

    @Override
    public boolean updateFoster(Foster foster) {
        return baseMapper.updateFoster(foster);
    }

    @Override
    public Result start(Integer fosterId) {
        Foster f = this.getById(fosterId);
        if (f == null) throw new BizException("寄养单不存在");
        if (Integer.valueOf(2).equals(f.getStatus())) throw new BizException("该寄养单已接回");
        if (Integer.valueOf(1).equals(f.getStatus())) return Result.success("已在寄养中");
        boolean ok = this.lambdaUpdate()
                .eq(Foster::getFosterId, fosterId)
                .ne(Foster::getStatus, 1)
                .set(Foster::getStatus, 1)
                .set(Foster::getInDate, LocalDate.now())
                .update();
        if (!ok) throw new BizException("开始寄养失败，请刷新重试");
        notificationService.notifyReport(f.getClientId(), "宠物已入托",
                "订单 " + f.getFosterCode() + " 已开始寄养，我们会每日更新照护记录与照片。",
                "FOSTER_IN", "foster", fosterId);
        return Result.success("已开始寄养");
    }

    /**
     * 接回结算（事务）：
     * 天数含首尾，与前端展示统一；定价可由 MANAGER 人工改价但必须填说明留痕；
     * 押金先冲抵，只把净额计入账单（fee_foster 独立分项 + foster_id 回指）；
     * 多退少补分别记 refundMoney / chargeMoney，fosterMoney 押金本身不再被覆盖。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result back(Integer fosterId, FosterBackVo vo) {
        Foster f = this.getById(fosterId);
        if (f == null) throw new BizException("寄养单不存在");
        if (Integer.valueOf(2).equals(f.getStatus())) throw new BizException("该寄养单已结算接回");
        LocalDate in = f.getInDate() == null ? LocalDate.now() : f.getInDate();
        LocalDate out = (vo == null || vo.backDate() == null) ? LocalDate.now() : vo.backDate();
        if (out.isBefore(in)) throw new BizException("接回日期不能早于入托日期");
        long days = Math.max(1, ChronoUnit.DAYS.between(in, out) + 1);
        BigDecimal perDay = nz(f.getDailyFee()).add(nz(f.getCleanFee())).add(nz(f.getMealFee()));
        if (perDay.signum() <= 0) throw new BizException("寄养单价未配置，无法结算");
        BigDecimal auto = perDay.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal settle = auto;
        String adjustNote = null;
        if (vo != null && vo.settleMoney() != null) {
            settle = vo.settleMoney().setScale(2, RoundingMode.HALF_UP);
            if (settle.signum() < 0) throw new BizException("结算金额不能为负");
            if (StrUtil.isWhite(vo.adjustNote())) throw new BizException("人工改价必须填写改价说明");
            adjustNote = "系统计价￥" + auto.toPlainString() + " → 人工￥" + settle.toPlainString()
                    + "；" + vo.adjustNote().trim();
        }

        BigDecimal deposit = BigDecimal.valueOf(f.getFosterMoney() == null ? 0 : f.getFosterMoney());
        BigDecimal used = deposit.min(settle).setScale(2, RoundingMode.HALF_UP);
        BigDecimal refund = deposit.subtract(used).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal charge = settle.subtract(used).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        Integer billId = null;
        if (charge.signum() > 0) {
            billId = billService.addChargeForFoster(f.getClientId(), f.getPetId(), charge, fosterId);
        }

        Integer status = f.getStatus();
        boolean ok = this.lambdaUpdate()
                .eq(Foster::getFosterId, fosterId)
                .eq(status != null, Foster::getStatus, status)
                .isNull(status == null, Foster::getStatus)
                .set(Foster::getStatus, 2)
                .set(Foster::getBackDate, out)
                .set(Foster::getDayCount, (int) days)
                .set(Foster::getSettleMoney, settle)
                .set(Foster::getDepositUsed, used)
                .set(Foster::getRefundMoney, refund)
                .set(Foster::getChargeMoney, charge)
                .set(Foster::getAdjustNote, adjustNote)
                .set(Foster::getBillId, billId)
                .update();
        if (!ok) throw new BizException("接回结算失败，状态可能已变更，请刷新重试");

        String base = "共 " + days + " 天，结算 ￥" + settle.toPlainString()
                + "，押金冲抵 ￥" + used.toPlainString();
        notificationService.notifyReport(f.getClientId(), "寄养接回完成",
                base + (refund.signum() > 0 ? "，应退 ￥" + refund.toPlainString() : "") + "。",
                "FOSTER_BACK", "foster", fosterId);
        if (charge.signum() > 0) {
            notificationService.notifyReport(f.getClientId(), "寄养费用待缴",
                    "本次寄养需补缴 ￥" + charge.toPlainString() + "，请在账单明细中查看。",
                    "FOSTER_BILL", "foster", fosterId);
        }
        String msg = base
                + (refund.signum() > 0 ? "，应退 ￥" + refund.toPlainString() : "")
                + (charge.signum() > 0 ? "，已计入收银台账单待收 ￥" + charge.toPlainString() : "，押金已足额覆盖");
        return Result.success(msg);
    }

    @Override
    public List<FosterDto> selfList(Integer clientId) {
        if (clientId == null) return List.of();
        return baseMapper.selfFoster(clientId);
    }

    @Override
    public FosterDto selfOne(Integer fosterId, Integer clientId) {
        if (fosterId == null || clientId == null) return null;
        return baseMapper.selfOne(fosterId, clientId);
    }

    @Override
    public FosterDto staffOne(Integer fosterId) {
        return fosterId == null ? null : baseMapper.staffOne(fosterId);
    }

    @Override
    public List<FosterLog> logsOf(Integer fosterId, Integer clientId) {
        if (clientId != null && !owns(fosterId, clientId)) return List.of();
        return logMapper.selectList(Wrappers.<FosterLog>lambdaQuery()
                .eq(FosterLog::getFosterId, fosterId)
                .eq(FosterLog::getIsDel, 0)
                .orderByDesc(FosterLog::getLogDate)
                .last("LIMIT 200"));
    }

    @Override
    public List<FosterPhoto> photosOf(Integer fosterId, Integer clientId) {
        if (clientId != null && !owns(fosterId, clientId)) return List.of();
        return photoMapper.selectList(Wrappers.<FosterPhoto>lambdaQuery()
                .eq(FosterPhoto::getFosterId, fosterId)
                .eq(FosterPhoto::getIsDel, 0)
                .orderByDesc(FosterPhoto::getLogDate)
                .last("LIMIT 500"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addLog(FosterLogVo vo, String recorder) {
        if (vo == null || vo.fosterId() == null) throw new BizException("缺少寄养单");
        Foster f = this.getById(vo.fosterId());
        if (f == null) throw new BizException("寄养单不存在");
        if (Integer.valueOf(2).equals(f.getStatus())) throw new BizException("已接回的寄养单不能再添加记录");

        FosterLog log = new FosterLog();
        log.setFosterId(vo.fosterId());
        log.setLogDate(vo.logDate() == null ? LocalDate.now() : vo.logDate());
        log.setMentalState(vo.mentalState());
        log.setAppetite(vo.appetite());
        log.setDefecation(vo.defecation());
        log.setWeight(vo.weight());
        log.setNote(vo.note());
        log.setRecorder(recorder);
        log.setIsDel(0L);
        logMapper.insert(log);

        if (vo.photos() != null) {
            for (String name : vo.photos()) {
                if (!safeName(name)) continue;
                FosterPhoto p = new FosterPhoto();
                p.setFosterId(vo.fosterId());
                p.setLogId(log.getLogId());
                p.setLogDate(log.getLogDate());
                p.setFileName(name);
                p.setIsDel(0L);
                photoMapper.insert(p);
            }
        }
        notificationService.notifyReport(f.getClientId(), "寄养日报更新",
                "您的宠物今日照护记录已更新，点开可查看文字与照片。",
                "FOSTER_LOG", "foster", vo.fosterId());
        return Result.success("照护记录已保存并通知主人");
    }

    @Override
    public FosterPhoto photoForRead(Integer photoId, Integer clientId) {
        if (photoId == null) return null;
        FosterPhoto p = photoMapper.selectById(photoId);
        if (p == null || (p.getIsDel() != null && p.getIsDel() != 0)) return null;
        if (clientId != null && !owns(p.getFosterId(), clientId)) return null;
        return p;
    }

    private boolean owns(Integer fosterId, Integer clientId) {
        if (fosterId == null || clientId == null) return false;
        return this.lambdaQuery()
                .eq(Foster::getFosterId, fosterId)
                .eq(Foster::getClientId, clientId)
                .count() > 0;
    }

    private static boolean safeName(String name) {
        return name != null && !name.isBlank()
                && !name.contains("..") && !name.contains("/") && !name.contains("\\");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
