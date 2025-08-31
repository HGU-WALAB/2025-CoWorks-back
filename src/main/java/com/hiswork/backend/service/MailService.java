package com.hiswork.backend.service;

import com.hiswork.backend.dto.MailRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;


    public void sendTemplate(MailRequest mailRequest, String template, Map<String, Object> variables) {
        try {
            Context ctx = new Context();
            variables.forEach(ctx::setVariable);
            String html = templateEngine.process(template, ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, true, StandardCharsets.UTF_8.name());

            helper.setTo(mailRequest.getTo());
            helper.setSubject("이것은 test입니다.");
            helper.setText(html, true);

            mailSender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("메일 전송 실패", e);
        }
    }

    public void sendNotification(MailRequest mailRequest, String template, Map<String, Object> variables) {
        try {
            // 변수 주입 방법
            Context ctx = new Context();
            variables.forEach(ctx::setVariable);
            ctx.setVariable("message", mailRequest.getMessage());
            ctx.setVariable("notificationType", "알림에 대한 test진행합니다.");

            // css 주입 방법
//            ClassPathResource cssRes = new ClassPathResource("mail-templates/common-styles.css");
//            String styles = new String(cssRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
//            ctx.setVariable("styles", styles);

            String html = templateEngine.process(template, ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, true, StandardCharsets.UTF_8.name());

            helper.setTo(mailRequest.getTo());
            helper.setSubject("이것은 이제 알림에 대한 test입니다.");
            helper.setText(html, true);

            mailSender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("메일 전송 실패", e);
        }
    }
}