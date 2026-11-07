package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.entity.Client;
import com.mix.model.entity.ClientNotification;
import com.mix.service.IClientNotificationService;
import com.mix.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 顾客站内通知：报告出具、结果修订等定向触达。
 * 所有查询/写操作都强制按登录顾客的 clientId 过滤，员工访问返回空。
 */
@RestController
@RequestMapping("/notify")
public class ClientNotificationController {
    private final IClientNotificationService notificationService;

    @Autowired
    public ClientNotificationController(IClientNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/self")
    public List<ClientNotification> self() {
        return notificationService.selfList(selfClientId());
    }

    @GetMapping("/unread")
    public long unread() {
        return notificationService.unreadCount(selfClientId());
    }

    @PutMapping("/read/{id}")
    public Result read(@PathVariable Long id) {
        return notificationService.read(selfClientId(), id);
    }

    @PutMapping("/readAll")
    public Result readAll() {
        return notificationService.readAll(selfClientId());
    }

    private Integer selfClientId() {
        return UserUtil.getUser() instanceof Client client ? client.getClientId() : null;
    }
}
