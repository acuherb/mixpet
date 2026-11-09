package com.mix.security.details;

import com.mix.model.entity.Doctor;
import com.mix.security.model.RoleConst;
import com.mix.security.model.MyUser;
import com.mix.security.model.RoleEnum;
import com.mix.service.IDoctorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 医生（员工）登录时
 */
@Component
public class DoctorDetails implements UserDetailsService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final IDoctorService doctorService;

    @Autowired
    public DoctorDetails(IDoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * 权限档 → 角色：显式列出，不再用 switch fall-through 把低档角色无脑累加给高档。
     * <p>
     * NURSE 在后端大量接口里是「在职员工基线」（收款/发药/客户/药品等），所以所有 >=2 的档位都要带，
     * 否则管理员连后台管理页都打不开。
     * <p>
     * DOCTOR 代表「临床开方 / 写病历 / 开检查单资格」，只有 4 档医生具备。
     * 管理员(6)与超管(8)是纯管理账号，不继承临床资格——用户 2026-11-09 定下：
     * 即使院长本人也是执业兽医，也请另建一个医生账号处理临床业务，管理账号只做管理。
     * 这样处方笺上的开方人、执业注册号与账号职责才能对得上。
     * <p>
     * 边界：注解里同时含 MANAGER/ADMIN/NURSE 的「查询 / 审核」类接口，管理岗仍可访问（管理需要），
     * 真正被挡住的是纯 @Secured(DOCTOR) 的临床写操作。
     */
    public static List<String> rolesOf(int level) {
        List<String> roles = new ArrayList<>();
        switch (level) {
            case 8 -> {
                roles.add(RoleConst.ADMIN);
                roles.add(RoleConst.MANAGER);
                roles.add(RoleConst.NURSE);
            }
            case 6 -> {
                roles.add(RoleConst.MANAGER);
                roles.add(RoleConst.NURSE);
            }
            case 4 -> {
                roles.add(RoleConst.DOCTOR);
                roles.add(RoleConst.NURSE);
            }
            case 2 -> roles.add(RoleConst.NURSE);
            default -> {
            }
        }
        roles.add(RoleConst.LOSER);
        return roles;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("从数据库根据医生名查询数据");
        Doctor doctor = doctorService.lambdaQuery().eq(Doctor::getUsername, username).one();
        if (doctor == null) {
            log.warn("没有这个用户");
            throw new UsernameNotFoundException("用户名不存在");
        }
        String password = doctor.getDoctorPassword();
        var authorities = AuthorityUtils.createAuthorityList(rolesOf(doctor.getAuthLv()));
        return new MyUser(username, password, authorities, RoleEnum.DOCTOR, doctor);
    }
}
