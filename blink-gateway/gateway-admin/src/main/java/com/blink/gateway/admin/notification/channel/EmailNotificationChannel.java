package com.blink.gateway.admin.notification.channel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.blink.gateway.admin.notification.entity.NotificationChannelConfigDO;
import com.blink.gateway.admin.notification.mapper.NotificationChannelConfigMapper;
import com.blink.gateway.admin.notification.model.ChannelType;
import com.blink.gateway.admin.notification.model.NotificationMessage;
import com.blink.gateway.admin.notification.model.SendResult;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * 邮件通知渠道
 *
 * @author binblink
 * @since 2026-04-28
 */
@Component
@Slf4j
public class EmailNotificationChannel extends AbstractNotificationChannel {

    private final NotificationChannelConfigMapper configMapper;
    private JavaMailSender mailSender;
    private EmailConfig emailConfig;

    public EmailNotificationChannel(NotificationChannelConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @PostConstruct
    public void init() {
        loadConfig();
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.EMAIL;
    }

    @Override
    public boolean isAvailable() {
        return mailSender != null && emailConfig != null;
    }

    @Override
    protected SendResult validate(NotificationMessage message) {
        if (CollUtil.isEmpty(message.getRecipients())) {
            return SendResult.failure(getChannelType(), "收件人不能为空");
        }
        return SendResult.success(getChannelType());
    }

    @Override
    protected SendResult doSend(NotificationMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(message.getRecipients().toArray(new String[0]));
            helper.setSubject(message.getTitle());
            helper.setText(message.getContent(), true);

            mailSender.send(mimeMessage);

            return SendResult.success(getChannelType());
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("[EmailChannel] 邮件发送失败", e);
            return SendResult.failure(getChannelType(), e.getMessage());
        }
    }

    /**
     * 从数据库加载配置
     */
    private void loadConfig() {
        NotificationChannelConfigDO config = configMapper.selectByChannelType("email");
        if (config == null || config.getEnabled() == null || config.getEnabled() != 1) {
            log.info("[EmailChannel] 邮件渠道未配置或未启用");
            return;
        }

        try {
            emailConfig = JSONUtil.toBean(config.getConfigJson(), EmailConfig.class);
            mailSender = createMailSender(emailConfig);
            log.info("[EmailChannel] 邮件渠道初始化成功 | host={}", emailConfig.getHost());
        } catch (Exception e) {
            log.error("[EmailChannel] 邮件配置解析失败", e);
        }
    }

    /**
     * 创建邮件发送器
     */
    private JavaMailSender createMailSender(EmailConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (Boolean.TRUE.equals(config.getSslEnabled())) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        return sender;
    }

    /**
     * 邮件配置
     */
    @Data
    public static class EmailConfig {
        private String host;
        private Integer port;
        private String username;
        private String password;
        private Boolean sslEnabled;
        private String fromAddress;
        private String fromName;
    }
}
