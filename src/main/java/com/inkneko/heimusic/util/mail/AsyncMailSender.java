package com.inkneko.heimusic.util.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class AsyncMailSender {

    private JavaMailSender javaMailSender;

    Logger logger = LoggerFactory.getLogger(AsyncMailSender.class);

    //fixme: weird "no such bean" tip, but actually working
    public AsyncMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * 发送邮件
     *
     * @param from 作者
     * @param to 收件人
     * @param subject 主题
     * @param text 纯文本
     */
    public void send(String from, String to, String subject, String text) {
        this.send(from, to, text, subject, false);
    }

    /**
     * 发送邮件，支持html类型内容
     * @param from 作者
     * @param to 收件人
     * @param subject 主题
     * @param text 内容
     * @param html 是否为html
     */
    @Async
    public void send(String from, String to, String subject, String text, boolean html) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        try {
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);

            javaMailSender.send(message);

        } catch (MailException | MessagingException e) {
            logger.info("failed sending email to {}，msg: {}",to, e.getMessage());
        }
    }
}
