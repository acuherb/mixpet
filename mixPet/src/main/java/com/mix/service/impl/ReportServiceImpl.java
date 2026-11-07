package com.mix.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mix.mapper.BillMapper;
import com.mix.mapper.DepartmentMapper;
import com.mix.mapper.DoctorMapper;
import com.mix.mapper.DrugMapper;
import com.mix.mapper.MedicalRecordMapper;
import com.mix.mapper.PrescriptionMapper;
import com.mix.model.entity.Bill;
import com.mix.model.entity.Department;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Drug;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.entity.Prescription;
import com.mix.model.vo.FinanceReport;
import com.mix.service.IReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 经营报表 Service 实现
 */
@Service
public class ReportServiceImpl implements IReportService {
    private final BillMapper billMapper;
    private final MedicalRecordMapper recordMapper;
    private final DepartmentMapper departmentMapper;
    private final DoctorMapper doctorMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final DrugMapper drugMapper;

    @Autowired
    public ReportServiceImpl(BillMapper billMapper, MedicalRecordMapper recordMapper,
                             DepartmentMapper departmentMapper, DoctorMapper doctorMapper,
                             PrescriptionMapper prescriptionMapper, DrugMapper drugMapper) {
        this.billMapper = billMapper;
        this.recordMapper = recordMapper;
        this.departmentMapper = departmentMapper;
        this.doctorMapper = doctorMapper;
        this.prescriptionMapper = prescriptionMapper;
        this.drugMapper = drugMapper;
    }

