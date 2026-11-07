package com.mix.controller;

import com.mix.common.Result;
import com.mix.model.dto.MsgDo;
import com.mix.model.dto.MsgNameDo;
import com.mix.model.dto.NameDo;
import com.mix.model.entity.Client;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Msg;
import com.mix.security.model.RoleConst;
import com.mix.service.IMsgService;
import com.mix.util.UserUtil;
import org.slf4j.Logger;
import org.springframework.security.access.annotation.Secured;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(MsgController.class);
    private final IMsgService msgService;
    private final String chatPath;

    @Autowired
    public MsgController(IMsgService msgService,
                         @Value("${pet-mix.chatImg-path}") String chatPath) {
        this.msgService = msgService;
        this.chatPath = chatPath;
    }

    /**
     * 查询超过指定天数的历史聊天记录数量（用于提醒手动清理）
     *
     * @param days 保留天数，默认90
     */
    @GetMapping("/expiredMsgNum")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public long expiredMsgNum(Integer days) {
        if (days == null) days = 90;
        return msgService.lambdaQuery()
                .lt(Msg::getCreateTime, LocalDateTime.now().minusDays(days))
                .count();
    }

    /**
     * 手动删除超过指定天数的历史聊天记录及其文件（仅管理员，防任意登录用户毁证）
     *
     * @param days 保留天数，默认90
     */
    @PostMapping("/cleanExpiredMsg")
    @Secured({RoleConst.MANAGER, RoleConst.ADMIN})
    public Result cleanExpiredMsg(Integer days) {
        if (days == null) days = 90;
        if (days < 0) return Result.error("保留天数不能为负数");
        List<Msg> expired = msgService.lambdaQuery()
                .lt(Msg::getCreateTime, LocalDateTime.now().minusDays(days))
                .list();
        if (expired.isEmpty()) return Result.choice("清理聊天记录", true);
        // 文件名只能是纯文件名：msgContent 为用户可控内容，拼路径前必须校验，防 ../ 删任意文件
        List<Integer> ids = new ArrayList<>();
        for (Msg m : expired) {
            ids.add(m.getMsgId());
            String name = m.getMsgContent();
            if (isSafeFileName(name)) {
                deleteIfExists(new File(chatPath + "image/" + name));
                deleteIfExists(new File(chatPath + "file/" + name));
            }
        }
        msgService.lambdaUpdate().in(Msg::getMsgId, ids)
                .set(Msg::getIsDel, System.currentTimeMillis())
                .update();
        return Result.choice("清理聊天记录", true);
    }

    private static boolean isSafeFileName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]+(\\.[A-Za-z0-9]+)?");
    }

    private void deleteIfExists(File f) {
        if (f.exists()) {
            boolean del = f.delete();
            if (!del) {
                log.warn("文件删除失败，将标记下次JVM退出时删除: {}", f.getAbsolutePath());
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
            msg.setIsRead(false);
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
            msg.setIsRead(false);
            return Result.choice("消息发送", msgService.save(msg));
        }
        return Result.error("未有登录");
    }
}