package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.dto.AppointmentDto;
import com.ship.model.entity.Appointment;
import com.ship.model.entity.Client;
import com.ship.security.model.RoleConst;
import com.ship.service.IAppointmentService;
import com.ship.service.IPetService;
import com.ship.model.entity.Pet;
import com.ship.util.StrUtil;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 挂号单表 前端控制器
 * </p>
 *
 * @author MixJade
 * @since 2022-12-22
 */
@RestController
@RequestMapping("/appointment")
public class AppointmentController {
    private final IAppointmentService appointmentService;
    private final IPetService petService;

    /** 每位医生每日号源上限 */
    @Value("${appointment.max-per-day:20}")
    private int maxPerDay;

    @Autowired
    public AppointmentController(IAppointmentService appointmentService, IPetService petService) {
        this.appointmentService = appointmentService;
        this.petService = petService;
    }

    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<AppointmentDto> getPage(int numPage, int pageSize, String seaName, int seaType) {
        return appointmentService.selectByPage(seaName, seaType, numPage, pageSize);
    }

    /**
     * 根据医生查询挂号信息
     */
    @GetMapping("/doctor")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public List<AppointmentDto> getDoctor(Integer doctorId) {
        return appointmentService.getDoctor(doctorId);
    }

    /**
     * 用户个人中心:根据用户查询挂号信息
     */
    @GetMapping("/client")
    public List<AppointmentDto> getClient() {
        if (UserUtil.getUser() instanceof Client client) {
            return appointmentService.getClient(client.getClientId());
        } else return null;
    }

    @PostMapping
    @Secured(RoleConst.NURSE)
    public Result save(@RequestBody Appointment appointment) {
        Result checkAppoint = checkAppoint(appointment);
        if (checkAppoint.code() == 0) return checkAppoint;
        // 自动记录就诊时的宠物主人
        Pet pet = petService.getById(appointment.getPetId());
        if (pet != null) appointment.setPetOwnerId(pet.getClientId());
        else appointment.setPetOwnerId(appointment.getClientId());
        return Result.choice("添加", appointmentService.save(appointment));
    }

    /**
     * 顾客自己预约挂号：挂号人强制为登录人，且只能挂自己名下的宠物
     */
    @PostMapping("/self")
    public Result saveSelf(@RequestBody Appointment appointment) {
        if (!(UserUtil.getUser() instanceof Client client)) return Result.error("请先登录顾客账号");
        // 只能预约自己名下的宠物
        Pet pet = petService.getById(appointment.getPetId());
        if (pet == null) return Result.error("请选择宠物");
        if (!client.getClientId().equals(pet.getClientId())) return Result.error("只能预约自己名下的宠物");
        if (!Boolean.TRUE.equals(pet.getActive()))
            return Result.error("该宠物信息待审核（或已停用），暂不能挂号，请联系医院");
        appointment.setClientId(client.getClientId());
        appointment.setPetOwnerId(client.getClientId());
        Result checkAppoint = checkAppoint(appointment);
        if (checkAppoint.code() == 0) return checkAppoint;
        appointment.setIsSuc(false);
        return Result.choice("预约", appointmentService.save(appointment));
    }

    /**
     * 顾客改期自己的待就诊预约（仅可改就诊时间，其余以原单为准，防越权）
     */
    @PutMapping("/self/{id}")
    public Result updateSelf(@PathVariable Integer id, @RequestBody Appointment appointment) {
        if (!(UserUtil.getUser() instanceof Client client)) return Result.error("请先登录顾客账号");
        Appointment db = appointmentService.getById(id);
        if (db == null) return Result.error("预约不存在");
        if (!client.getClientId().equals(db.getClientId())) return Result.error("只能操作自己的预约");
        if (Boolean.TRUE.equals(db.getIsSuc())) return Result.error("该预约已完成，不能改期");
        // 锁定不可改字段，仅采纳新的就诊时间
        appointment.setAppointmentId(id);
        appointment.setClientId(db.getClientId());
        appointment.setPetId(db.getPetId());
        appointment.setPetOwnerId(db.getPetOwnerId());
        appointment.setDepartmentId(db.getDepartmentId());
        appointment.setDoctorId(db.getDoctorId());
        appointment.setAppointmentInfo(db.getAppointmentInfo());
        appointment.setIsSuc(db.getIsSuc());
        Result check = checkAppoint(appointment);
        if (check.code() == 0) return check;
        return Result.choice("改期", appointmentService.updateById(appointment));
    }

