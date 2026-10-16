package com.ship.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * <p>
 * 文件上传和下载
 * </p>
 */
@RestController
@RequestMapping("/common")
public class ImgController {
    private static final Logger log = LoggerFactory.getLogger(ImgController.class);
    // 允许上传的图片扩展名白名单
    private static final java.util.Set<String> ALLOWED_IMG_SUFFIX = java.util.Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
    // 修复微信/支付宝图片因 MIME 缺失（无扩展名）导致上传失败：识别出图片才走图片流程
    private final String basePath, chatPath, reportPath;

    public ImgController(@Value("${pet-ship.images-path}") String basePath,
                         @Value("${pet-ship.chatImg-path}") String chatPath,
                         @Value("${pet-ship.report-path}") String reportPath) {
        this.basePath = basePath;
        this.chatPath = chatPath;
        this.reportPath = reportPath;
        // 确保聊天目录下的 image 与 file 子目录存在
        new File(chatImageDir()).mkdirs();
        new File(chatFileDir()).mkdirs();
        // 确保报告目录下的 image 与 file 子目录存在
        new File(reportImageDir()).mkdirs();
        new File(reportFileDir()).mkdirs();
    }

    private String chatImageDir() {
        return chatPath + "image/";
    }

    private String chatFileDir() {
        return chatPath + "file/";
    }

    private String reportImageDir() {
        return reportPath + "image/";
    }

    private String reportFileDir() {
        return reportPath + "file/";
    }

    /**
     * 文件上传，只限头像图片
     */
    @PostMapping("/upload")
    public String upload(MultipartFile myFile) {
        String fileName = getFileName(myFile);
        if (fileName == null) return null;
        String filePath = basePath + fileName;
        try {
            myFile.transferTo(new File(filePath));
            boolean resizeSuc = resize(filePath);
            log.info("转化头像尺寸{}", resizeSuc ? "成功" : "失败");
        } catch (IOException e) {
            log.warn("转存图片失败", e);
            return null;
        }
        return fileName;
    }

    /**
     * 文件上传，聊天图片消息（存 image 子目录）
     */
    @PostMapping("/upChat")
    public String upChat(MultipartFile myFile) {
        String fileName = getChatImgName(myFile);
        if (fileName == null) return null;
        String filePath = chatImageDir() + fileName;
        try {
            myFile.transferTo(new File(filePath));//转存临时文件
        } catch (IOException e) {
            log.warn("转存图片失败", e);
            return null;
        }
        return fileName;
    }

    /**
     * 文件上传，聊天文件消息（存 file 子目录）
     * 返回 JSON：{name: UUID新文件名, origin: 原始文件名}
     */
    @PostMapping("/upChatFile")
    public String upChatFile(MultipartFile myFile) throws IOException {
        if (myFile == null || myFile.isEmpty()) return null;
        String originalFilename = myFile.getOriginalFilename();
        if (originalFilename == null) return null;
        // 生成 UUID 存储名，保留扩展名用于正确返回 MIME
        String suffix = "";
        int dot = originalFilename.lastIndexOf(".");
        if (dot >= 0 && dot < originalFilename.length() - 1) suffix = originalFilename.substring(dot).toLowerCase();
        String name = UUID.randomUUID() + suffix;
        String filePath = chatFileDir() + name;
        try {
            myFile.transferTo(new File(filePath));
        } catch (IOException e) {
            log.warn("转存文件失败", e);
            return null;
        }
        return "{\"name\":\"" + name + "\",\"origin\":\"" +
                URLEncoder.encode(originalFilename, StandardCharsets.UTF_8) + "\"}";
    }

    /**
     * 文件下载，头像图片
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response) {
        sendFile(name, response, basePath, false);
    }

    /**
     * 文件下载，聊天图片/文件
     *
     * @param type image 或 file，决定去哪个子目录找
     */
    @GetMapping("/downChat")
    public void downChat(String type, String name, Boolean inline, HttpServletResponse response) {
        String dir = "file".equals(type) ? chatFileDir() : chatImageDir();
        // 预览（inline=true）时内联显示，便于浏览器直接预览；
        // 否则保持原逻辑：聊天图片内联显示、聊天文件强制下载
        boolean asAttachment = Boolean.TRUE.equals(inline) ? false : "file".equals(type);
        sendFile(name, response, dir, asAttachment);
    }

    /**
     * 报告图片上传（存 report/image 子目录，仅限图片）
     */
    @PostMapping("/upImgReport")
    public String upImgReport(MultipartFile myFile) {
        String fileName = getFileName(myFile);
        if (fileName == null) return null;
        try {
            myFile.transferTo(new File(reportImageDir() + fileName));
        } catch (IOException e) {
            log.warn("报告图片转存失败", e);
            return null;
        }
        return fileName;
    }

