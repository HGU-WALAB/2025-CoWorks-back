package com.hiswork.backend.service;

import com.hiswork.backend.domain.User;
import com.hiswork.backend.dto.FailureRow;
import com.hiswork.backend.dto.MailRequest;
import com.hiswork.backend.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;

    // test.html 템플릿을 이용한 메일 전송 테스트
    public void sendTemplateTest(MailRequest.MailNotificationRequest mailRequest, String template, Map<String, Object> variables) {
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
            FailureRow failureRow = new FailureRow();
            failureRow.setEmail(mailRequest.getTo());
            failureRow.setTaName("unknown");


        }
    }

    /**
     * notification.html 템플릿을 이용한 메일 전송 테스트 <br/>
     * 실제로는 sendNotificationInBatch 메서드를 통해 대량 메일 전송 <br/>
     * -> 사유 : 매우 오래걸림.. 1개당 5초 이상
     * @param mailRequest
     * @param senderId
     */
    public void sendNotificationTest(MailRequest.MailNotificationRequest mailRequest, UUID senderId) {
        User sender = userRepository.findById(senderId).orElse(null);
        try {
            // 변수 주입 방법
            Context ctx = new Context();
            ctx.setVariable("message", mailRequest.getMessage());
            ctx.setVariable("notificationType", "TA 문서일지 작성");
            ctx.setVariable("username", "zzang");
            ctx.setVariable("verifyLink", "https://hisnet.handong.edu/");

            // css 주입 방법 -> inline style로 대체 why? gmail에서 css 지원을 안함
            // ClassPathResource cssRes = new ClassPathResource("mail-templates/common-styles.css");
            // String styles = new String(cssRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // ctx.setVariable("styles", styles);

            // template 처리 : notification.html
            String html = templateEngine.process("notifications", ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, true, StandardCharsets.UTF_8.name());

            // 누구에게 보낼지
            helper.setTo(mailRequest.getTo());
            // 제목을 작성
            helper.setSubject("[Hiswork] 알림이 도착했습니다.");
            // 본문 작성 (html)
            helper.setText(html, true);

            mailSender.send(mime);
        } catch (Exception e) {
            notifyTaAboutFailures(sender);
        }
    }

    /**
     * 테스트용 TA 보고 메일<br/>
     * 실제 실패 리스트 없이 테스트용 1개만
     * @param sender
     */
    private void notifyTaAboutFailures(User sender) {
        try {
            // 테스트용 FailureRow 1개만
            List<FailureRow> failures = List.of(
                    new FailureRow("student1@example.com", sender.getName())
            );

            Context ctx = new Context();
            ctx.setVariable("course", "TA 문서일지");
            ctx.setVariable("username", sender.getName());
            ctx.setVariable("failedCount", failures.size());
            ctx.setVariable("batchId", "TEST-001"); // 테스트용 고정 ID
            ctx.setVariable("failureRows", failures);
            ctx.setVariable("retryLink", "https://hisnet.handong.edu/test-retry");
            ctx.setVariable("dashboardLink", "https://hisnet.handong.edu/test-dashboard");

            String html = templateEngine.process("failure-notice-ta", ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setTo(sender.getEmail());
            helper.setSubject("[Hiswork] (TEST) 메일 전송 실패 보고서");
            helper.setText(html, true);

            mailSender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("TA 보고 메일 전송 실패", e);
        }
    }

    /**
     * <h4> 대량 메일 전송 처리 </h4> <br/>
     * users 리스트를 50개씩 잘라서 처리 <br/>
     * users : 메일을 받을 학생들 <br/>
     * sender : 관리자 (실패 보고 메일 받을 사람) <br/>
     * 추후 <br/>
     * massage : 메일 중심에 들어 갈 내용 (예: TA 문서일지 작성, 과제 제출 등) <br/>
     * notificationType : 메일 중심에 들어 갈 내용2
     *                         (예: TA 문서일지 작성, 과제 제출 등) <br/>
     *                         -> 추후에 Enum으로 관리하는게 좋을듯: Enum으로 관리하면, Enum에 따라 템플릿도 다르게 처리 가능 <br/>
     * subject : 메일 제목 (예: [Hiswork] 알림이 도착했습니다. -> notificationType에 따라 다르게 처리하면 좋을거 같다고 생각.)
     * @param users
     * @param sender
     */
    public void sendNotificationInBatch(List<User> users, User sender) {
        int chunkSize = 50;
        for (int i = 0; i < users.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, users.size());
            List<User> chunk = users.subList(i, end);
            processChunk(chunk, sender);
        }
    }

    private void processChunk(List<User> chunk, User sender) {
        List<FailureRow> failures = new ArrayList<>();

        for (User u : chunk) {
            try {
                Context ctx = new Context();
                ctx.setVariable("message", "TA 문서일지"); // 동적으로 바꿔야함
                ctx.setVariable("notificationType", "TA 문서일지 작성"); // 동적으로 바꿔야함 2
                ctx.setVariable("username", u.getName());
                ctx.setVariable("verifyLink", "https://hisnet.handong.edu/"); // 개별 링크 수정요함
                String html = templateEngine.process("notification", ctx);

                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
                helper.setTo(u.getEmail());
                helper.setSubject("[Hiswork] 알림이 도착했습니다.");
                helper.setText(html, true);

                mailSender.send(mime);

            } catch (Exception ex) {
                failures.add(new FailureRow(u.getEmail(), u.getName()));
            }
        }

        if (!failures.isEmpty()) {
            notifyTaAboutFailures(failures, sender);
        }
    }

    private void notifyTaAboutFailures(List<FailureRow> failures, User sender) {
        try {
            Context ctx = new Context();
            ctx.setVariable("course", "Ta 문서일지"); // 바꿔야함
            ctx.setVariable("username", sender.getName());
            ctx.setVariable("failedCount", failures.size());
            ctx.setVariable("batchId", "20240601-001"); // 실제 배치 ID로 바꿔야함 근데 있어야할까? 라는 고민
            ctx.setVariable("failureRows", failures);
            ctx.setVariable("retryLink", "https://hisnet.handong.edu/"); // 해당 재시도 링크로 바꿔야함
            ctx.setVariable("dashboardLink", "https://hisnet.handong.edu/"); // 해당 데시보드 링크로 바꿔야함

            String html = templateEngine.process("failure-notice-ta", ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());

            helper.setTo(sender.getEmail());
            helper.setSubject("[Hiswork] 메일 전송 실패 보고서");
            helper.setText(html, true);

            mailSender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("TA 보고 메일 전송 실패", e);
        }
    }
}