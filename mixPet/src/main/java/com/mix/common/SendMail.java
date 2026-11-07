package com.mix.common;

import com.mix.model.entity.HospitalSetting;
import com.mix.service.IHospitalSettingService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 发送邮件验证码（品牌化：发件人显示医院名、邮件内容带品牌色卡片版式）
 */
@Component
public class SendMail {
    private static final Logger log = LoggerFactory.getLogger(SendMail.class);
    private static final char[] CHARS = {'1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C',
            'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
            'W', 'X', 'Y', 'Z'};
    /** 品牌主色（与前端 --brand 默认值一致；医院自定义了品牌色则优先用医院色） */
    private static final String DEFAULT_BRAND = "#1E6FA8";
    private final String from;
    private final JavaMailSender mailSender;
    private final IHospitalSettingService hospitalSettingService;

    public SendMail(@Value("${spring.mail.username}") String from,
                    @Autowired JavaMailSender mailSender,
                    @Autowired IHospitalSettingService hospitalSettingService) {
        this.from = from;
        this.mailSender = mailSender;
        this.hospitalSettingService = hospitalSettingService;
    }

    /** 医院名（发件人显示名/邮件落款用），未配置回落系统名 */
    private String hospitalName() {
        try {
            HospitalSetting s = hospitalSettingService.get();
            if (s != null && s.getName() != null && !s.getName().isBlank()) return s.getName();
        } catch (Exception ignore) {
            // 医院信息读取失败不应影响发信
        }
        return "mixpet 宠物医疗";
    }

    /** 品牌色（医院自定义用医院色，否则默认蓝） */
    private String brandColor() {
        try {
            HospitalSetting s = hospitalSettingService.get();
            if (s != null && s.getBrandColor() != null && s.getBrandColor().matches("^#[0-9A-Fa-f]{6}$"))
                return s.getBrandColor();
        } catch (Exception ignore) {
        }
        return DEFAULT_BRAND;
    }

