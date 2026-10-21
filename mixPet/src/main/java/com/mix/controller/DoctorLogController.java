package com.mix.controller;

import com.mix.common.Result;
import com.mix.common.SendMail;
import com.mix.model.dto.AppointmentDto2;
import com.mix.model.dto.DoctorDto;
import com.mix.model.entity.Appointment;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Pet;
import com.mix.model.vo.DealAppointVo;
import com.mix.model.vo.DoctorResetPwdVo;
import com.mix.security.model.RoleConst;
import com.mix.service.IAppointmentService;
import com.mix.service.IDoctorService;
import com.mix.service.IPetService;
import com.mix.util.StrUtil;
import com.mix.util.UserUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 医生：已经登录
 */
@RestController
@RequestMapping("/doctorLog")
@Secured(RoleConst.LOSER)
public class DoctorLogController {
    private final IDoctorService doctorService;
    private final IAppointmentService appointService;
    private final SendMail sendMail;
    private final IPetService petService;

    @Autowired
    public DoctorLogController(IDoctorService doctorService, IAppointmentService appointService, SendMail sendMail,
                               IPetService petService) {
        this.doctorService = doctorService;
        this.appointService = appointService;
        this.sendMail = sendMail;
        this.petService = petService;
    }

    /**
     * 医生：查询当前登录的医生信息
     */
    @GetMapping
    public DoctorDto getOneLogin() {
        if (UserUtil.getUser() instanceof Doctor doctor) {
            return doctorService.selectById(doctor.getDoctorId());
        } else return null;
    }

    /**
     * 医生：重置密码发送邮件
     */
    @GetMapping("/resetPwd")
    public Result sendMail(HttpSession session) {
        if (UserUtil.getUser() instanceof Doctor doctor) {
            String mail = doctor.getDoctorEmail();
            if (session.getAttribute(mail) != null) {
                if (System.currentTimeMillis() - session.getCreationTime() < 30 * 1000)
                    return Result.error("发送邮件需要间隔30s");
            }
            String code = sendMail.sendQQEmail(mail);
            if (code != null) session.setAttribute(mail, code);
            return Result.success("验证码已发送至邮箱");
        } else return Result.error("发送失败");
    }

    /**
     * 医生：重置密码
     */
    @PutMapping("/resetPwd")
    public Result resetPwd(@RequestBody DoctorResetPwdVo pwdVo, HttpSession session) {
        String password = pwdVo.password();
        String checkCode = pwdVo.checkCode();
        if (UserUtil.getUser() instanceof Doctor doctor) {
            String mail = doctor.getDoctorEmail();
            String sessionCode = (String) session.getAttribute(mail);
            if (StrUtil.isWhite(checkCode)) return Result.error("验证码不能为空");
            if (StrUtil.isWhite(password)) return Result.error("密码不能为空");
            else if (sessionCode == null) return Result.error("请点击发送验证码");
            else if (sessionCode.equals(checkCode.toUpperCase())) {
                session.invalidate();//销毁验证码
                password = StrUtil.tranPwd(password);
                doctor.setDoctorPassword(password);// 重新设置密码
                return Result.choice("密码重置", doctorService.updateById(doctor));
            } else return Result.error("验证码不正确");
        } else return Result.error("当前未登录");
    }

    /**
     * 医生：修改简历
     */
    @PutMapping
    public Result update(@RequestBody Doctor doctor) {
        if (StrUtil.isWhite(doctor.getDoctorName())) return Result.error("姓名不能为空");
        if (!StrUtil.isWhite(doctor.getDoctorTel()) && !doctor.getDoctorTel().matches("^1[3-9]\\d{9}$"))
            return Result.error("手机号格式不正确（11位，1 开头）");
        if (!StrUtil.isWhite(doctor.getDoctorEmail()) && !doctor.getDoctorEmail().matches("^[A-Za-z0-9][A-Za-z0-9._]*@[A-Za-z0-9]+\\.[A-Za-z0-9.-]+[A-Za-z]$"))
            return Result.error("邮箱格式不正确");
        if (UserUtil.getUser() instanceof Doctor doctorLog) {
            doctor.setDoctorId(doctorLog.getDoctorId());
            return Result.choice("修改", doctorService.updateSelf(doctor));
        } else return Result.error("未登录");
    }

    /**
     * 医生：查询自己的订单
     */
    @GetMapping("/appoint")
    public List<AppointmentDto2> getAppoint() {
        if (UserUtil.getUser() instanceof Doctor doctor) {
            return appointService.getDoctorLog(doctor.getDoctorId());
        } else return null;
    }

    /**
     * 医生：添加挂号
     */
    @PostMapping("/appoint")
    public Result addAppoint(@RequestBody Appointment appointment) {
        if (UserUtil.getUser() instanceof Doctor doctorLog) {
            // 给医生端新增补全必填校验，避免直接插入触发数据完整性违约
            if (appointment.getPetId() == null || appointment.getPetId() == 0)
                return Result.error("请选择宠物");
            if (appointment.getClientId() == null || appointment.getClientId() == 0)
                return Result.error("请选择用户");
            if (appointment.getAppointmentDate() == null)
                return Result.error("请选择就诊时间");
            String info = appointment.getAppointmentInfo();
            if (info != null && info.getBytes().length > 64)
                return Result.error("挂号描述不能超过64个字符");
            appointment.setDoctorId(doctorLog.getDoctorId());
            if (doctorLog.getDepartmentId() == null)
                return Result.error("您尚未配置所属科室，无法挂号，请联系管理员");
            appointment.setDepartmentId(doctorLog.getDepartmentId());
            // 自动记录就诊时的宠物主人，避免主键关联字段为空触发数据完整性违约
            Pet pet = petService.getById(appointment.getPetId());
            appointment.setPetOwnerId(pet != null ? pet.getClientId() : appointment.getClientId());
            if (appointment.getIsSuc() == null) appointment.setIsSuc(false);
            return Result.choice("添加", appointService.save(appointment));
        } else return Result.error("未获取登录信息");
    }

    /**
     * 医生：处理挂号
     */
    @PutMapping("/appoint/deal")
    @Secured(RoleConst.DOCTOR)
    public Result dealAppoint(@RequestBody DealAppointVo dealAppointVo) {
        // 医生只能处理自己的挂号单
        if (!(UserUtil.getUser() instanceof Doctor doctor)) return Result.error("请登录医生账号");
        Appointment appoint = appointService.getById(dealAppointVo.appointId());
        if (appoint == null) return Result.error("挂号单不存在");
        if (!doctor.getDoctorId().equals(appoint.getDoctorId()))
            return Result.error("只能处理自己的挂号单");
        return Result.choice("处理", appointService.lambdaUpdate()
                .eq(Appointment::getAppointmentId, dealAppointVo.appointId())
                .set(Appointment::getIsSuc, dealAppointVo.isSuc())
                .update());
    }
}