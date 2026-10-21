package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.dto.DoctorDto;
import com.mix.model.entity.Doctor;
import com.mix.model.dto.NameDo;
import com.mix.security.model.RoleConst;
import com.mix.service.AvatarFileService;
import com.mix.service.IDoctorService;
import com.mix.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 医生表，外键部门 前端控制器
 * </p>
 *
 * @author MixJade
 * @since 2022-12-22
 */
@RestController
@RequestMapping("/doctor")
public class DoctorController {
    private final IDoctorService doctorService;
    private final AvatarFileService avatarFileService;

    @Autowired
    public DoctorController(IDoctorService doctorService, AvatarFileService avatarFileService) {
        this.doctorService = doctorService;
        this.avatarFileService = avatarFileService;
    }

    /**
     * 管理员：在添加科室主任时，查询医生姓名
     *
     * @return 医生姓名与id
     */
    @GetMapping
    @Secured({RoleConst.ADMIN, RoleConst.MANAGER, RoleConst.NURSE})
    public List<NameDo> getName() {
        return doctorService.selectName();
    }

    /**
     * 管理员：在添加挂号单时，通过科室查询医生
     *
     * @param departmentId 科室id
     * @return 医生姓名与id
     */
    @GetMapping("/department")
    @Secured({RoleConst.ADMIN, RoleConst.MANAGER, RoleConst.NURSE})
    public List<NameDo> getByDepartment(Integer departmentId) {
        return doctorService.selectNameByDepartment(departmentId);
    }

    /**
     * 科室管理：查看一个科室下面的所有医生
     *
     * @param departmentId 科室id
     * @return 对应科室的所有医生信息
     */
    @GetMapping("/department/detail")
    @Secured({RoleConst.ADMIN, RoleConst.MANAGER})
    public List<Doctor> getByDepartmentId(Integer departmentId) {
        return doctorService.selectByDepartmentId(departmentId);
    }

    @GetMapping("/page")
    @Secured({RoleConst.ADMIN, RoleConst.MANAGER})
    public IPage<DoctorDto> getPage(int numPage, int pageSize, String doctorName, String departmentName, Boolean certNull) {
        return doctorService.selectByPage(doctorName, departmentName, certNull, numPage, pageSize);
    }

    /**
     * 顾客端展示医生分页（脱敏：不返回电话/邮箱/账号）
     */
    @GetMapping("/view")
    public IPage<DoctorDto> getView(int numPage, int pageSize, String doctorName, String departmentName) {
        return doctorService.selectByPagePublic(doctorName, departmentName, numPage, pageSize);
    }

    /**
     * 根据id查询单个医生
     *
     * @param doctorId 网页读取get参数，必须用包装类，因为可能不是数字
     * @return 医生信息
     */
    @GetMapping("/one")
    public DoctorDto getOne(Integer doctorId) {
        DoctorDto dto = doctorService.selectById(doctorId);
        // 顾客端详情：脱敏，不暴露完整手机号/邮箱/账号
        if (dto != null) {
            dto.setUsername(null);
            dto.setDoctorEmail(null);
            String tel = dto.getDoctorTel();
            if (tel != null && tel.matches("^1[3-9]\\d{9}$")) {
                dto.setDoctorTel(tel.substring(0, 3) + "****" + tel.substring(7));
            } else if (tel != null && tel.length() >= 7) {
                dto.setDoctorTel(tel.substring(0, 3) + "****" + tel.substring(tel.length() - 4));
            }
        }
        return dto;
    }

    @PostMapping
    @Secured(RoleConst.ADMIN)
    public Result save(@RequestBody DoctorDto doctor) {
        if (StrUtil.isWhite(doctor.getUsername())) return Result.error("帐号不能为空");
        if (StrUtil.isWhite(doctor.getDoctorName())) return Result.error("姓名不能为空");
        return Result.choice("添加", doctorService.addDoctor(doctor));
    }

    @DeleteMapping("/{id}")
    @Secured(RoleConst.ADMIN)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除单个", doctorService.deleteById(id));
    }

    @DeleteMapping("/batch/{ids}")
    @Secured(RoleConst.ADMIN)
    public Result deleteGroup(@PathVariable List<Integer> ids) {
        return Result.choice("删除多个", doctorService.deleteByIds(ids));
    }

    @PutMapping
    @Secured(RoleConst.ADMIN)
    public Result update(@RequestBody DoctorDto doctor) {
        if (StrUtil.isWhite(doctor.getDoctorName())) return Result.error("姓名不能为空");
        if (doctor.getAuthLv() == null) doctor.setAuthLv(0);
        // 手机号非空时必须为 11 位
        if (!StrUtil.isWhite(doctor.getDoctorTel()) && !doctor.getDoctorTel().matches("^1[3-9]\\d{9}$"))
            return Result.error("手机号格式不正确（11位，1 开头）");
        // 邮箱非空时校验
        if (!StrUtil.isWhite(doctor.getDoctorEmail()) && !doctor.getDoctorEmail().matches("^[A-Za-z0-9][A-Za-z0-9._]*@[A-Za-z0-9]+\\.[A-Za-z0-9.-]+[A-Za-z]$"))
            return Result.error("邮箱格式不正确");
        // 更换头像时删除旧头像文件
        Doctor oldDoctor = doctorService.getById(doctor.getDoctorId());
        if (oldDoctor != null)
            avatarFileService.removeOldAvatar(oldDoctor.getDoctorPhoto(), doctor.getDoctorPhoto());
        return Result.choice("修改", doctorService.updateById(doctor));
    }
}