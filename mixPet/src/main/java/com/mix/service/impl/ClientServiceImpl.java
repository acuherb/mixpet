package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.PhotoEnum;
import com.mix.mapper.ClientMapper;
import com.mix.model.entity.Client;
import com.mix.model.dto.NameDo;
import com.mix.service.IClientService;
import com.mix.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author MixJade
 * @since 2022-12-22
 */
@Service
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements IClientService {

    /** 新建用户/重置密码默认口令 */
    @Value("${app.default-password}")
    private String defaultPwd;

    @Override
    public boolean deleteById(Integer clientId) {
        return this.lambdaUpdate()
                .eq(Client::getClientId, clientId)
                .set(Client::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public boolean deleteByIds(List<Integer> idGroup) {
        return this.lambdaUpdate()
                .in(Client::getClientId, idGroup)
                .set(Client::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public IPage<Client> selectByPage(String clientName, Integer auditStatus, int numPage, int pageSize) {
        return baseMapper.selectClientPage(new Page<>(numPage, pageSize), clientName, auditStatus);
    }

    @Override
    public List<NameDo> selectName() {
        return baseMapper.selectName();
    }

    /**
     * 【子查询】名下有宠物的用户，带上当前用户
     *
     * @param clientId 当前客户ID
     * @return 客户名称+ID
     */
    @Override
    public List<NameDo> selectNameHavePet(Integer clientId) {
        return baseMapper.selectNameHavePet(clientId);
    }

    @Override
    public boolean addClient(Client client) {
        // 账号即手机号
        client.setClientUsername(client.getClientTel());
        if (StrUtil.isWhite(client.getClientPhoto())) client.setClientPhoto(PhotoEnum.CLIENT.getPhotoName());
        if (StrUtil.isWhite(client.getClientInfo())) client.setClientInfo("该用户暂无简介");
        if (StrUtil.isWhite(client.getClientPassword())) client.setClientPassword(defaultPwd);
        client.setClientPassword(StrUtil.tranPwd(client.getClientPassword()));
        if (client.getClientPwdChanged() == null) client.setClientPwdChanged(false);
        return save(client);
    }

    @Override
    public boolean existsByTel(String tel, Integer excludeId) {
        return this.lambdaQuery()
                .eq(Client::getClientTel, tel)
                .ne(excludeId != null, Client::getClientId, excludeId)
                .exists();
    }

    @Override
    public boolean updateSelf(Client client) {
        return baseMapper.updateSelfById(client);
    }

    @Override
    public java.util.Map<String, Long> countRefById(Integer id) {
        return baseMapper.countRefById(id);
    }
}