    /**
     * 报告文件上传（含 PDF 等非图片，存 report/file 子目录，保留原始文件名）
     * 返回 JSON：{name: UUID新文件名, origin: 原始文件名, asImg: 是否图片}
     */
    @PostMapping("/upReportFile")
    public String upReportFile(MultipartFile myFile) throws IOException {
        if (myFile == null || myFile.isEmpty()) return null;
        String originalFilename = myFile.getOriginalFilename();
        if (originalFilename == null) return null;
        String suffix = "";
        int dot = originalFilename.lastIndexOf(".");
        if (dot >= 0 && dot < originalFilename.length() - 1) suffix = originalFilename.substring(dot).toLowerCase();
        String name = UUID.randomUUID() + suffix;
        boolean asImg = ALLOWED_IMG_SUFFIX.contains(suffix);
        String dir = asImg ? reportImageDir() : reportFileDir();
        try {
            myFile.transferTo(new File(dir + name));
        } catch (IOException e) {
            log.warn("报告文件转存失败", e);
            return null;
        }
        return "{\"name\":\"" + name + "\",\"origin\":\"" +
                URLEncoder.encode(originalFilename, StandardCharsets.UTF_8) + "\",\"asImg\":" + asImg + "}";
    }

    /**
     * 文件下载，报告图片/文件
     * 由于只保存了文件名，无法判断其属于 image 还是 file 子目录，
     * 因此在两个子目录中依次查找，命中即输出；PDF 等文件以附件形式下载
     */
    @GetMapping("/downReport")
    public void downReport(String name, HttpServletResponse response) {
        if (name == null || name.isBlank()) return;
        if (name.contains("..") || name.contains("/") || name.contains("\\")) return;
        File image = new File(reportImageDir() + name);
        File file = new File(reportFileDir() + name);
        String dir = null;
        if (image.exists()) dir = reportImageDir();
        else if (file.exists()) dir = reportFileDir();
        if (dir == null) return;
        sendFile(name, response, dir, file.exists());
    }

    private void sendFile(String name, HttpServletResponse response, String imgPath, boolean asAttachment) {
        if (name == null || name.isBlank()) return;
        // 路径遍历防护：禁止使用 ../ 或路径分隔符
        if (name.contains("..") || name.contains("/") || name.contains("\\")) return;
        File file = new File(imgPath + name);
        if (!file.exists()) {
            log.warn("未找到相应文件" + name);
            return;
        }
        // 根据扩展名返回正确的 MIME 类型，浏览器可在线预览（pdf、txt、图片等）
        String mime = MediaTypeFactory.getMediaType(name)
                .map(Object::toString)
                .orElse("application/octet-stream");
        try (var fis = new FileInputStream(file); var ops = response.getOutputStream()) {
            response.setContentType(mime);
            if (asAttachment) {
                response.setHeader("Content-Disposition", "attachment; filename=\"" +
                        URLEncoder.encode(name, StandardCharsets.UTF_8) + "\"");
            }
            int len;
            byte[] bytes = new byte[8192];
            while ((len = fis.read(bytes)) != -1) {
                ops.write(bytes, 0, len);
                ops.flush();
            }
        } catch (Exception e) {
            log.warn("文件输出失败" + name, e);
        }
    }

    /**
     * 文件上传：使用UUID重新生成文件名（头像，仅限图片）
     */
    private String getFileName(MultipartFile myFile) {
        if (myFile == null || myFile.isEmpty()) return null;
        String originalFilename = myFile.getOriginalFilename();
        if (originalFilename == null) return null;
        int dot = originalFilename.lastIndexOf(".");
        if (dot < 0) return null;
        String suffix = originalFilename.substring(dot).toLowerCase();
        if (!ALLOWED_IMG_SUFFIX.contains(suffix)) return null;
        return UUID.randomUUID() + suffix;
    }

    /**
     * 聊天图片上传：保留前端传来的扩展名（前端 compressImage 已转成 WebP）
     * 前端转码成功会以 .webp 扩展名上传；转码失败降级为原图时保留其原扩展名，
     * 这样存储扩展名与文件真实内容始终一致，避免内容/扩展名不符导致显示异常
     */
    private String getChatImgName(MultipartFile myFile) {
        if (myFile == null || myFile.isEmpty()) return null;
        String originalFilename = myFile.getOriginalFilename();
        if (originalFilename == null) return null;
        int dot = originalFilename.lastIndexOf(".");
        String suffix = (dot < 0 ? "" : originalFilename.substring(dot).toLowerCase());
        if (!ALLOWED_IMG_SUFFIX.contains(suffix)) suffix = ".webp";
        return UUID.randomUUID() + suffix;
    }

    /**
     * 重写头像图片的尺寸
     */
    private boolean resize(String path) {
        String formatName = path.substring(path.lastIndexOf(".") + 1);
        File input = new File(path);
        try {
            BufferedImage img = ImageIO.read(input);
            if (img == null) {
                log.warn("图片读取失败（格式不支持？）：{}", path);
                return false;
            }
            int size = Math.min(img.getWidth(), img.getHeight());
            BufferedImage subImage = img.getSubimage(0, 0, size, size);
            Image tmp = subImage.getScaledInstance(128, 128, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(128, 128, subImage.getType());
            Graphics2D g2d = resized.createGraphics();
            boolean drawImage = g2d.drawImage(tmp, 0, 0, null);
            log.info("转化{}图片{}", formatName, drawImage ? "成功" : "失败");
            g2d.dispose();
            ImageIO.write(resized, formatName, input);
            return true;
        } catch (IOException e) {
            log.warn("图片读取失败");
            return false;
        }
    }
}