    /**
     * 验证一个邮箱格式是否合格，只验证常用的邮箱格式
     *
     * @param email 待验证邮箱
     * @return 验证通过
     */
    public static boolean isValidEmail(String email) {
        String EMAIL_PATTERN = "^[A-Za-z0-9][A-Za-z0-9._]*@[A-Za-z0-9]+\\.[A-Za-z0-9.-]+[A-Za-z]$";
        Pattern pattern = java.util.regex.Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /**
     * 使用系统默认字符源生成验证码
     *
     * @param verifySize 验证码长度
     * @return 验证码字符
     */
    /**
     * 个性化提醒邮件：只发给单个客户，正文不含其他客户信息。
     * 与验证码邮件分开，避免改动 sendQQEmail 的签名与内容。
     */
    public boolean sendReminderMail(String to, String subject, String content) {
        if (!isValidEmail(to)) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("【" + hospitalName() + "】" + subject);
            String safe = content == null ? "" : content
                    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\n", "<br>");
            helper.setText("<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;font-size:14px;"
                    + "color:#333;border-left:4px solid " + brandColor()
                    + ";padding:12px 16px;background:#f7f9fb\">" + safe + "</div>"
                    + "<p style='color:#888;font-size:12px'>本邮件由 " + hospitalName()
                    + " 系统自动发送，请勿回复。</p>", true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("提醒邮件发送失败 to={} err={}", to, e.getMessage());
            return false;
        }
    }

    public static String bornVerifyCode(int verifySize) {
        SecureRandom rand = new SecureRandom();
        StringBuilder verifyCode = new StringBuilder(verifySize);
        for (int i = 0; i < verifySize; i++) {
            verifyCode.append(CHARS[rand.nextInt(CHARS.length)]);
        }
        return verifyCode.toString();
    }

    /**
     * 发送简单文本邮件
     *
     * @param to 接收方的邮箱
     * @return 验证码
     */
    public String sendQQEmail(String to) {
        if (!isValidEmail(to)) return null;
        String hospName = hospitalName();
        String brand = brandColor();
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true, "UTF-8");
            // 发件人显示医院名（收件箱显示「爱心宠物医院」而不是裸 QQ 号；QQ 邮箱 SMTP 只校验地址一致，显示名可自定义）
            messageHelper.setFrom(hospName + " <" + from + ">");
            messageHelper.setTo(to);
            messageHelper.setSubject("【" + hospName + "】邮箱验证码");
            String code = bornVerifyCode(6);
            messageHelper.setText(buildHtml(code, hospName, brand), true);
            mailSender.send(message);
            log.info("成功向{}发送邮件（发件人显示名：{}）", to, hospName);
            return code;
        } catch (MessagingException e) {
            // 异常栈必须打全：STARTTLS 未配/授权码错误/网络不通都靠它定位
            log.warn("邮件发送失败（检查 application.yml 的 mail 配置、.env 的 MAIL_USER/MAIL_PASS、QQ 邮箱 SMTP 服务是否开启）", e);
        } catch (MailSendException e) {
            log.warn("发送邮件过于频繁", e);
        } catch (MailAuthenticationException e) {
            log.warn("发送者邮箱认证失败（授权码错误或 SMTP 服务未开启）", e);
        }
        return null;
    }

    /**
     * 验证码邮件 HTML（table + inline style：桌面邮件客户端兼容性最好）
     * 用 {{BRAND}}/{{HOSP}}/{{CODE}} 占位符 + replace 填充——邮件模板里 % 遍布 CSS
     * （width="100%" 等），用 String.formatted 会被 % 转义炸出 IllegalFormatConversionException
     * （2026-10-22 线上事故：Conversion = '"'），占位符方案从机制上杜绝
     */
    private String buildHtml(String code, String hospName, String brand) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head><meta charset="UTF-8"/><title>邮箱验证码</title></head>
                <body style="margin:0;padding:0;background:#f2f4f7;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f2f4f7;padding:32px 12px;">
                  <tr><td align="center">
                    <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="width:560px;max-width:100%;">
                      <!-- 品牌头：渐变横幅 + 医院名 -->
                      <tr><td style="background:linear-gradient(135deg,{{BRAND}} 0%,#4ba3e0 100%);border-radius:14px 14px 0 0;padding:26px 32px;">
                        <div style="font-size:20px;font-weight:700;color:#ffffff;letter-spacing:1px;">{{HOSP}}</div>
                        <div style="font-size:12px;color:rgba(255,255,255,.82);margin-top:4px;">宠物医疗管理系统 · 邮箱验证</div>
                      </td></tr>
                      <!-- 主体白卡 -->
                      <tr><td style="background:#ffffff;border-radius:0 0 14px 14px;padding:32px;box-shadow:0 6px 24px rgba(31,45,61,.08);">
                        <div style="font-size:15px;color:#1F2D3D;line-height:1.8;">尊敬的用户，你好：</div>
                        <div style="font-size:14px;color:#606266;line-height:1.8;margin-top:8px;">你正在进行邮箱验证，请使用以下验证码完成操作：</div>
                        <!-- 验证码大字 -->
                        <div style="margin:24px auto;text-align:center;background:#f5f8fb;border:1px dashed {{BRAND}};border-radius:10px;padding:20px 0;">
                          <span style="font-size:36px;font-weight:700;letter-spacing:10px;color:{{BRAND}};font-family:'Courier New',monospace;">{{CODE}}</span>
                        </div>
                        <div style="font-size:12px;color:#909399;line-height:1.8;">
                          · 验证码 <b style="color:#E6A23C;">5 分钟</b>内有效，请尽快使用<br/>
                          · 请勿向任何人透露验证码，本院工作人员不会主动索要<br/>
                          · 如非本人操作，请忽略本邮件，你的账号仍然安全
                        </div>
                      </td></tr>
                      <!-- 页脚 -->
                      <tr><td align="center" style="padding:20px 0 8px;font-size:11px;color:#a8abb2;line-height:1.8;">
                        本邮件由系统自动发送，请勿直接回复<br/>
                        {{HOSP}} · mixpet 宠物医疗管理系统
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """
                .replace("{{BRAND}}", brand)
                .replace("{{HOSP}}", hospName)
                .replace("{{CODE}}", code);
    }
}
