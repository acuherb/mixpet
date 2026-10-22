package com.mix.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mix.common.Result;
import com.mix.model.entity.Client;
import com.mix.model.dto.NameDo;
import com.mix.model.vo.EmailVo;
import com.mix.model.vo.PasswordVo;
import com.mix.common.SendMail;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import com.mix.security.model.RoleConst;
import com.mix.service.AvatarFileService;
import com.mix.service.IClientService;
import com.mix.service.IMemberService;
import com.mix.util.StrUtil;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author MixJade
 * @since 2022-12-22
 */
@RestController
@RequestMapping("/client")
public class ClientController {
    private final IClientService clientService;
    private final IMemberService memberService;
    private final AvatarFileService avatarFileService;

    @Autowired
    public ClientController(IClientService clientService, IMemberService memberService,
                            AvatarFileService avatarFileService) {
        this.clientService = clientService;
        this.memberService = memberService;
        this.avatarFileService = avatarFileService;
    }

    /** 管理员重置密码默认口令 */
    @Value("${app.default-password:123456}")
    private String defaultPwd;

    /**
     * 查询用户的名字与id
     */
    @GetMapping
    @Secured(RoleConst.NURSE)
    public List<NameDo> getName() {
        return clientService.selectName();
    }

    /**
     * 【子查询】名下有宠物的用户，带上当前用户
     *
     * @param clientId 当前客户ID
     * @return 客户名称+ID
     */
    @GetMapping("/full")
    @Secured(RoleConst.NURSE)
    public List<NameDo> getNameHavePet(Integer clientId) {
        return clientService.selectNameHavePet(clientId);
    }

