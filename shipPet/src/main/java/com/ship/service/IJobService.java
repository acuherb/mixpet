package com.ship.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ship.common.Result;
import com.ship.model.dto.JobDto;
import com.ship.model.dto.NameDo;
import com.ship.model.entity.Job;

import java.util.List;

/**
 * 职称字典 Service
 */
public interface IJobService extends IService<Job> {
    /** 分页（带医生数） */
    IPage<JobDto> selectByPage(String jobName, int numPage, int pageSize);

    /** 下拉用 id+名称 */
    List<NameDo> selectName();

    /** 删除单个（含在职医生则拒绝） */
    Result deleteById(Integer jobId);

    /** 批量删除（含在职医生则拒绝） */
    Result deleteByIds(List<Integer> ids);
}
