package com.ship.service;

import com.ship.common.PhotoEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 头像文件服务：更换头像时删除被替换的旧文件，避免 avatar/ 堆积孤儿图片
 */
@Service
public class AvatarFileService {
    /** 头像目录（pet-ship.images-path，末尾带 /） */
    private final String basePath;
    /** 默认头像白名单：不删除 */
    private static final Set<String> DEFAULT_PHOTOS = Arrays.stream(PhotoEnum.values())
            .map(PhotoEnum::getPhotoName).collect(Collectors.toSet());

    public AvatarFileService(@Value("${pet-ship.images-path}") String basePath) {
        this.basePath = basePath;
    }

    /**
     * 当新头像非空、与旧头像不同、且旧头像不是默认头像时，删除旧头像文件
     *
     * @param oldPhoto 旧头像文件名
     * @param newPhoto 新头像文件名
     */
    public void removeOldAvatar(String oldPhoto, String newPhoto) {
        if (newPhoto == null || newPhoto.isBlank()) return;   // 未传新头像，头像未变
        if (newPhoto.equals(oldPhoto)) return;                // 头像未变化
        if (oldPhoto == null || oldPhoto.isBlank()) return;   // 原本无头像
        if (DEFAULT_PHOTOS.contains(oldPhoto)) return;        // 默认头像不删
        File old = new File(basePath + oldPhoto);
        if (old.exists() && !old.delete()) old.deleteOnExit();
    }
}
