package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.dto.JobDto;
import com.ship.model.dto.NameDo;
import com.ship.model.entity.Job;
import com.ship.security.model.RoleConst;
import com.ship.service.IJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医院职务/职称字典管理
 */
@RestController
@RequestMapping("/job")
public class JobController {
    private final IJobService jobService;

    @Autowired
    public JobController(IJobService jobService) {
        this.jobService = jobService;
    }

    /** 下拉用：全部职称（登录即可，医生自选职称也用） */
    @GetMapping
    public List<NameDo> getName() {
        return jobService.selectName();
    }

    /** 分页（带医生数） */
    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<JobDto> getPage(int numPage, int pageSize, String jobName) {
        return jobService.selectByPage(jobName, numPage, pageSize);
    }

    /** 新增 */
    @PostMapping
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody Job job) {
        if (job.getJobName() == null || job.getJobName().isBlank()) return Result.error("请输入职称名称");
        if (job.getSort() == null) job.setSort(99);
        return Result.choice("添加", jobService.save(job));
    }

    /** 修改 */
    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody Job job) {
        if (job.getJobId() == null) return Result.error("缺少职称id");
        if (job.getJobName() == null || job.getJobName().isBlank()) return Result.error("请输入职称名称");
        return Result.choice("修改", jobService.updateById(job));
    }

    /** 删除单个 */
    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return jobService.deleteById(id);
    }

    /** 批量删除 */
    @DeleteMapping("/batch/{ids}")
    @Secured(RoleConst.MANAGER)
    public Result deleteGroup(@PathVariable List<Integer> ids) {
        return jobService.deleteByIds(ids);
    }
}
