package com.mix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mix.common.PhotoEnum;
import com.mix.mapper.NoticeMapper;
import com.mix.model.dto.NoticeDto;
import com.mix.model.entity.Doctor;
import com.mix.model.entity.Notice;
import com.mix.service.INoticeService;
import com.mix.util.UserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * <p>
 * 公告表 服务实现类
 * </p>
 *
 * @author MixJade
 * @since 2023-01-02
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements INoticeService {
    private static final Logger log = LoggerFactory.getLogger(NoticeServiceImpl.class);
    private final String basePath, noticePath, chatImg;

    public NoticeServiceImpl(@Value("${pet-mix.images-path}") String basePath,
                             @Value("${pet-mix.notice-path}") String noticePath,
                             @Value("${pet-mix.chatImg-path}") String chatImg) {
        this.basePath = basePath;
        this.noticePath = noticePath;
        this.chatImg = chatImg;
    }


    @Override
    public boolean delImg() {
        // 第一个参数为被删除的图片数，二是被删除的公告数
        final int[] delNum = {0, 0};
        // 一：删除冗余头像
        List<String> avatarList = baseMapper.selectImgAvatar(); // 数据库中头像图片
        avatarList.addAll(myConst()); // 加上默认的照片名
        Set<String> avatarSet = new HashSet<>(avatarList); // 去重
        String[] list01 = new File(basePath).list(); // 文件夹下的头像文件
        if (list01 != null) {
            Arrays.stream(list01)
                    .filter(i -> !avatarSet.contains(i))
                    .forEach(fileName -> {
                        if (new File(basePath + fileName).delete()) delNum[0]++;
                    });
        }
        // 二：公告文件夹下的冗余文件
        String[] list02 = new File(noticePath).list(); // 文件夹下文件名
        List<String> dbNotice = baseMapper.selectNoticeFile(); // 数据库文件名
        if (list02 != null) {
            Arrays.stream(list02)
                    .filter(i -> !dbNotice.contains(i))
                    .forEach(fileName -> {
                        if (new File(noticePath + fileName).delete()) delNum[1]++;
                    });
        }
        // 三：删除冗余聊天图片（存储在 chat/image 子目录）
        String imageDir = chatImg + "image/";
        String[] list03 = new File(imageDir).list(); // 文件夹下文件名
        List<String> dbChat = baseMapper.selectImgChat(); // 数据库文件名
        if (list03 != null) {
            Arrays.stream(list03)
                    .filter(i -> !dbChat.contains(i))
                    .forEach(fileName -> {
                        if (new File(imageDir + fileName).delete()) delNum[0]++;
                    });
        }
        log.info("删除冗余图片{}张，冗余公告{}条", delNum[0], delNum[1]);
        return delNum[0] != 0 || delNum[1] != 0;
    }

    @Override
    public boolean deleteById(Integer noticeId) {
        return this.lambdaUpdate()
                .eq(Notice::getNoticeId, noticeId)
                .set(Notice::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public boolean deleteByIds(List<Integer> idGroup) {
        return this.lambdaUpdate()
                .in(Notice::getNoticeId, idGroup)
                .set(Notice::getIsDel, System.currentTimeMillis())
                .update();
    }

    @Override
    public IPage<NoticeDto> selectByPage(String noticeName, int numPage, int pageSize) {
        IPage<NoticeDto> noticePage = baseMapper.selectNoticePage(new Page<>(numPage, pageSize), noticeName);
        noticePage.getRecords().forEach(noticeDto -> noticeDto.setTextNotice(getText(noticeDto.getNoticeFile())));
        return noticePage;
    }

    @Override
    public IPage<NoticeDto> selectNoticePagePublic(int numPage, int pageSize, String noticeName) {
        return baseMapper.selectNoticePagePublic(new Page<>(numPage, pageSize), noticeName);
    }

    @Override
    public String getNoticeText(Integer noticeId) {
        Notice notice = getById(noticeId);
        if (notice == null) return null;
        return getText(notice.getNoticeFile());
    }

    @Override
    public String getPublicNoticeText(Integer noticeId) {
        Notice notice = getById(noticeId);
        // 顾客端接口：只放行「顾客可见 + 未禁用」的公告，内部提醒（含全院客户明细）不可枚举
        if (notice == null || Boolean.TRUE.equals(notice.getNoticeType())
                || Boolean.TRUE.equals(notice.getIsDisable())) return null;
        return getText(notice.getNoticeFile());
    }

    @Override
    public List<NoticeDto> selectFour() {
        List<NoticeDto> noticeList = baseMapper.selectFour();
        noticeList.forEach(noticeDto -> noticeDto.setTextNotice(getText(noticeDto.getNoticeFile())));
        return noticeList;
    }

    @Override
    public boolean addNotice(NoticeDto noticeDto) {
        if (UserUtil.getUser() instanceof Doctor doctor) {
            noticeDto.setCreatId(doctor.getDoctorId());
            String path = UUID.randomUUID() + ".txt";
            writeText(noticeDto.getTextNotice(), path);
            Integer sort = noticeDto.getSort() == null ? 0 : noticeDto.getSort();
            return baseMapper.addNotice(noticeDto.getCreatId(), noticeDto.getNoticeTitle(), path, sort);
        } else return false;
    }

    @Override
    public boolean updateNotice(NoticeDto noticeDto) {
        if (UserUtil.getUser() instanceof Doctor doctor) {
            // 文件名一律以数据库现值为准，不信任前端传参（防路径穿越写任意文件）
            Notice existing = noticeDto.getNoticeId() == null ? null : getById(noticeDto.getNoticeId());
            if (existing == null || !isSafeFileName(existing.getNoticeFile())) return false;
            noticeDto.setUpdateId(doctor.getDoctorId());
            noticeDto.setNoticeFile(existing.getNoticeFile());
            writeText(noticeDto.getTextNotice(), existing.getNoticeFile());
            return baseMapper.updateNotice(noticeDto);
        } else return false;
    }

    @Override
    public boolean disableNotice(Integer noticeId, boolean isDis) {
        return baseMapper.disableNotice(noticeId, !isDis);
    }

    @Override
    public boolean createSystemNotice(String title, String content) {
        if (title == null || title.isBlank()) return false;
        String path = UUID.randomUUID() + ".txt";
        writeText(content, path);
        return baseMapper.addSystemNotice(title, path);
    }

    /**
     * 获取公告文本
     *
     * @param noticeFile 公告文件名
     * @return 公告文本
     */
    private String getText(String noticeFile) {
        if (!isSafeFileName(noticeFile)) {
            log.warn("拒绝读取非法公告文件名：{}", noticeFile);
            return null;
        }
        try {
            return Files.readString(Path.of(noticePath + noticeFile));
        } catch (IOException e) {
            log.warn("公告文本获取失败" + e);
            return null;
        }
    }

    /** 公告文件名只能是纯文件名：禁止 ../、绝对路径、路径分隔符 */
    private static boolean isSafeFileName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]+(\\.[A-Za-z0-9]+)?");
    }

    /**
     * 将公告内容写入文本
     *
     * @param text       公告内容
     * @param noticeFile 公告文件名
     */
    private void writeText(String text, String noticeFile) {
        if (!isSafeFileName(noticeFile)) throw new IllegalArgumentException("非法公告文件名");
        try (var sw = new OutputStreamWriter(new FileOutputStream(noticePath + noticeFile), StandardCharsets.UTF_8)) {
            sw.write(text);
        } catch (IOException e) {
            log.warn("公告文本写入失败" + e);
        }
    }

    /**
     * 通过枚举批量获取默认照片名称
     *
     * @return 设置的默认照片名称
     */
    private List<String> myConst() {
        return Arrays.stream(PhotoEnum.values())
                .map(PhotoEnum::getPhotoName)
                .toList();
    }
}