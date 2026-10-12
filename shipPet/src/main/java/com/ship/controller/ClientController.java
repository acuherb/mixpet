package com.ship.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ship.common.Result;
import com.ship.model.entity.Client;
import com.ship.model.dto.NameDo;
import com.ship.model.vo.EmailVo;
import com.ship.model.vo.PasswordVo;
import com.ship.common.SendMail;
import jakarta.servlet.http.HttpSession;
import com.ship.security.model.RoleConst;
import com.ship.service.IClientService;
import com.ship.util.StrUtil;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    public ClientController(IClientService clientService) {
        this.clientService = clientService;
    }

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
    @Secured(RoleConst.MANAGER)
    public Client getOne(Integer id) {
        return clientService.getById(id);
    }

    @GetMapping("/page")
    @Secured(RoleConst.MANAGER)
    public IPage<Client> getPage(int numPage, int pageSize, String clientName) {
        return clientService.selectByPage(clientName, numPage, pageSize);
    }

    @PostMapping
    @Secured(RoleConst.MANAGER)
    public Result save(@RequestBody Client client) {
        if (StrUtil.isWhite(client.getClientName())) return Result.error("用户姓名为空");
        if (StrUtil.isWhite(client.getClientTel())) return Result.error("手机号为空");
        if (!client.getClientTel().matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
        if (clientService.existsByTel(client.getClientTel(), null))
            return Result.error("手机号已被使用");
        client.setClientUsername(client.getClientTel());
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

    @PutMapping
    @Secured(RoleConst.MANAGER)
    public Result update(@RequestBody Client client) {
        if (StrUtil.isWhite(client.getClientName())) return Result.error("用户姓名为空");
        if (StrUtil.isWhite(client.getClientTel())) return Result.error("手机号为空");
        if (!client.getClientTel().matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
        if (clientService.existsByTel(client.getClientTel(), client.getClientId()))
            return Result.error("手机号已被其他用户使用");
        client.setClientUsername(client.getClientTel());
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
            return Result.choice("修改", clientService.updateSelf(client));
        } else return Result.error("未得到登录人信息");
    }

    /**
     * 用户修改自己的登录密码
     */
    @PutMapping("/password")
    @Secured(RoleConst.CLIENT)
    public Result updatePassword(@RequestBody PasswordVo vo) {
        if (StrUtil.isWhite(vo.oldPwd()) || StrUtil.isWhite(vo.newPwd()))
            return Result.error("原密码或新密码不能为空");
        if (vo.newPwd().length() < 6)
            return Result.error("新密码至少6位");
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
        dbClient.setClientPassword(StrUtil.tranPwd("123456"));
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
}
