package com.mix.controller;

import com.mix.common.Result;
import com.mix.common.SendMail;
import com.mix.common.VerifyCodeStore;
import com.mix.mapper.ClientMapper;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.vo.RegisterVo;
import com.mix.security.model.RoleEnum;
import com.mix.util.JwtUtil;
import com.mix.util.StrUtil;
import com.mix.util.UserUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/login")
public class LoginController {
    private final ClientMapper clientMapper;
    private final SendMail sendMail;
    private final JwtUtil jwtUtil;
    private final VerifyCodeStore verifyCodeStore;

    @Autowired
    public LoginController(ClientMapper clientMapper, SendMail sendMail, JwtUtil jwtUtil,
                           VerifyCodeStore verifyCodeStore) {
        this.clientMapper = clientMapper;
        this.sendMail = sendMail;
        this.jwtUtil = jwtUtil;
        this.verifyCodeStore = verifyCodeStore;
    }

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

    @GetMapping("/user")
    public Client getUser() {
        if (UserUtil.getUser() instanceof Client client) {
            Client full = clientMapper.selectById(client.getClientId());
            if (full != null) full.setClientPassword(null);
            return full;
        }
        return null;
    }

    @GetMapping("/regMail")
    public Result sendRegMail(String mail, HttpSession session) {
        if (StrUtil.isWhite(mail)) return Result.error("邮箱不能为空");
        if (!verifyCodeStore.trySend(mail)) return Result.error("发送邮件需要间隔30s");
        String code = sendMail.sendQQEmail(mail);
        if (code != null) {
            // 验证码只存服务端；下发的 token 仅包含一次性 id，base64 解码也拿不到验证码
            String token = jwtUtil.generateVerifyCode(mail, null,
                    verifyCodeStore.issue(mail, null, code));
            // 顾客端「绑定邮箱」链路：Session 里只放 token，不再放明文验证码
            session.setAttribute(mail, token);
            // 必须用 data()：success(String) 会把 token 当 msg 塞进 msg 字段
            return Result.data(token);
        }
        return Result.error("邮件发送失败");
    }

    @GetMapping("/isExist")
    public Result isExist(String regUse) {
        if (StrUtil.isWhite(regUse)) return Result.success("空白字符");
        int nowExist = clientMapper.isExist(regUse);
        if (nowExist == 0) return Result.success("用户不存在");
        else return Result.error("该用户已存在");
    }

    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterVo registerVo) {
        if (StrUtil.isWhite(registerVo.tel())) return Result.error("手机号不能为空");
        if (!registerVo.tel().matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
        if (clientMapper.isExist(registerVo.tel()) > 0) return Result.error("该手机号已注册");

        String combined = registerVo.code();
        if (StrUtil.isWhite(combined)) return Result.error("验证码不能为空");

        int sepIdx = combined.indexOf('|');
        if (sepIdx <= 0) return Result.error("验证码格式错误");
        String token = combined.substring(0, sepIdx);
        String userCode = combined.substring(sepIdx + 1);

        String jti = jwtUtil.validateAndGetJti(token, registerVo.mail());
        if (jti == null || !verifyCodeStore.verify(jti, registerVo.mail(), null, userCode))
            return Result.error("验证码不正确或已过期");

        Client client = new Client();
        client.setClientUsername(registerVo.tel());
        client.setClientPassword(registerVo.getReallyPwd());
        client.setClientName(registerVo.name());
        client.setClientGender(registerVo.sex());
        client.setClientEmail(registerVo.mail());
        if (!StrUtil.isWhite(registerVo.birthday())) {
            try {
                client.setClientBirthday(LocalDate.parse(registerVo.birthday()));
            } catch (Exception e) {
                client.setClientBirthday(null);
            }
        }
        client.setClientTel(registerVo.tel());
        client.setClientTel2(registerVo.tel2());
        client.setClientAddress(registerVo.address());
        client.setClientIdCard(registerVo.idCard());
        client.setClientPwdChanged(true);
        client.setActive(true);
        client.setAuditStatus(0);
        if (clientMapper.insert(client) > 0)
            return Result.success("注册成功，请等待管理员审核通过后登录");
        return Result.error("注册失败");
    }

    @GetMapping("/findMail")
    public Result sendFindMail(String mail, String username) {
        if (StrUtil.isWhite(username)) return Result.error("账号不能为空");
        if (StrUtil.isWhite(mail)) return Result.error("邮箱不能为空");
        if (clientMapper.sureMail(username, mail) == 0) return Result.error("账号邮箱不匹配");
        if (!verifyCodeStore.trySend(mail)) return Result.error("发送邮件需要间隔30s");
        String code = sendMail.sendQQEmail(mail);
        if (code != null) {
            String token = jwtUtil.generateVerifyCode(mail, username,
                    verifyCodeStore.issue(mail, username, code));
            return Result.data(token);
        }
        return Result.error("邮件发送失败");
    }

    @PostMapping("/find")
    public Result find(@Valid @RequestBody RegisterVo regVo) {
        String username = regVo.username();
        String combined = regVo.code();
        if (StrUtil.isWhite(username)) return Result.error("账号不能为空");
        if (StrUtil.isWhite(regVo.password()) || regVo.password().length() < 6)
            return Result.error("新密码至少6位");
        if (StrUtil.isWhite(combined)) return Result.error("验证码不能为空");

        int sepIdx = combined.indexOf('|');
        if (sepIdx <= 0) return Result.error("验证码格式错误");
        String token = combined.substring(0, sepIdx);
        String userCode = combined.substring(sepIdx + 1);

        String jti = jwtUtil.validateAndGetJti(token, regVo.mail(), username);
        if (jti == null || !verifyCodeStore.verify(jti, regVo.mail(), username, userCode))
            return Result.error("验证码不正确或已过期");

        boolean isFind = clientMapper.updatePwdByUsername(regVo.getReallyPwd(), regVo.username());
        return Result.choice("密码重置", isFind);
    }
}
