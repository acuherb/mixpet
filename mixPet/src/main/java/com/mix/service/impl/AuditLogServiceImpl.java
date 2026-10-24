package com.mix.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.mapper.AuditLogMapper;
import com.mix.model.entity.AuditLog;
import com.mix.model.entity.Doctor;
import com.mix.service.IAuditLogService;
import com.mix.util.UserUtil;
import org.springframework.stereotype.Service;

/**
 * 操作审计日志 Service：写入时操作人/角色一律从登录态取（防参数伪造）
 */
@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements IAuditLogService {

    @Override
    public void log(String module, String action, String target, String detail, String ip) {
        AuditLog log = new AuditLog();
        Object user = UserUtil.getUser();
        if (user instanceof Doctor doctor) {
            log.setOperator(doctor.getDoctorName());
            log.setRole(switch (doctor.getAuthLv() == null ? 0 : doctor.getAuthLv()) {
                case 8 -> "ADMIN";
                case 6 -> "MANAGER";
                case 4 -> "DOCTOR";
                case 2 -> "NURSE";
                default -> "LOSER";
            });
        } else if (user != null) {
            log.setOperator(String.valueOf(user));
        } else {
            log.setOperator("系统/匿名");
        }
        log.setModule(module);
        log.setAction(action);
        log.setTarget(target == null ? null : target.substring(0, Math.min(128, target.length())));
        log.setDetail(detail == null ? null : detail.substring(0, Math.min(512, detail.length())));
        log.setIp(ip);
        this.save(log);
    }
}
