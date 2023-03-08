package com.inkneko.heimusic.util.mail;

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

    @Autowired
    private JavaMailSender javaMailSender;

    public void send(String from, String to, String subject, String text) {
        this.send(from, to, text, subject, false);
    }

    @Async
    public void send(String from, String to, String subject, String text, boolean html) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        try {
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            //         helper.setTo(to);
            //         helper.setFrom("heimusic@inkneko.com");
            //         helper.setText(String.format("您的验证码为<span style='color: #3a62bf;'>%s</span>, 5分钟内有效", code), true);

            javaMailSender.send(message);
        } catch (MailException | MessagingException ignored) {

        }
    }
}
