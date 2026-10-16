package com.ship.controller;

import com.ship.common.Result;
import com.ship.common.SendMail;
import com.ship.mapper.ClientMapper;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.vo.RegisterVo;
import com.ship.security.model.RoleEnum;
import com.ship.util.StrUtil;
import com.ship.util.UserUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 登录与注册模块
 */
@RestController
@RequestMapping("/login")
public class LoginController {
    private final ClientMapper clientMapper;
    private final SendMail sendMail;

    @Autowired
    public LoginController(ClientMapper clientMapper, SendMail sendMail) {
        this.clientMapper = clientMapper;
        this.sendMail = sendMail;
    }

    /**
     * 判断当前的角色进行跳转
     *
     * @return 当前登录的是管理员或用户或没有
     */
    @GetMapping
    public RoleEnum getLogin() {
        Object principal = UserUtil.getUser();
        if (principal instanceof Client) {
            return RoleEnum.CLIENT;
        } else if (principal instanceof Doctor) {
            return RoleEnum.DOCTOR;
        }
        return null;
    }


    /**
     * 登录成功的用户信息，可能在未登录的情况
     *
     * @return id，账号，名字，电话，照片
     */
    @GetMapping("/user")
    public Client getUser() {
        if (UserUtil.getUser() instanceof Client client) {
            Client full = clientMapper.selectById(client.getClientId());
            if (full != null) full.setClientPassword(null); // 密码哈希不外发
            return full;
        }
        return null;
    }

    /**
     * 发送验证码邮件，并将之存入session
     *
     * @param mail    接受者邮箱 sanyu0201@163.com
     * @param session 建立session所需
     */
    @GetMapping("/regMail")
    public Result sendRegMail(String mail, HttpSession session) {
        // 用专属 attribute 记录上次发送时间（session.getCreationTime() 是会话创建时间，不能用）
        String timeKey = mail + "_regmail_time";
        Object last = session.getAttribute(timeKey);
        if (last instanceof Long lastTime
                && System.currentTimeMillis() - lastTime < 30 * 1000)
            return Result.error("发送邮件需要间隔30s");
        String code = sendMail.sendQQEmail(mail);
        if (code != null) {
            session.setAttribute(mail, code);
            session.setAttribute(timeKey, System.currentTimeMillis());
            return Result.success("邮件发送成功");
        } else return Result.error("邮件发送失败");
    }

    /**
     * 注册时检查当前账号是否存在
     *
     * @param regUse 用户名
     * @return 不存在
     */
    @GetMapping("/isExist")
    public Result isExist(String regUse) {
        if (StrUtil.isWhite(regUse)) return Result.success("空白字符");
        int nowExist = clientMapper.isExist(regUse);
        if (nowExist == 0) return Result.success("用户不存在");
        else return Result.error("该用户已存在");
    }

    /**
     * 进行注册
     *
     * @param registerVo 客户信息，附带验证码
     * @param session    获取服务端验证码所需
     * @return 注册成功
     */
    @PostMapping("/register")
    public Result register(@RequestBody RegisterVo registerVo, HttpSession session) {
        // 手机号即账号
        if (StrUtil.isWhite(registerVo.tel())) return Result.error("手机号不能为空");
        if (!registerVo.tel().matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
        if (clientMapper.isExist(registerVo.tel()) > 0) return Result.error("该手机号已注册");
        String checkCode = registerVo.code();
        String sessionCode = (String) session.getAttribute(registerVo.mail());
        if (StrUtil.isWhite(checkCode)) return Result.error("验证码不能为空");
        else if (sessionCode == null) return Result.error("未发送验证码");
        else if (sessionCode.equals(checkCode.toUpperCase())) {
            session.invalidate();//销毁验证码
            // 组装顾客完整信息（参照顾客列表字段）
            Client client = new Client();
            client.setClientUsername(registerVo.tel());
            client.setClientPassword(registerVo.getReallyPwd());
            client.setClientName(registerVo.name());
            client.setClientGender(registerVo.sex());
            client.setClientEmail(registerVo.mail());
            // 生日（选填，格式 YYYY-MM-DD，非法则置空）
            if (!StrUtil.isWhite(registerVo.birthday())) {
                try {
                    client.setClientBirthday(LocalDate.parse(registerVo.birthday()));
                } catch (Exception e) {
                    client.setClientBirthday(null);
                }
            } else client.setClientBirthday(null);
            client.setClientTel(registerVo.tel());
            client.setClientTel2(registerVo.tel2());
            client.setClientAddress(registerVo.address());
            client.setClientIdCard(registerVo.idCard());
            // 注册时密码是用户自己设置的，不算初始密码，无需首次登录强制改密
            client.setClientPwdChanged(true);
            client.setActive(true);
            // 自助注册需管理员审核通过后才能登录
            client.setAuditStatus(0);
            if (clientMapper.insert(client) > 0)
                return Result.success("注册成功，请等待管理员审核通过后登录");
            return Result.error("注册失败");
        } else return Result.error("验证码不正确");
    }

    /**
     * 找回密码：验证账号与用户名，发送验证码
     *
     * @param mail     账号所对应邮箱
     * @param username 账号
     * @param session  建立session所需
     * @return 邮箱不匹配、验证成功
     */
    @GetMapping("/findMail")
    public Result sendFindMail(String mail, String username, HttpSession session) {
        if (StrUtil.isWhite(username)) return Result.error("账号不能为空");
        if (StrUtil.isWhite(mail)) return Result.error("邮箱不能为空");
        if (clientMapper.sureMail(username, mail) == 0) return Result.error("账号邮箱不匹配");
        return sendRegMail(mail, session);
    }

    /**
     * 找回密码
     *
     * @param regVo 客户信息，附带验证码
     * @param session    获取服务端验证码所需
     * @return 注册成功
     */
    @PostMapping("/find")
    public Result find(@RequestBody RegisterVo regVo, HttpSession session) {
        String username = regVo.username();
        String checkCode = regVo.code();
        String mail = regVo.mail();
        if (StrUtil.isWhite(username)) return Result.error("账号不能为空");
        if (StrUtil.isWhite(mail)) return Result.error("未获取到邮箱");
        if (StrUtil.isWhite(regVo.password()) || regVo.password().length() < 6) return Result.error("新密码至少6位");
        String sessionCode = (String) session.getAttribute(mail);
        if (StrUtil.isWhite(checkCode)) return Result.error("验证码不能为空");
        else if (sessionCode == null) return Result.error("未发送验证码");
        else if (sessionCode.equals(checkCode.toUpperCase())) {
            session.invalidate();//销毁验证码
            // 重新设置密码
            boolean isFind = clientMapper.updatePwdByUsername(regVo.getReallyPwd(), regVo.username());
            return Result.choice("密码重置", isFind);
        } else return Result.error("验证码不正确");
    }
}
