package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.annotation.AuditLog;
import com.mix.annotation.FlowChanged;
import com.mix.common.Result;
import com.mix.model.dto.AppointmentDto;
import com.mix.model.entity.Appointment;
import com.mix.model.entity.Client;
import com.mix.security.model.RoleConst;
import com.mix.service.IAppointmentService;
import com.mix.service.IPetService;
import com.mix.model.entity.Pet;
import com.mix.util.StrUtil;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 挂号单表 前端控制器。
 * 所有新增/改期路径统一走 IAppointmentService.reserve（事务 + 行锁 + 容量校验），
 * Controller 只做身份与字段级校验，不再各自做号源判断。
 */
@RestController
@RequestMapping("/appointment")
public class AppointmentController {
    private final IAppointmentService appointmentService;
    private final IPetService petService;

    @Autowired
    public AppointmentController(IAppointmentService appointmentService, IPetService petService) {
        this.appointmentService = appointmentService;
        this.petService = petService;
    }

    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER})
    public IPage<AppointmentDto> getPage(int numPage, int pageSize, String seaName, int seaType) {
        return appointmentService.selectByPage(seaName, seaType, numPage, pageSize);
    }

    @GetMapping("/doctor")
    @Secured({RoleConst.DOCTOR, RoleConst.NURSE, RoleConst.MANAGER, RoleConst.ADMIN})
    public List<AppointmentDto> getDoctor(Integer doctorId) {
        return appointmentService.getDoctor(doctorId);
    }

    @GetMapping("/client")
    public List<AppointmentDto> getClient() {
        if (UserUtil.getUser() instanceof Client client) {
            return appointmentService.getClient(client.getClientId());
        } else return null;
    }

    /** 顾客查某医生当日已约数（保留兼容） */
    @GetMapping("/self/dayCount")
    public long dayCount(Integer doctorId, String date) {
        return sameDay(doctorId, date).size();
    }

    /** 顾客端余号：按上午/下午分别返回已约数，避免用整日数把两个时段各减一次 */
    @GetMapping("/self/slotCount")
    public Map<String, Long> slotCount(Integer doctorId, String date) {
        List<Appointment> rows = sameDay(doctorId, date);
        long am = rows.stream().filter(a -> a.getAppointmentDate() != null
                && a.getAppointmentDate().getHour() < 12).count();
        Map<String, Long> out = new HashMap<>();
        out.put("1", am);
        out.put("2", rows.size() - am);
        out.put("total", (long) rows.size());
        return out;
    }

    private List<Appointment> sameDay(Integer doctorId, String date) {
        if (doctorId == null || StrUtil.isWhite(date)) return List.of();
        LocalDate day = LocalDate.parse(date);
        return appointmentService.lambdaQuery()
                .eq(Appointment::getDoctorId, doctorId)
                .ge(Appointment::getAppointmentDate, day.atStartOfDay())
                .lt(Appointment::getAppointmentDate, day.plusDays(1).atStartOfDay())
                .and(w -> w.eq(Appointment::getIsDel, 0).or().isNull(Appointment::getIsDel))
                .list();
    }

    @PostMapping
    @Secured(RoleConst.NURSE)
    @FlowChanged(stage = "wait", text = "新增挂号，待接诊")
    public Result save(@RequestBody Appointment appointment) {
        Result check = checkFields(appointment);
        if (check.code() == 0) return check;
        Pet pet = petService.getById(appointment.getPetId());
        appointment.setPetOwnerId(pet != null ? pet.getClientId() : appointment.getClientId());
        boolean ok = appointmentService.reserve(appointment, true);
        if (!ok) return Result.choice("添加", false);
        // 挂号凭证卡需要单号：insert 后实体 id 已回填，随 data 返回（msg 置空避免拦截器重复 toast）
        return Result.data(appointment.getAppointmentId());
    }

    /** 顾客自己预约挂号：挂号人强制为登录人，且只能挂自己名下的宠物 */
    @PostMapping("/self")
    @FlowChanged(stage = "wait", text = "顾客自主预约，待接诊")
    public Result saveSelf(@RequestBody Appointment appointment) {
        if (!(UserUtil.getUser() instanceof Client client)) return Result.error("请先登录顾客账号");
        Pet pet = petService.getById(appointment.getPetId());
        if (pet == null) return Result.error("请选择宠物");
        if (!client.getClientId().equals(pet.getClientId())) return Result.error("只能预约自己名下的宠物");
        if (!Boolean.TRUE.equals(pet.getActive()))
            return Result.error("该宠物信息待审核（或已停用），暂不能挂号，请联系医院");
        appointment.setClientId(client.getClientId());
        appointment.setPetOwnerId(client.getClientId());
        Result check = checkFields(appointment);
        if (check.code() == 0) return check;
        appointment.setIsSuc(false);
        return Result.choice("预约", appointmentService.reserve(appointment, true));
    }

    /** 顾客改期自己的待就诊预约（仅可改就诊时间，其余以原单为准，防越权） */
    @PutMapping("/self/{id}")
    public Result updateSelf(@PathVariable Integer id, @RequestBody Appointment appointment) {
        if (!(UserUtil.getUser() instanceof Client client)) return Result.error("请先登录顾客账号");
        Appointment db = appointmentService.getById(id);
        if (db == null) return Result.error("预约不存在");
        if (!client.getClientId().equals(db.getClientId())) return Result.error("只能操作自己的预约");
        if (Boolean.TRUE.equals(db.getIsSuc())) return Result.error("该预约已完成，不能改期");
        if (Boolean.TRUE.equals(db.getMiss())) return Result.error("该预约已爽约，不能改期，请重新预约");
        appointment.setAppointmentId(id);
        appointment.setClientId(db.getClientId());
        appointment.setPetId(db.getPetId());
        appointment.setPetOwnerId(db.getPetOwnerId());
        appointment.setDepartmentId(db.getDepartmentId());
        appointment.setDoctorId(db.getDoctorId());
        appointment.setAppointmentInfo(db.getAppointmentInfo());
        appointment.setIsSuc(db.getIsSuc());
        Result check = checkFields(appointment);
        if (check.code() == 0) return check;
        return Result.choice("改期", appointmentService.reserve(appointment, false));
    }

    @PutMapping("/self/cancel/{id}")
    public Result cancelSelf(@PathVariable Integer id) {
        if (!(UserUtil.getUser() instanceof Client client)) return Result.error("请先登录顾客账号");
        Appointment db = appointmentService.getById(id);
        if (db == null) return Result.error("预约不存在");
        if (!client.getClientId().equals(db.getClientId())) return Result.error("只能操作自己的预约");
        if (Boolean.TRUE.equals(db.getIsSuc())) return Result.error("该预约已接诊，无法取消");
        if (Boolean.TRUE.equals(db.getMiss())) return Result.error("该预约已爽约，无需取消，请重新预约");
        return Result.choice("取消", appointmentService.lambdaUpdate()
                .eq(Appointment::getAppointmentId, id)
                .set(Appointment::getIsDel, System.currentTimeMillis())
                .update());
    }

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
        Result check = checkFields(appointment);
        if (check.code() == 0) return check;
        return Result.choice("修改", appointmentService.reserve(appointment, false));
    }

    /** 字段级校验；容量/出诊/并发由 Service.reserve 统一负责 */
    private Result checkFields(Appointment appoint) {
        if (appoint.getAppointmentDate() == null) return Result.error("就诊时间不能为空");
        if (StrUtil.isWhite(appoint.getAppointmentInfo())) return Result.error("请输入挂号描述");
        if (appoint.getClientId() == null || appoint.getClientId() == 0) return Result.error("请选择用户");
        if (appoint.getPetId() == null || appoint.getPetId() == 0) return Result.error("请选择宠物");
        if (appoint.getDepartmentId() == null || appoint.getDepartmentId() == 0) return Result.error("请选择科室");
        if (appoint.getDoctorId() == null || appoint.getDoctorId() == 0) return Result.error("请选择医生");
        return Result.success("数据合法");
    }
}
