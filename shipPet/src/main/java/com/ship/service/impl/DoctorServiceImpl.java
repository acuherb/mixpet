package com.ship.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ship.common.PhotoEnum;
import com.ship.mapper.DoctorDepartMapper;
import com.ship.mapper.DoctorMapper;
import com.ship.model.dto.DoctorDto;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.DoctorDepart;
import com.ship.model.dto.NameDo;
import com.ship.service.IDoctorService;
import com.ship.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 医生表，外键部门 服务实现类
 * </p>
 *
 * @author MixJade
 * @since 2023-01-02
 */
@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements IDoctorService {
    private final DoctorDepartMapper doctorDepartMapper;

    @Autowired
    public DoctorServiceImpl(DoctorDepartMapper doctorDepartMapper) {
        this.doctorDepartMapper = doctorDepartMapper;
    }

    /** 新建医生/重置密码默认口令 */
    @Value("${app.default-password:123456}")
    private String defaultPwd;

    @Override
    public boolean deleteById(Integer doctorId) {
        boolean del = this.lambdaUpdate()
                .eq(Doctor::getDoctorId, doctorId)
                .set(Doctor::getIsDel, System.currentTimeMillis())
                .update();
        if (del) doctorDepartMapper.deleteByDoctorId(doctorId);
        return del;
    }

    @Override
    public boolean deleteByIds(List<Integer> idGroup) {
        boolean del = this.lambdaUpdate()
                .in(Doctor::getDoctorId, idGroup)
                .set(Doctor::getIsDel, System.currentTimeMillis())
                .update();
        if (del) idGroup.forEach(doctorDepartMapper::deleteByDoctorId);
        return del;
    }

    @Override
    public IPage<DoctorDto> selectByPage(String doctorName, String departmentName, Boolean certNull, int numPage, int pageSize) {
        return baseMapper.selectDoctorPage(new Page<>(numPage, pageSize), doctorName, departmentName, certNull);
    }

    @Override
    public IPage<DoctorDto> selectByPagePublic(String doctorName, String departmentName, int numPage, int pageSize) {
        return baseMapper.selectDoctorPagePublic(new Page<>(numPage, pageSize), doctorName, departmentName);
    }

    @Override
    public DoctorDto selectById(Integer doctorId) {
        DoctorDto dto = baseMapper.selectOneById(doctorId);
        if (dto == null) return null;
        List<NameDo> departs = doctorDepartMapper.selectDepartNamesByDoctorId(doctorId);
        if (departs != null && !departs.isEmpty()) {
            dto.setDepartmentIds(departs.stream().map(NameDo::roleId).collect(Collectors.toList()));
            dto.setDepartmentNames(departs.stream().map(NameDo::roleName).collect(Collectors.toList()));
        }
        return dto;
    }

    @Override
    public List<NameDo> selectName() {
        return baseMapper.selectName();
    }

    @Override
    public List<NameDo> selectNameByDepartment(Integer departmentId) {
        return doctorDepartMapper.selectDoctorNamesByDepartmentId(departmentId);
    }

    @Override
    public List<Doctor> selectByDepartmentId(Integer departmentId) {
        return doctorDepartMapper.selectDoctorsByDepartmentId(departmentId);
    }

    /**
     * 同步医生的多科室关系：先删后插
     */
    private void syncDepartments(Integer doctorId, List<Integer> departmentIds) {
        doctorDepartMapper.deleteByDoctorId(doctorId);
        if (departmentIds == null || departmentIds.isEmpty()) return;
        // 去重
        List<Integer> distinct = departmentIds.stream().distinct().collect(Collectors.toList());
        for (Integer depId : distinct) {
            if (depId == null) continue;
            DoctorDepart rel = new DoctorDepart();
            rel.setDoctorId(doctorId);
            rel.setDepartmentId(depId);
            doctorDepartMapper.insert(rel);
        }
    }

    @Override
    @Transactional
    public boolean addDoctor(Doctor doctor) {
        if (doctor.getAuthLv() == null) doctor.setAuthLv(0);
        if (StrUtil.isWhite(doctor.getDoctorInfo())) doctor.setDoctorInfo("暂时没有简介");
        if (StrUtil.isWhite(doctor.getDoctorPhoto())) doctor.setDoctorPhoto(PhotoEnum.DOCTOR.getPhotoName());
        if (StrUtil.isWhite(doctor.getDoctorPassword())) doctor.setDoctorPassword(defaultPwd);
        doctor.setDoctorPassword(StrUtil.tranPwd(doctor.getDoctorPassword()));
        boolean ok = save(doctor);
        if (ok && doctor instanceof DoctorDto) {
            DoctorDto dto = (DoctorDto) doctor;
            syncDepartments(doctor.getDoctorId(), dto.getDepartmentIds());
        } else if (ok && doctor.getDepartmentId() != null) {
            // 兼容：仅填主科室时也写进关系表（去除无意义的 if/else 死代码）
            syncDepartments(doctor.getDoctorId(), List.of(doctor.getDepartmentId()));
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean updateById(Doctor doctor) {
        boolean ok = super.updateById(doctor);
        if (ok && doctor instanceof DoctorDto) {
            DoctorDto dto = (DoctorDto) doctor;
            syncDepartments(doctor.getDoctorId(), dto.getDepartmentIds());
        }
        return ok;
    }

    @Override
    public boolean updateSelf(Doctor doctor) {
        return baseMapper.updateSelf(doctor);
    }
}