package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.Result;
import com.mix.mapper.DepartmentMapper;
import com.mix.mapper.DoctorDepartMapper;
import com.mix.model.dto.DepartmentDto;
import com.mix.model.entity.Department;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.DoctorDepart;
import com.mix.model.dto.NameDo;
import com.mix.service.IDepartmentService;
import com.mix.service.IDoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 部门表 服务实现类
 * </p>
 *
 * @author MixJade
 * @since 2022-12-22
 */
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements IDepartmentService {

    private final IDoctorService doctorService;
    private final DoctorDepartMapper doctorDepartMapper;

    @Autowired
    public DepartmentServiceImpl(IDoctorService doctorService, DoctorDepartMapper doctorDepartMapper) {
        this.doctorService = doctorService;
        this.doctorDepartMapper = doctorDepartMapper;
    }

    /**
     * 检查科室下是否还有医生（经关系表）
     */
    private boolean hasDoctor(Integer departmentId) {
        return doctorDepartMapper.selectCount(com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<DoctorDepart>lambdaQuery()
                .eq(DoctorDepart::getDepartmentId, departmentId)) > 0;
    }

    @Override
    public Result deleteById(Integer departmentId) {
        if (hasDoctor(departmentId))
            return Result.error("当前科室下有人");
        return Result.choice("删除单个", this.lambdaUpdate()
                .eq(Department::getDepartmentId, departmentId)
                .set(Department::getIsDel, System.currentTimeMillis())
                .update());
    }

    @Override
    public Result deleteByIds(List<Integer> idGroup) {
        if (idGroup.size() < 1) return Result.error("请选择数据"); // IN语句中没有数据会报错
        int cnt = 0;
        for (Integer id : idGroup) if (hasDoctor(id)) cnt++;
        if (cnt > 0) {
            if (idGroup.size() == 1) return Result.error("所选科室有人");
            else return Result.error("部分所选科室有人");
        }
        return Result.choice("删除多个", lambdaUpdate()
                .in(Department::getDepartmentId, idGroup)
                .set(Department::getIsDel, System.currentTimeMillis())
                .update());
    }

    @Override
    public IPage<DepartmentDto> selectByPage(String departmentName, int numPage, int pageSize) {
        return baseMapper.selectDepartmentPage(new Page<>(numPage, pageSize), departmentName);
    }

    @Override
    public List<NameDo> selectName() {
        return baseMapper.selectName();
    }

    @Override
    public List<NameDo> selectNameHaveDoctor(Integer departmentId) {
        return baseMapper.selectNameHaveDoctor(departmentId);
    }

    @Override
    public boolean updateDepart(Department department) {
        return baseMapper.updateDepart(department);
    }
}