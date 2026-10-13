package com.ship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.model.dto.JobDto;
import com.ship.model.dto.NameDo;
import com.ship.model.entity.Job;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 职称字典 Mapper
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
    /** 分页联查（带该职称下医生数） */
    IPage<JobDto> selectJobPage(IPage<JobDto> page, String jobName);

    /** 下拉用：职称 id + 名称 */
    List<NameDo> selectName();
}
