package com.ship.security.details;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ship.model.entity.Client;
import com.ship.mapper.ClientMapper;
import com.ship.security.model.RoleConst;
import com.ship.security.model.MyUser;
import com.ship.security.model.RoleEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * 用户登录时
 */
@Component
public class ClientDetails implements UserDetailsService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ClientMapper clientMapper;

    @Autowired
    public ClientDetails(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("从数据库根据用户名查询数据");
        // 从数据库获取用户信息（active=0 已停用的账号不允许登录）
        QueryWrapper<Client> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("client_username", username);
        queryWrapper.eq("active", 1);
        Client client = clientMapper.selectOne(queryWrapper);
        if (client == null) {
            log.warn("没有这个用户");
            throw new UsernameNotFoundException("用户名不存在");
        }
        // 审核状态校验：仅"通过(1)"可登录；待审核/拒绝均拦截并给出原因
        Integer audit = client.getAuditStatus();
        if (audit == null || audit != 1) {
            if (audit != null && audit == 2) {
                // 拒绝时带出管理员填写的拒绝原因，顾客知道为什么被拒、找谁
                String reason = client.getAuditReason();
                throw new UsernameNotFoundException(
                        (reason == null || reason.isBlank())
                                ? "账号未通过审核，请联系管理员"
                                : "账号未通过审核：" + reason);
            } else {
                throw new UsernameNotFoundException("账号审核中，请等待管理员通过");
            }
        }
        // 校验所需要的密码
        String password = client.getClientPassword();
        // 授予角色
        var authorities = AuthorityUtils.createAuthorityList(RoleConst.CLIENT);
        return new MyUser(username, password, authorities, RoleEnum.CLIENT, client);
    }
}
