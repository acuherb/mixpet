package com.mix.service.impl;

import com.mix.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.BillMapper;
import com.mix.mapper.PrescriptionMapper;
import com.mix.mapper.RxPrintSupplementMapper;
import com.mix.model.dto.RecordDto;
import com.mix.model.entity.Bill;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.entity.Prescription;
import com.mix.model.entity.RxPrintSupplement;
import com.mix.model.vo.RxArchiveVo;
import com.mix.model.vo.RxPrintPrepareVo;
import com.mix.model.vo.RxPrintResultVo;
import com.mix.model.vo.RxSheetVo;
import com.mix.model.vo.RxSupplementVo;
import com.mix.service.IMedicalRecordService;
import com.mix.service.IRxPrintService;
import com.mix.util.UserUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 兽用处方笺打印服务实现（734号国标）
 * 核心约束：
 * - 处方正文仍以 prescription/medical_record 为准，本服务只补「打印时才需要的字段」；
 * - 编号按天自增、作废占号不回收；并发防重号靠 uk_print_no 唯一索引冲突重试；
 * - 已有 VALID 记录复用同号补打；已 VOID 拒绝再打。
 */
@Service
public class RxPrintServiceImpl extends ServiceImpl<RxPrintSupplementMapper, RxPrintSupplement>
        implements IRxPrintService {

    private final PrescriptionMapper prescriptionMapper;
    private final IMedicalRecordService recordService;
    private final BillMapper billMapper;

    public RxPrintServiceImpl(PrescriptionMapper prescriptionMapper, IMedicalRecordService recordService,
                              BillMapper billMapper) {
        this.prescriptionMapper = prescriptionMapper;
        this.recordService = recordService;
        this.billMapper = billMapper;
    }

    /** 发药人：优先 bill.dispense_by（存姓名），未发药返回 null（补录或留空手写） */
    private String dispenserOf(Integer recordId) {
        Bill bill = billMapper.selectOne(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getRecordId, recordId)
                .eq(Bill::getIsDel, 0)
                .orderByDesc(Bill::getBillId).last("limit 1"));
        return bill == null ? null : bill.getDispenseBy();
    }

    /** 处方正文数据（处方笺主体） */
    private RxSheetVo sheetOf(Integer recordId) {
        return recordService.selectRxSheet(recordId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RxPrintResultVo prepare(RxPrintPrepareVo p) {
        if (p.getRecordId() == null) return RxPrintResultVo.fail("缺少病历ID");
        RecordDto record = recordService.selectRecordOne(p.getRecordId());
        if (record == null) return RxPrintResultVo.fail("病历不存在");
        // 定位处方行：入参优先，为空取该病历第一行
        List<Prescription> items = prescriptionMapper.selectByRecord(p.getRecordId());
        if (items == null || items.isEmpty()) return RxPrintResultVo.fail("该病历没有处方明细，无法打印处方笺");
        Integer prescriptionId = p.getPrescriptionId();
        if (prescriptionId == null) prescriptionId = items.get(0).getPrescriptionId();
        final Integer pid = prescriptionId;
        boolean owned = items.stream().anyMatch(x -> x.getPrescriptionId().equals(pid));
        if (!owned) return RxPrintResultVo.fail("该处方行不属于此病历");

        // 已有打印记录：VALID 复用 / VOID 拒绝
        RxPrintSupplement sup = baseMapper.selectOne(new LambdaQueryWrapper<RxPrintSupplement>()
                .eq(RxPrintSupplement::getPrescriptionId, pid).last("limit 1"));
        if (sup != null && "VOID".equals(sup.getStatus()))
            return RxPrintResultVo.fail("该处方笺编号已作废，如需重开请联系管理员");

        if (sup == null) {
            // 首次打印：生成编号 + 落 VALID 记录（冲突重试防并发重号）
            sup = new RxPrintSupplement();
            sup.setPrescriptionId(Long.valueOf(pid));
            sup.setRecordId(Long.valueOf(p.getRecordId()));
            sup.setStatus("VALID");
            sup.setCopies(p.getCopies() == null || (!p.getCopies().equals(1) && !p.getCopies().equals(3)) ? 1 : p.getCopies());
            sup.setPrintedBy(currentUserId());
            sup.setPrintedAt(LocalDateTime.now());
            applyFill(sup, p, dispenserOf(p.getRecordId()));
            if (!insertWithRetry(sup)) return RxPrintResultVo.fail("编号生成冲突，请重试");
        } else {
            // 补打：仅更新仍为空的补录字段；刷新打印时间与联次
            if (StrUtil.isWhite(sup.getHairColor()) && !StrUtil.isWhite(p.getHairColor())) sup.setHairColor(p.getHairColor());
            if (StrUtil.isWhite(sup.getSpecies()) && !StrUtil.isWhite(p.getSpecies())) sup.setSpecies(p.getSpecies());
            if (StrUtil.isWhite(sup.getWithdrawalPeriod()) && !StrUtil.isWhite(p.getWithdrawalPeriod()))
                sup.setWithdrawalPeriod(p.getWithdrawalPeriod());
            if (StrUtil.isWhite(sup.getDispenser()) && !StrUtil.isWhite(p.getDispenser())) sup.setDispenser(p.getDispenser());
            if (p.getCopies() != null && (p.getCopies().equals(1) || p.getCopies().equals(3))) sup.setCopies(p.getCopies());
            sup.setPrintedBy(currentUserId());
            sup.setPrintedAt(LocalDateTime.now());
            baseMapper.updateById(sup);
        }

        RxSheetVo sheet = sheetOf(p.getRecordId());
        RxSupplementVo supplement = toVo(sup);
        return RxPrintResultVo.ok(sheet, supplement);
    }

    /** 补录字段合并：本次入参 > 账单/宠物既有值（后续补打只补缺项） */
    private void applyFill(RxPrintSupplement sup, RxPrintPrepareVo p, String billDispenser) {
        if (!StrUtil.isWhite(p.getHairColor())) sup.setHairColor(p.getHairColor());
        if (!StrUtil.isWhite(p.getSpecies())) sup.setSpecies(p.getSpecies());
        // 宠物处方休药期固定"—"
        sup.setWithdrawalPeriod(!StrUtil.isWhite(p.getWithdrawalPeriod()) ? p.getWithdrawalPeriod() : "—");
        String dispenser = !StrUtil.isWhite(p.getDispenser()) ? p.getDispenser() : billDispenser;
        if (!StrUtil.isWhite(dispenser)) sup.setDispenser(dispenser);
    }

    /** 按天自增生成 RX-YYYYMMDD-XXXX；唯一索引冲突时重试（最多3次） */
    private boolean insertWithRetry(RxPrintSupplement sup) {
        for (int i = 0; i < 3; i++) {
            String no = nextPrintNo(LocalDate.now());
            sup.setPrintNo(no);
            try {
                baseMapper.insert(sup);
                return true;
            } catch (DuplicateKeyException e) {
                // 并发重号或处方重复插入：重算编号重试；处方唯一键冲突则直接失败
                if (baseMapper.selectCount(new LambdaQueryWrapper<RxPrintSupplement>()
                        .eq(RxPrintSupplement::getPrescriptionId, sup.getPrescriptionId())) > 0) return false;
            }
        }
        return false;
    }

    /** 取当天最大流水 +1 */
    private String nextPrintNo(LocalDate day) {
        String prefix = "RX-" + day.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        RxPrintSupplement max = baseMapper.selectOne(new LambdaQueryWrapper<RxPrintSupplement>()
                .likeRight(RxPrintSupplement::getPrintNo, prefix)
                .orderByDesc(RxPrintSupplement::getPrintNo).last("limit 1"));
        int seq = 1;
        if (max != null && max.getPrintNo() != null && max.getPrintNo().length() > prefix.length()) {
            try {
                seq = Integer.parseInt(max.getPrintNo().substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private Long currentUserId() {
        Object user = UserUtil.getUser();
        if (user instanceof Doctor d) return Long.valueOf(d.getDoctorId());
        return null;
    }

    private RxSupplementVo toVo(RxPrintSupplement sup) {
        RxSupplementVo vo = new RxSupplementVo();
        vo.setPrintNo(sup.getPrintNo());
        vo.setHairColor(sup.getHairColor());
        vo.setSpecies(sup.getSpecies());
        vo.setWithdrawalPeriod(sup.getWithdrawalPeriod());
        vo.setDispenser(sup.getDispenser());
        vo.setCopies(sup.getCopies());
        vo.setStatus(sup.getStatus());
        vo.setPrintedAt(sup.getPrintedAt());
        return vo;
    }

    @Override
    public Page<RxArchiveVo> archive(LocalDate startDate, LocalDate endDate, Integer doctorId,
                                     String keyword, Boolean printed, String status,
                                     int numPage, int pageSize) {
        return baseMapper.selectRxArchive(new Page<>(numPage, pageSize),
                startDate, endDate, doctorId, keyword, printed, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean voidBy(String printNo, String reason) {
        if (StrUtil.isWhite(printNo)) return false;
        RxPrintSupplement sup = baseMapper.selectOne(new LambdaQueryWrapper<RxPrintSupplement>()
                .eq(RxPrintSupplement::getPrintNo, printNo).last("limit 1"));
        if (sup == null) return false;
        if ("VOID".equals(sup.getStatus())) return true; // 幂等
        sup.setStatus("VOID");
        sup.setVoidBy(currentUserId());
        sup.setVoidAt(LocalDateTime.now());
        sup.setVoidReason(StrUtil.isWhite(reason) ? "未填原因" : reason);
        baseMapper.updateById(sup);
        return true;
    }
}
