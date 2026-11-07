package com.mix.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.Result;
import com.mix.mapper.ClientNotificationMapper;
import com.mix.model.entity.ClientNotification;
import com.mix.service.IClientNotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientNotificationServiceImpl extends ServiceImpl<ClientNotificationMapper, ClientNotification>
        implements IClientNotificationService {

    @Override
    public void notifyReport(Integer clientId, String title, String content,
                             String notifyType, String refType, Integer refId) {
        if (clientId == null) return;
        ClientNotification n = new ClientNotification();
        n.setClientId(clientId);
        n.setTitle(title);
        n.setContent(content);
        n.setNotifyType(notifyType);
        n.setRefType(refType);
        n.setRefId(refId);
        n.setIsRead(false);
        this.save(n);
    }

    @Override
    public List<ClientNotification> selfList(Integer clientId) {
        if (clientId == null) return List.of();
        return this.lambdaQuery()
                .eq(ClientNotification::getClientId, clientId)
                .orderByDesc(ClientNotification::getCreateTime)
                .last("limit 50")
                .list();
    }

    @Override
    public long unreadCount(Integer clientId) {
        if (clientId == null) return 0;
        return this.lambdaQuery()
                .eq(ClientNotification::getClientId, clientId)
                .eq(ClientNotification::getIsRead, false)
                .count();
    }

    @Override
    public Result read(Integer clientId, Long notifyId) {
        if (clientId == null) return Result.error("请登录顾客账号");
        // 只更新本人通知，防止越权标记他人消息
        this.update(Wrappers.<ClientNotification>lambdaUpdate()
                .eq(ClientNotification::getNotifyId, notifyId)
                .eq(ClientNotification::getClientId, clientId)
                .set(ClientNotification::getIsRead, true));
        return Result.success("已读");
    }

    @Override
    public Result readAll(Integer clientId) {
        if (clientId == null) return Result.error("请登录顾客账号");
        this.update(Wrappers.<ClientNotification>lambdaUpdate()
                .eq(ClientNotification::getClientId, clientId)
                .eq(ClientNotification::getIsRead, false)
                .set(ClientNotification::getIsRead, true));
        return Result.success("已全部标记为已读");
    }
}
