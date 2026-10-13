package com.ship.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ship.mapper.BillMapper;
import com.ship.mapper.HospitalMapper;
import com.ship.mapper.ImageOrderMapper;
import com.ship.mapper.ImmuneRecordMapper;
import com.ship.mapper.LabOrderMapper;
import com.ship.mapper.StatisticsMapper;
import com.ship.model.entity.Bill;
import com.ship.model.entity.HospitalRecord;
import com.ship.model.entity.ImageOrder;
import com.ship.model.entity.ImmuneRecord;
import com.ship.model.entity.LabOrder;
import com.ship.model.vo.StatOverviewVo;
import com.ship.service.IStatisticsService;
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

    @Autowired
    public StatisticsServiceImpl(StatisticsMapper statisticsMapper, BillMapper billMapper,
                                 HospitalMapper hospitalMapper, LabOrderMapper labOrderMapper,
                                 ImageOrderMapper imageOrderMapper, ImmuneRecordMapper immuneRecordMapper) {
        this.statisticsMapper = statisticsMapper;
        this.billMapper = billMapper;
        this.hospitalMapper = hospitalMapper;
        this.labOrderMapper = labOrderMapper;
        this.imageOrderMapper = imageOrderMapper;
        this.immuneRecordMapper = immuneRecordMapper;
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
        vo.setRevenueTrend(statisticsMapper.revenueTrend(LocalDateTime.of(today.minusDays(6), LocalTime.MIN)));
        vo.setDoctorTop(statisticsMapper.doctorTop(5));
        return vo;
    }
}