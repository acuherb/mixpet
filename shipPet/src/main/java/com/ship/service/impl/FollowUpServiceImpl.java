package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.Result;
import com.ship.mapper.FollowUpMapper;
import com.ship.mapper.MedicalRecordMapper;
import com.ship.model.dto.FollowUpDto;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.FollowUp;
import com.ship.model.entity.MedicalRecord;
import com.ship.service.IFollowUpService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 随访记录 Service 实现
 */
@Service
public class FollowUpServiceImpl extends ServiceImpl<FollowUpMapper, FollowUp> implements IFollowUpService {
    private final MedicalRecordMapper recordMapper;

    @Autowired
    public FollowUpServiceImpl(MedicalRecordMapper recordMapper) {
        this.recordMapper = recordMapper;
    }

    private String currentOperator() {
        Object u = UserUtil.getUser();
        if (u instanceof Doctor d) return d.getDoctorName();
        if (u instanceof Client c) return c.getClientName();
        return "系统";
    }

    @Override
    public Result add(FollowUp f) {
        if (f == null || f.getRecordId() == null) return Result.error("缺少病历id");
        MedicalRecord r = recordMapper.selectById(f.getRecordId());
        if (r == null) return Result.error("病历不存在");
        f.setFollowId(null);
        f.setPetId(r.getPetId());
        f.setClientId(r.getClientId());
        f.setStatus(false);
        f.setFollowBy(currentOperator());
        if (f.getFollowType() == null || f.getFollowType().isBlank()) f.setFollowType("电话");
        this.save(f);
        return Result.success("随访计划已创建");
    }

    @Override
    public List<FollowUp> listByRecord(Integer recordId) {
        return this.lambdaQuery()
                .eq(FollowUp::getRecordId, recordId)
                .orderByDesc(FollowUp::getPlanTime)
                .list();
    }

    @Override
    public IPage<FollowUpDto> selectPage(Boolean status, String seaName, int numPage, int pageSize) {
        return baseMapper.selectFollowPage(new Page<>(numPage, pageSize), status, seaName);
    }

    @Override
    public Result done(Integer followId, String result) {
        FollowUp f = this.getById(followId);
        if (f == null) return Result.error("随访记录不存在");
        if (Boolean.TRUE.equals(f.getStatus())) return Result.error("该随访已完成");
        this.lambdaUpdate()
                .eq(FollowUp::getFollowId, followId)
                .set(FollowUp::getStatus, true)
                .set(FollowUp::getResult, result)
                .set(FollowUp::getFollowTime, LocalDateTime.now())
                .set(FollowUp::getFollowBy, currentOperator())
                .update();
        return Result.success("随访已完成");
    }
}
