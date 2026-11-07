package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.common.Result;
import com.mix.model.entity.ClientNotification;

import java.util.List;

public interface IClientNotificationService extends IService<ClientNotification> {
    /** 报告出具/修订后定向通知顾客 */
    void notifyReport(Integer clientId, String title, String content,
                      String notifyType, String refType, Integer refId);

    List<ClientNotification> selfList(Integer clientId);

    long unreadCount(Integer clientId);

    Result read(Integer clientId, Long notifyId);

    Result readAll(Integer clientId);
}