    private static BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    @Override
    public FinanceReport finance(LocalDate start, LocalDate end) {
        FinanceReport r = new FinanceReport();
        r.setStartDate(start);
        r.setEndDate(end);
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay(); // [start, end]

        // 统一按独立收款时间 pay_time 统计；退款不整单排除，而是扣减累计已退金额（与 StatisticsMapper 同口径）
        List<Bill> paid = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getPayStatus, true)
                .ge(Bill::getPayTime, startDt).lt(Bill::getPayTime, endDt));
        // 区间内发生退款的账单
        List<Bill> refunded = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .gt(Bill::getRefundMoney, BigDecimal.ZERO)
                .ge(Bill::getPayTime, startDt).lt(Bill::getPayTime, endDt));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal fReg = BigDecimal.ZERO, fTreat = BigDecimal.ZERO, fDrug = BigDecimal.ZERO, fLab = BigDecimal.ZERO;
        BigDecimal fImg = BigDecimal.ZERO, fPkg = BigDecimal.ZERO, fSur = BigDecimal.ZERO, fHos = BigDecimal.ZERO;
        Map<LocalDate, BigDecimal> trend = new TreeMap<>();
        List<Integer> recordIds = new ArrayList<>();
        for (Bill b : paid) {
            BigDecimal real = (b.getRealMoney() != null ? b.getRealMoney() : nz(b.getTotalMoney()))
                    .subtract(nz(b.getRefundMoney())).max(BigDecimal.ZERO);
            total = total.add(real);
            discountTotal = discountTotal.add(nz(b.getDiscountMoney()));
            fReg = fReg.add(nz(b.getFeeRegister()));
            fTreat = fTreat.add(nz(b.getFeeTreat()));
            fDrug = fDrug.add(nz(b.getFeeDrug()));
            fLab = fLab.add(nz(b.getFeeLab()));
            fImg = fImg.add(nz(b.getFeeImage()));
            fPkg = fPkg.add(nz(b.getFeePackage()));
            fSur = fSur.add(nz(b.getFeeSurgery()));
            fHos = fHos.add(nz(b.getFeeHospital()));
            if (b.getPayTime() != null)
                trend.merge(b.getPayTime().toLocalDate(), real, BigDecimal::add);
            if (b.getRecordId() != null) recordIds.add(b.getRecordId());
        }
        r.setTotalRevenue(total);
        r.setDiscountMoney(discountTotal);
        r.setOrderCount(paid.size());
        r.setAvgOrder(paid.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(paid.size()), 2, RoundingMode.HALF_UP));
        r.setFeeRegister(fReg);
        r.setFeeTreat(fTreat);
        r.setFeeDrug(fDrug);
        r.setFeeLab(fLab);
        r.setFeeImage(fImg);
        r.setFeePackage(fPkg);
        r.setFeeSurgery(fSur);
        r.setFeeHospital(fHos);
        BigDecimal refundAmt = refunded.stream().map(b -> nz(b.getRefundMoney()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        r.setRefundCount(refunded.size());
        r.setRefundAmount(refundAmt);
        r.setTrend(trend.entrySet().stream()
                .map(e -> new FinanceReport.TrendItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));

        // 按科室 / 按医生
        if (!recordIds.isEmpty()) {
            Map<Integer, MedicalRecord> recMap = recordMapper.selectList(Wrappers.<MedicalRecord>lambdaQuery()
                            .in(MedicalRecord::getRecordId, recordIds)).stream()
                    .collect(Collectors.toMap(MedicalRecord::getRecordId, x -> x, (a, b) -> a));
            Map<Integer, BigDecimal> deptSum = new HashMap<>();
            Map<Integer, BigDecimal> docSum = new HashMap<>();
            for (Bill b : paid) {
                MedicalRecord rec = recMap.get(b.getRecordId());
                if (rec == null) continue;
                BigDecimal net = nz(b.getRealMoney()).subtract(nz(b.getRefundMoney())).max(BigDecimal.ZERO);
                if (rec.getDepartmentId() != null) deptSum.merge(rec.getDepartmentId(), net, BigDecimal::add);
                if (rec.getDoctorId() != null) docSum.merge(rec.getDoctorId(), net, BigDecimal::add);
            }
            Map<Integer, String> deptNames = departmentMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Department::getDepartmentId, Department::getDepartmentName, (a, b) -> a));
            Map<Integer, String> docNames = doctorMapper.selectList(null).stream()
                    .collect(Collectors.toMap(Doctor::getDoctorId, Doctor::getDoctorName, (a, b) -> a));
            Comparator<FinanceReport.NameAmount> byAmtDesc = Comparator.comparing(
                    (FinanceReport.NameAmount na) -> na.getAmount()).reversed();
            r.setByDepartment(deptSum.entrySet().stream()
                    .map(e -> new FinanceReport.NameAmount(deptNames.getOrDefault(e.getKey(), "未知科室"), e.getValue()))
                    .sorted(byAmtDesc).collect(Collectors.toList()));
            r.setByDoctor(docSum.entrySet().stream()
                    .map(e -> new FinanceReport.NameAmount(docNames.getOrDefault(e.getKey(), "未知医生"), e.getValue()))
                    .sorted(byAmtDesc).collect(Collectors.toList()));
        } else {
            r.setByDepartment(List.of());
            r.setByDoctor(List.of());
        }

        // 药品成本/毛利（成本按当前进货价估算：未快照历史进货价）
        BigDecimal drugCost = BigDecimal.ZERO;
        if (!recordIds.isEmpty()) {
            List<Prescription> rx = prescriptionMapper.selectList(Wrappers.<Prescription>lambdaQuery()
                    .in(Prescription::getRecordId, recordIds)
                    .and(w -> w.eq(Prescription::getIsCancel, false).or().isNull(Prescription::getIsCancel)));
            for (Prescription p : rx) {
                if (p.getDrugId() == null || p.getQuantity() == null) continue;
                Drug d = drugMapper.selectById(p.getDrugId());
                if (d != null && d.getCostPrice() != null)
                    drugCost = drugCost.add(d.getCostPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
            }
        }
        r.setDrugCost(drugCost);
        r.setDrugProfit(fDrug.subtract(drugCost));
        return r;
    }
}
