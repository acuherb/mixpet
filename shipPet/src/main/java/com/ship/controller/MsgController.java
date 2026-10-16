package com.ship.controller;

import com.ship.common.Result;
import com.ship.model.dto.MsgDo;
import com.ship.model.dto.MsgNameDo;
import com.ship.model.dto.NameDo;
import com.ship.model.entity.Client;
import com.ship.model.entity.Doctor;
import com.ship.model.entity.Msg;
import com.ship.service.IMsgService;
import com.ship.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 聊天记录 前端控制器
 * </p>
 *
 * @author MixJade
 * @since 2023-02-25
 */
@RestController
@RequestMapping("/msg")
public class MsgController {
    private final IMsgService msgService;
    private final String chatPath;

    @Autowired
    public MsgController(IMsgService msgService,
                         @Value("${pet-ship.chatImg-path}") String chatPath) {
        this.msgService = msgService;
        this.chatPath = chatPath;
    }

    /**
     * 查询超过指定天数的历史聊天记录数量（用于提醒手动清理）
     *
     * @param days 保留天数，默认90
     */
    @GetMapping("/expiredMsgNum")
    public long expiredMsgNum(Integer days) {
        if (days == null) days = 90;
        return msgService.lambdaQuery()
                .lt(Msg::getCreateTime, LocalDateTime.now().minusDays(days))
                .count();
    }

    /**
     * 手动删除超过指定天数的历史聊天记录及其文件
     *
     * @param days 保留天数，默认90
     */
    @PostMapping("/cleanExpiredMsg")
    public Result cleanExpiredMsg(Integer days) {
        if (days == null) days = 90;
        List<Msg> expired = msgService.lambdaQuery()
                .lt(Msg::getCreateTime, LocalDateTime.now().minusDays(days))
                .list();
        if (expired.isEmpty()) return Result.choice("清理聊天记录", true);
        // 删除磁盘上的图片/文件（纯文字消息 msgContent 为空则跳过）
        // 文件名全局唯一，直接在 image/ 与 file/ 两个目录各尝试删除一次，
        // 这样即使消息的 isImg/msgType 标记异常也能正确清理对应文件，避免漏删
        List<Integer> ids = new ArrayList<>();
        for (Msg m : expired) {
            ids.add(m.getMsgId());
            if (m.getMsgContent() != null) {
                deleteIfExists(new File(chatPath + "image/" + m.getMsgContent()));
                deleteIfExists(new File(chatPath + "file/" + m.getMsgContent()));
            }
        }
        // 逻辑删除记录
        msgService.lambdaUpdate().in(Msg::getMsgId, ids)
                .set(Msg::getIsDel, System.currentTimeMillis())
                .update();
        return Result.choice("清理聊天记录", true);
    }

    private void deleteIfExists(File f) {
        if (f.exists()) {
            boolean del = f.delete();
            if (!del) {
                f.deleteOnExit();
            }
        }
    }

    /**
     * 用户：查询用户最近谈话的医生
     */
    @GetMapping("/client/nearDoctor")
    public NameDo clientGetNearDoctor() {
        if (UserUtil.getUser() instanceof Client client)
            return msgService.clientGetNearDoctor(client.getClientId());
        return null;
    }

    /**
     * 用户：查询特定用户与医生对话
     */
    @GetMapping("/client/aDoctor")
    public List<MsgDo> clientGetADoctor(Integer doctorId) {
        if (UserUtil.getUser() instanceof Client client)
            return msgService.clientGetADoctor(client.getClientId(), doctorId);
        return null;
    }

    /**
     * 用户：查询用户谈过话的医生
     *
     * @param doctorId 当前的医生，排序第一
     */
    @GetMapping("/client/doctorGroup")
    public List<MsgNameDo> clientGetDoctorGroup(Integer doctorId) {
        if (UserUtil.getUser() instanceof Client client)
            return msgService.clientGetDoctorGroup(client.getClientId(), doctorId);
        return null;
    }

    /**
     * 用户：发送消息
     */
    @PostMapping("/client")
    public Result sendClientMessage(@RequestBody Msg msg) {
        if (UserUtil.getUser() instanceof Client client) {
            msg.setClientId(client.getClientId());
            msg.setIsClient(true);
            return Result.choice("消息发送", msgService.save(msg));
        }
        return Result.error("未有登录");
    }

    /**
     * 医生：查询医生最近谈话的用户
     */
    @GetMapping("/doctor/nearClient")
    public NameDo doctorGetNearClient() {
        if (UserUtil.getUser() instanceof Doctor doctor)
            return msgService.doctorGetNearClient(doctor.getDoctorId());
        return null;
    }

    /**
     * 医生：查询特定医生与用户对话
     */
    @GetMapping("/doctor/aClient")
    public List<MsgDo> doctorGetAClient(Integer clientId) {
        if (UserUtil.getUser() instanceof Doctor doctor)
            return msgService.doctorGetAClient(doctor.getDoctorId(), clientId);
        return null;
    }

    /**
     * 医生：医生谈过话的用户
     */
    @GetMapping("/doctor/clientGroup")
    public List<MsgNameDo> doctorGetClientGroup(Integer clientId) {
        if (UserUtil.getUser() instanceof Doctor doctor)
            return msgService.doctorGetClientGroup(doctor.getDoctorId(), clientId);
        return null;
    }

    /**
     * 医生：发送消息
     */
    @PostMapping("/doctor")
    public Result sendDoctorMessage(@RequestBody Msg msg) {
        if (UserUtil.getUser() instanceof Doctor doctor) {
            msg.setDoctorId(doctor.getDoctorId());
            msg.setIsClient(false);
            return Result.choice("消息发送", msgService.save(msg));
        }
        return Result.error("未有登录");
    }
}