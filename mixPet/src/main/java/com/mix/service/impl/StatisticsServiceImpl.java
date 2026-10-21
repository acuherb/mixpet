package com.mix.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mix.mapper.AppointmentMapper;
import com.mix.mapper.BillMapper;
import com.mix.mapper.ClientMapper;
import com.mix.mapper.DrugMapper;
import com.mix.mapper.HospitalMapper;
import com.mix.mapper.ImageOrderMapper;
import com.mix.mapper.ImmuneRecordMapper;
import com.mix.mapper.LabOrderMapper;
import com.mix.mapper.MedicalRecordMapper;
import com.mix.mapper.StatisticsMapper;
import com.mix.model.entity.Appointment;
import com.mix.model.entity.Bill;
import com.mix.model.entity.Client;
import com.mix.model.entity.Drug;
import com.mix.model.entity.HospitalRecord;
import com.mix.model.entity.ImageOrder;
import com.mix.model.entity.ImmuneRecord;
import com.mix.model.entity.LabOrder;
import com.mix.model.entity.MedicalRecord;
import com.mix.model.vo.StatOverviewVo;
import com.mix.service.IStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 统计看板 Service 实现
 */
@Service
public class StatisticsServiceImpl implements IStatisticsService {
    private final StatisticsMapper statisticsMapper;
    private final BillMapper billMapper;
    private final HospitalMapper hospitalMapper;
    private final LabOrderMapper labOrderMapper;
    private final ImageOrderMapper imageOrderMapper;
    private final ImmuneRecordMapper immuneRecordMapper;
    private final AppointmentMapper appointmentMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final ClientMapper clientMapper;
    private final DrugMapper drugMapper;

    @Autowired
    public StatisticsServiceImpl(StatisticsMapper statisticsMapper, BillMapper billMapper,
                                 HospitalMapper hospitalMapper, LabOrderMapper labOrderMapper,
                                 ImageOrderMapper imageOrderMapper, ImmuneRecordMapper immuneRecordMapper,
                                 AppointmentMapper appointmentMapper, MedicalRecordMapper medicalRecordMapper,
                                 ClientMapper clientMapper, DrugMapper drugMapper) {
        this.statisticsMapper = statisticsMapper;
        this.billMapper = billMapper;
        this.hospitalMapper = hospitalMapper;
        this.labOrderMapper = labOrderMapper;
        this.imageOrderMapper = imageOrderMapper;
        this.immuneRecordMapper = immuneRecordMapper;
        this.appointmentMapper = appointmentMapper;
        this.medicalRecordMapper = medicalRecordMapper;
        this.clientMapper = clientMapper;
        this.drugMapper = drugMapper;
    }

    @Override
    public StatOverviewVo selectOverview() {
        LocalDate today = LocalDate.now();
        StatOverviewVo vo = new StatOverviewVo();
        vo.setTodayRevenue(statisticsMapper.sumRevenue(LocalDateTime.of(today, LocalTime.MIN)));
        vo.setMonthRevenue(statisticsMapper.sumRevenue(LocalDateTime.of(today.withDayOfMonth(1), LocalTime.MIN)));
        vo.setPendingBills(billMapper.selectCount(Wrappers.<Bill>lambdaQuery().eq(Bill::getPayStatus, false)));
        vo.setInHospital(hospitalMapper.selectCount(Wrappers.<HospitalRecord>lambdaQuery().eq(HospitalRecord::getStatus, false)));
        vo.setPendingLab(labOrderMapper.selectCount(Wrappers.<LabOrder>lambdaQuery().eq(LabOrder::getStatus, false)));
        vo.setPendingImage(imageOrderMapper.selectCount(Wrappers.<ImageOrder>lambdaQuery().eq(ImageOrder::getStatus, false)));
        vo.setDueVaccine(immuneRecordMapper.selectCount(Wrappers.<ImmuneRecord>lambdaQuery()
                .ge(ImmuneRecord::getNextTime, LocalDateTime.now())
                .le(ImmuneRecord::getNextTime, LocalDateTime.now().plusDays(30))));
        // 今日挂号 / 今日接诊
        vo.setTodayAppoint(appointmentMapper.selectCount(Wrappers.<Appointment>lambdaQuery()
                .ge(Appointment::getCreateTime, LocalDateTime.of(today, LocalTime.MIN))));
        vo.setTodayRecord(medicalRecordMapper.selectCount(Wrappers.<MedicalRecord>lambdaQuery()
                .ge(MedicalRecord::getCreateTime, LocalDateTime.of(today, LocalTime.MIN))));
        // 客户总数（启用）
        vo.setClientTotal(clientMapper.selectCount(Wrappers.<Client>lambdaQuery()
                .eq(Client::getActive, true)));
        // 待发药：已收款、未退款、未发药
        vo.setPendingDispense(billMapper.selectCount(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getPayStatus, true)
                .and(w -> w.eq(Bill::getRefundStatus, false).or().isNull(Bill::getRefundStatus))
                .and(w -> w.eq(Bill::getDispenseStatus, false).or().isNull(Bill::getDispenseStatus))));
        // 低库存药品：设置过阈值且库存<=阈值
        vo.setLowStock(drugMapper.selectCount(Wrappers.<Drug>lambdaQuery()
                .eq(Drug::getActive, true)
                .apply("warning_stock > 0 AND drug_stock <= warning_stock")));
        // 近效期药品（90天内到期，含当天）
        vo.setExpiringDrug(drugMapper.selectCount(Wrappers.<Drug>lambdaQuery()
                .eq(Drug::getActive, true)
                .isNotNull(Drug::getExpireDate)
                .ge(Drug::getExpireDate, today)
                .le(Drug::getExpireDate, today.plusDays(90))));
        vo.setRevenueTrend(statisticsMapper.revenueTrend(LocalDateTime.of(today.minusDays(6), LocalTime.MIN)));
        vo.setDoctorTop(statisticsMapper.doctorTop(5));
        return vo;
    }
}