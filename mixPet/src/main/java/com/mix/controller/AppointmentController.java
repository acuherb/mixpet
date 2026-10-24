package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.annotation.AuditLog;
import com.mix.common.Result;
import com.mix.model.dto.AppointmentDto;
import com.mix.model.entity.Appointment;
import com.mix.model.entity.Client;
import com.mix.security.model.RoleConst;
import com.mix.service.IAppointmentService;
import com.mix.service.IPetService;
import com.mix.service.IScheduleService;
import com.mix.model.entity.Pet;
import com.mix.util.StrUtil;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private final IScheduleService scheduleService;

    /** 每位医生每日号源上限 */
    @Value("${appointment.max-per-day:20}")
    private int maxPerDay;

    @Autowired
    public AppointmentController(IAppointmentService appointmentService, IPetService petService,
                                 IScheduleService scheduleService) {
        this.appointmentService = appointmentService;
        this.petService = petService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER})
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

    /**
     * 顾客查某医生当日已约数（挂号时段余号展示，date=YYYY-MM-DD）
     */
    @GetMapping("/self/dayCount")
    public long dayCount(Integer doctorId, String date) {
        if (doctorId == null || StrUtil.isWhite(date)) return 0;
        LocalDate day = LocalDate.parse(date);
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
        return appointmentService.lambdaQuery()
                .eq(Appointment::getDoctorId, doctorId)
                .ge(Appointment::getAppointmentDate, dayStart)
                .lt(Appointment::getAppointmentDate, dayEnd)
                .and(w -> w.eq(Appointment::getIsDel, 0).or().isNull(Appointment::getIsDel))
                .count();
    }

    // 事务保证：号源校验(count)与 insert 在同一事务内，缩小并发竞态窗口（彻底方案需号源行锁表，后续迭代）
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
     */    @PutMapping("/self/{id}")
    public Result updateSelf(@PathVariable Integer id, @RequestBody Appointment appointment) {
        if (!(UserUtil.getUser() instanceof Client client)) return Result.error("请先登录顾客账号");
        Appointment db = appointmentService.getById(id);
        if (db == null) return Result.error("预约不存在");
        if (!client.getClientId().equals(db.getClientId())) return Result.error("只能操作自己的预约");
        if (Boolean.TRUE.equals(db.getIsSuc())) return Result.error("该预约已完成，不能改期");
        if (Boolean.TRUE.equals(db.getMiss())) return Result.error("该预约已爽约，不能改期，请重新预约");
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
        if (Boolean.TRUE.equals(db.getMiss())) return Result.error("该预约已爽约，无需取消，请重新预约");
        return Result.choice("取消", appointmentService.lambdaUpdate()
                .eq(Appointment::getAppointmentId, id)
                .set(Appointment::getIsDel, System.currentTimeMillis())
                .update());
    }

    /**
     * 医生标记爽约（预约时间已过但顾客未到店）：记录保留（miss 状态位，不逻辑删除），
     * 顾客端可见「已爽约」记录且不能再改期/取消；仅允许操作「未处理」且就诊时间已过的挂号单
     */
    @PutMapping("/miss/{id}")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER})
    @AuditLog(module = "appointment", action = "miss", target = "标记爽约")
    public Result markMiss(@PathVariable Integer id) {
        Appointment db = appointmentService.getById(id);
        if (db == null) return Result.error("预约不存在");
        if (Boolean.TRUE.equals(db.getIsSuc())) return Result.error("该预约已接诊，不能标记爽约");
        if (Boolean.TRUE.equals(db.getMiss())) return Result.error("该预约已标记爽约");
        if (db.getAppointmentDate() == null || db.getAppointmentDate().isAfter(LocalDateTime.now()))
            return Result.error("就诊时间未到，不能标记爽约");
        return Result.choice("标记爽约", appointmentService.lambdaUpdate()
                .eq(Appointment::getAppointmentId, id)
                .set(Appointment::getMiss, true)
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
        // 排班校验：该医生当日不出诊则不能挂号（未配置排班的医生不限制，向后兼容）
        if (!scheduleService.isOnDuty(appoint.getDoctorId(), appoint.getAppointmentDate().toLocalDate()))
            return Result.error("该医生当日未排班（不出诊），请改选其他日期或医生");
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