    /**
     * 顾客取消自己的待就诊预约
     */
    @PutMapping("/self/cancel/{id}")
    public Result cancelSelf(@PathVariable Integer id) {
        if (!(UserUtil.getUser() instanceof Client client)) return Result.error("请先登录顾客账号");
        Appointment db = appointmentService.getById(id);
        if (db == null) return Result.error("预约不存在");
        if (!client.getClientId().equals(db.getClientId())) return Result.error("只能操作自己的预约");
        if (Boolean.TRUE.equals(db.getIsSuc())) return Result.error("该预约已完成，无法取消");
        return Result.choice("取消", appointmentService.lambdaUpdate()
                .eq(Appointment::getAppointmentId, id)
                .set(Appointment::getIsDel, System.currentTimeMillis())
                .update());
    }

    @DeleteMapping("/{id}")
    @Secured(RoleConst.DOCTOR)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除单个", appointmentService.deleteById(id));
    }

    @DeleteMapping("/batch/{ids}")
    @Secured(RoleConst.DOCTOR)
    public Result deleteGroup(@PathVariable List<Integer> ids) {
        return Result.choice("删除多个", appointmentService.deleteByIds(ids));
    }

    @PutMapping
    @Secured(RoleConst.DOCTOR)
    public Result update(@RequestBody Appointment appointment) {
        Result checkAppoint = checkAppoint(appointment);
        if (checkAppoint.code() == 0) return checkAppoint;
        else return Result.choice("修改", appointmentService.updateById(appointment));
    }

    /**
     * 新增或修改时，校验信息
     *
     * @param appoint 新增或修改的挂号传参
     * @return 是否校验通过
     */
    private Result checkAppoint(Appointment appoint) {
        if (appoint.getAppointmentDate() == null)
            return Result.error("就诊时间不能为空");
        if (StrUtil.isWhite(appoint.getAppointmentInfo()))
            return Result.error("请输入挂号描述");
        if (appoint.getClientId() == null || appoint.getClientId() == 0)
            return Result.error("请选择用户");
        if (appoint.getPetId() == null || appoint.getPetId() == 0)
            return Result.error("请选择宠物");
        if (appoint.getDepartmentId() == null || appoint.getDepartmentId() == 0)
            return Result.error("请选择科室");
        if (appoint.getDoctorId() == null || appoint.getDoctorId() == 0)
            return Result.error("请选择医生");
        // 号源容量：同一医生同一自然日预约数不得超过上限（改期时排除自身）
        LocalDateTime dayStart = appoint.getAppointmentDate().toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = appoint.getAppointmentDate().toLocalDate().plusDays(1).atStartOfDay();
        long sameDay = appointmentService.lambdaQuery()
                .eq(Appointment::getDoctorId, appoint.getDoctorId())
                .ge(Appointment::getAppointmentDate, dayStart)
                .lt(Appointment::getAppointmentDate, dayEnd)
                .ne(appoint.getAppointmentId() != null, Appointment::getAppointmentId, appoint.getAppointmentId())
                .and(w -> w.eq(Appointment::getIsDel, 0).or().isNull(Appointment::getIsDel))
                .count();
        if (sameDay >= maxPerDay)
            return Result.error("该医生当日号源已满（每日最多 " + maxPerDay + " 个），请改选其他日期或医生");
        return Result.success("数据合法");
    }
}