    /**
     * 根据id查询单个用户
     */
    @GetMapping("/one")
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER})
    public Client getOne(Integer id) {
        return clientService.getById(id);
    }

    @GetMapping("/page")
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER})
    public IPage<Client> getPage(int numPage, int pageSize, String clientName) {
        return clientService.selectByPage(clientName, numPage, pageSize);
    }

    @PostMapping
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER})
    public Result save(@RequestBody Client client) {
        if (StrUtil.isWhite(client.getClientName())) return Result.error("用户姓名为空");
        if (StrUtil.isWhite(client.getClientTel())) return Result.error("手机号为空");
        if (!client.getClientTel().matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
        if (clientService.existsByTel(client.getClientTel(), null))
            return Result.error("手机号已被使用");
        client.setClientUsername(client.getClientTel());
        client.setAuditStatus(1); // 后台手动新增默认审核通过
        return Result.choice("添加", clientService.addClient(client));
    }

    @DeleteMapping("/{id}")
    @Secured(RoleConst.MANAGER)
    public Result delete(@PathVariable Integer id) {
        return Result.choice("删除单个", clientService.deleteById(id));
    }

    @DeleteMapping("/batch/{ids}")
    @Secured(RoleConst.MANAGER)
    public Result deleteGroup(@PathVariable List<Integer> ids) {
        return Result.choice("删除多个", clientService.deleteByIds(ids));
    }

    /**
     * 前台为客户充值（现金收款存入余额，供收费时余额扣款）
     */
    @PutMapping("/recharge")
    @Secured(RoleConst.NURSE)
    public Result recharge(Integer clientId, BigDecimal money) {
        if (clientId == null) return Result.error("缺少客户id");
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) return Result.error("充值金额必须大于0");
        if (money.compareTo(new BigDecimal("100000")) > 0) return Result.error("单次充值不可超过10万元");
        Client client = clientService.getById(clientId);
        if (client == null) return Result.error("客户不存在");
        // 会员等级充值返赠
        BigDecimal bonus = memberService.rechargeBonus(client.getClientLevel(), money);
        BigDecimal total = money.add(bonus);
        boolean ok = clientService.lambdaUpdate()
                .eq(Client::getClientId, clientId)
                .setSql("client_balance = client_balance + " + total.toPlainString())
                .update();
        if (ok && bonus.compareTo(BigDecimal.ZERO) > 0)
            return Result.success("充值成功，返赠 ￥" + bonus.toPlainString() + "，到账 ￥" + total.toPlainString());
        return Result.choice("充值", ok);
    }

    @PutMapping
    @Secured({RoleConst.NURSE, RoleConst.DOCTOR, RoleConst.MANAGER})
    public Result update(@RequestBody Client client) {
        if (StrUtil.isWhite(client.getClientName())) return Result.error("用户姓名为空");
        if (StrUtil.isWhite(client.getClientTel())) return Result.error("手机号为空");
        if (!client.getClientTel().matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
        if (clientService.existsByTel(client.getClientTel(), client.getClientId()))
            return Result.error("手机号已被其他用户使用");
        client.setClientUsername(client.getClientTel());
        // 密码一律不走本接口（防前端塞明文密码被直接入库）；改密码走 /password 专门接口
        client.setClientPassword(null);
        // 更换头像时删除旧头像文件
        Client oldClient = clientService.getById(client.getClientId());
        if (oldClient != null)
            avatarFileService.removeOldAvatar(oldClient.getClientPhoto(), client.getClientPhoto());
        return Result.choice("修改", clientService.updateById(client));
    }

    /**
     * 用户修改自己的资料
     *
     * @param client 用户信息
     * @return 修改成功
     */
    @PutMapping("/self")
    @Secured(RoleConst.CLIENT)
    public Result updateSelf(@RequestBody Client client) {
        if (StrUtil.isWhite(client.getClientName())) return Result.error("用户姓名为空");
        if (StrUtil.isWhite(client.getClientTel())) return Result.error("手机号为空");
        if (!client.getClientTel().matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
        if (!StrUtil.isWhite(client.getClientIdCard())
                && !client.getClientIdCard().matches("^\\d{17}[\\dXx]$"))
            return Result.error("身份证格式不正确");
        if (UserUtil.getUser() instanceof Client client1) {
            if (clientService.existsByTel(client.getClientTel(), client1.getClientId()))
                return Result.error("手机号已被其他用户使用");
            client.setClientId(client1.getClientId());
            client.setClientUsername(client.getClientTel());
            // 密码一律不走本接口（防前端塞明文密码被直接入库）；改密码走 /password 专门接口
            client.setClientPassword(null);
            // 更换头像时删除旧头像文件
            Client oldClient = clientService.getById(client1.getClientId());
            if (oldClient != null)
                avatarFileService.removeOldAvatar(oldClient.getClientPhoto(), client.getClientPhoto());
            return Result.choice("修改", clientService.updateSelf(client));
        } else return Result.error("未得到登录人信息");
    }

    /**
     * 用户修改自己的登录密码
     */
    @PutMapping("/password")
    @Secured(RoleConst.CLIENT)
    public Result updatePassword(@Valid @RequestBody PasswordVo vo) {
        if (UserUtil.getUser() instanceof Client current) {
            Client dbClient = clientService.getById(current.getClientId());
            if (dbClient == null) return Result.error("用户不存在");
            if (!StrUtil.matches(vo.oldPwd(), dbClient.getClientPassword()))
                return Result.error("原密码不正确");
            dbClient.setClientPassword(StrUtil.tranPwd(vo.newPwd()));
            dbClient.setClientPwdChanged(true);
            return Result.choice("修改密码", clientService.updateById(dbClient));
        }
        return Result.error("未登录");
    }

    /**
     * 管理员重置某用户密码为 123456
     */
    @PutMapping("/reset/{id}")
    @Secured(RoleConst.MANAGER)
    public Result resetPassword(@PathVariable Integer id) {
        Client dbClient = clientService.getById(id);
        if (dbClient == null) return Result.error("用户不存在");
        dbClient.setClientPassword(StrUtil.tranPwd(defaultPwd));
        dbClient.setClientPwdChanged(false);
        return Result.choice("重置密码", clientService.updateById(dbClient));
    }

    /**
     * 用户绑定/修改自己的邮箱（需邮箱验证码）
     */
    @PutMapping("/email")
    @Secured(RoleConst.CLIENT)
    public Result updateEmail(@RequestBody EmailVo vo, HttpSession session) {
        if (StrUtil.isWhite(vo.email())) return Result.error("邮箱不能为空");
        if (!SendMail.isValidEmail(vo.email())) return Result.error("邮箱格式不正确");
        if (StrUtil.isWhite(vo.code())) return Result.error("验证码不能为空");
        String sessionCode = (String) session.getAttribute(vo.email());
        if (sessionCode == null) return Result.error("未发送验证码");
        if (!sessionCode.equalsIgnoreCase(vo.code())) return Result.error("验证码不正确");
        session.removeAttribute(vo.email());
        if (UserUtil.getUser() instanceof Client current) {
            Client dbClient = clientService.getById(current.getClientId());
            if (dbClient == null) return Result.error("用户不存在");
            dbClient.setClientEmail(vo.email());
            return Result.choice("绑定邮箱", clientService.updateById(dbClient));
        }
        return Result.error("未登录");
    }

    /**
     * 检查客户名下关联记录
     */
    @GetMapping("/ref/{id}")
    @Secured(RoleConst.MANAGER)
    public Result checkRef(@PathVariable Integer id) {
        java.util.Map<String, Long> m = clientService.countRefById(id);
        long pet = m.getOrDefault("petCount", 0L);
        long ap = m.getOrDefault("appointCount", 0L);
        if (pet == 0 && ap == 0) return Result.success("可彻底删除");
        return Result.error("该客户名下有 " + pet + " 只宠物、" + ap + " 条预约，不能彻底删除，请改为停用");
    }

    /**
     * 停用客户
     */
    @PutMapping("/deactivate/{id}")
    @Secured(RoleConst.MANAGER)
    public Result deactivate(@PathVariable Integer id) {
        Client db = clientService.getById(id);
        if (db == null) return Result.error("客户不存在");
        db.setActive(false);
        return Result.choice("停用", clientService.updateById(db));
    }

    /**
     * 启用客户
     */
    @PutMapping("/activate/{id}")
    @Secured(RoleConst.MANAGER)
    public Result activate(@PathVariable Integer id) {
        Client db = clientService.getById(id);
        if (db == null) return Result.error("客户不存在");
        db.setActive(true);
        return Result.choice("启用", clientService.updateById(db));
    }

    /**
     * 审核自助注册顾客：1通过 2拒绝
     */
    @PutMapping("/audit/{id}")
    @Secured(RoleConst.MANAGER)
    public Result audit(@PathVariable Integer id, Integer status, String reason) {
        if (status == null || (status != 1 && status != 2)) return Result.error("审核状态不正确");
        Client db = clientService.getById(id);
        if (db == null) return Result.error("客户不存在");
        // 拒绝必须填原因（顾客登录时可见）；通过则清空历史原因
        if (status == 2) {
            if (reason == null || reason.isBlank()) return Result.error("请填写拒绝原因（顾客登录时可见）");
            db.setAuditReason(reason.trim());
        } else {
            db.setAuditReason(null);
        }
        db.setAuditStatus(status);
        // 审核通过时同时启用账号，确保可登录
        if (status == 1) db.setActive(true);
        return Result.choice("审核", clientService.updateById(db));
    }
}