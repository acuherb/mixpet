package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.Result;
import com.mix.common.BizException;
import com.mix.mapper.DoctorMapper;
import com.mix.mapper.JobMapper;
import com.mix.model.dto.JobDto;
import com.mix.model.dto.NameDo;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Job;
import com.mix.service.IJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 职称字典 Service 实现
 */
@Service
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements IJobService {
    private final DoctorMapper doctorMapper;

    @Autowired
    public JobServiceImpl(DoctorMapper doctorMapper) {
        this.doctorMapper = doctorMapper;
    }

    /** 该职称下是否还有在职医生 */
    private boolean hasDoctor(Integer jobId) {
        return doctorMapper.selectCount(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getJobId, jobId)) > 0;
    }

    @Override
    public IPage<JobDto> selectByPage(String jobName, int numPage, int pageSize) {
        return baseMapper.selectJobPage(new Page<>(numPage, pageSize), jobName);
    }

    @Override
    public List<NameDo> selectName() {
        return baseMapper.selectName();
    }

    @Override
    public Result deleteById(Integer jobId) {
        if (hasDoctor(jobId)) throw new BizException("该职称下还有医生，不能删除");
        return Result.choice("删除", this.lambdaUpdate()
                .eq(Job::getJobId, jobId)
                .set(Job::getIsDel, System.currentTimeMillis())
                .update());
    }

    @Override
    public Result deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) throw new BizException("请选择数据");
        for (Integer id : ids) {
            if (hasDoctor(id)) throw new BizException("所选职称含在职医生，不能删除");
        }
        return Result.choice("批量删除", this.lambdaUpdate()
                .in(Job::getJobId, ids)
                .set(Job::getIsDel, System.currentTimeMillis())
                .update());
    }
}
