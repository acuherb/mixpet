package com.mix.common;

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

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 发送邮件验证码
 */
@Component
public class SendMail {
    private static final Logger log = LoggerFactory.getLogger(SendMail.class);
    private static final char[] CHARS = {'1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C',
            'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
            'W', 'X', 'Y', 'Z'};
    private final String from;
    private final JavaMailSender mailSender;

    public SendMail(@Value("${spring.mail.username}") String from, @Autowired JavaMailSender mailSender) {
        this.from = from;
        this.mailSender = mailSender;
    }

    /**
     * 验证一个邮箱格式是否合格，只验证常用的邮箱格式
     *
     * @param email 待验证邮箱
     * @return 验证通过
     */
    public static boolean isValidEmail(String email) {
        String EMAIL_PATTERN = "^[A-Za-z0-9][A-Za-z0-9._]*@[A-Za-z0-9]+\\.[A-Za-z0-9.-]+[A-Za-z]$";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /**
     * 使用系统默认字符源生成验证码
     *
     * @param verifySize 验证码长度
     * @return 验证码字符
     */
    public static String bornVerifyCode(int verifySize) {
        Random rand = new Random();
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
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);
            messageHelper.setFrom(from);
            messageHelper.setTo(to);
            messageHelper.setSubject("【验证码】邮箱验证 - mixpet 宠物医疗");
            String code = bornVerifyCode(4);
            // true 为 HTML 邮件
            String content = String.format("""
                    <!DOCTYPE html>
                    <html lang="zh-CN">
                    <head>
                       <meta charset="UTF-8">
                       <title>验证码邮件</title>
                    </head>
                    <body style="font-family:sans-serif">
                    <h2>尊敬的用户，你好：</h2>
                    <p>你正在进行邮箱验证，验证码为：</p>
                    <p style="font-size:24px;font-weight:bold;letter-spacing:4px;color:#1E6FA8">%s</p>
                    <p>验证码 5 分钟内有效，请勿转发他人。如非本人操作，请忽略本邮件。</p>
                    </body>
                    </html>""", code);
            messageHelper.setText(content, true);
            mailSender.send(message);
            log.info("成功向{}发送邮件", to);
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
}

