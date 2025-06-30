package com.skala.decase.domain.project.service;

import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.domain.ProjectInvitation;
import com.skala.decase.domain.project.exception.ProjectException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private final static String MAIL_SUBJECT_CONTENT = "Decase 프로젝트 초대 메일입니다.";
    private final static String MAIL_WELCOME_CONTENT = "Decase와 함께 하시는 것을 진심으로 환영합니다.";

    @Value("${invite.web-url}")
    private String webUrl;

    /*
        메일 전송
     */
    @Async
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendMail(ProjectInvitation projectInvitation) {
        MimeMessage mimeMailMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMailMessage, false, "UTF-8");

            mimeMessageHelper.setTo(projectInvitation.getEmail()); // 수신자
            mimeMessageHelper.setSubject(MAIL_SUBJECT_CONTENT); // 제목

            String content = getContent(projectInvitation);
            mimeMessageHelper.setText(content, true); // 내용

            javaMailSender.send(mimeMailMessage);
        } catch (MessagingException e) {
            throw new ProjectException("수신자를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private String getContent(ProjectInvitation projectInvitation) {
        String link = webUrl + "/" + projectInvitation.getToken();

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f8f9fa;
                            padding: 20px;
                        }
                        .email-container {
                            max-width: 600px;
                            margin: auto;
                            background-color: white;
                            border-radius: 12px;
                            overflow: hidden;
                            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            padding: 40px;
                            text-align: center;
                        }
                        .content {
                            padding: 30px;
                        }
                        .cta-button {
                            display: inline-block;
                            background-color: #667eea;
                            color: white;
                            padding: 12px 24px;
                            border-radius: 30px;
                            text-decoration: none;
                            font-weight: bold;
                            margin-top: 20px;
                        }
                        .cta-button:hover {
                            background-color: #5a67d8;
                        }
                        .footer {
                            text-align: center;
                            padding: 20px;
                            font-size: 13px;
                            color: #888;
                        }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="header">
                            <h1>DECASE</h1>
                            <p>프로젝트 협업 플랫폼</p>
                        </div>
                        <div class="content">
                            <p>안녕하세요! 👋</p>
                            <h2>🎉 프로젝트 초대장이 도착했어요!</h2>
                            <p><strong>Decase</strong> 프로젝트에 초대되었습니다.<br>
                            함께 멋진 프로젝트를 만들어보세요!</p>
                            <a href="%s" class="cta-button">🚀 프로젝트 참여하기</a>
                        </div>
                        <div class="footer">
                            이 메일은 Decase에서 자동으로 발송된 초대 메일입니다.<br>
                            문의사항이 있으시면 언제든지 연락 주세요.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(link);
    }

    @Async
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendWelcomeMail(Member newMember, Project project) {
        MimeMessage mimeMailMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMailMessage, false, "UTF-8");

            mimeMessageHelper.setTo(newMember.getEmail()); // 수신자
            mimeMessageHelper.setSubject(MAIL_WELCOME_CONTENT); // 제목

            String content = getWelcomeContent(newMember, project);
            mimeMessageHelper.setText(content, true); // 내용

            javaMailSender.send(mimeMailMessage);
        } catch (MessagingException e) {
            throw new ProjectException("수신자를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private String getWelcomeContent(Member member, Project project) {
        return String.format(
                "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                        "    <style>\n" +
                        "        body {\n" +
                        "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                        "            line-height: 1.6;\n" +
                        "            color: #333;\n" +
                        "            max-width: 600px;\n" +
                        "            margin: 0 auto;\n" +
                        "            background-color: #f8f9fa;\n" +
                        "            padding: 20px;\n" +
                        "        }\n" +
                        "        .email-container {\n" +
                        "            background-color: white;\n" +
                        "            border-radius: 12px;\n" +
                        "            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);\n" +
                        "            overflow: hidden;\n" +
                        "        }\n" +
                        "        .header {\n" +
                        "            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);\n" +
                        "            color: white;\n" +
                        "            padding: 30px 40px;\n" +
                        "            text-align: center;\n" +
                        "        }\n" +
                        "        .logo {\n" +
                        "            font-size: 28px;\n" +
                        "            font-weight: bold;\n" +
                        "            margin-bottom: 10px;\n" +
                        "        }\n" +
                        "        .content {\n" +
                        "            padding: 40px;\n" +
                        "        }\n" +
                        "        .greeting {\n" +
                        "            font-size: 18px;\n" +
                        "            font-weight: 600;\n" +
                        "            color: #2c3e50;\n" +
                        "            margin-bottom: 20px;\n" +
                        "        }\n" +
                        "        .main-message {\n" +
                        "            background-color: #f1f8ff;\n" +
                        "            border-left: 4px solid #667eea;\n" +
                        "            padding: 20px;\n" +
                        "            margin: 20px 0;\n" +
                        "            border-radius: 0 8px 8px 0;\n" +
                        "        }\n" +
                        "        .project-name {\n" +
                        "            color: #667eea;\n" +
                        "            font-weight: bold;\n" +
                        "            background-color: #e8f0fe;\n" +
                        "            padding: 2px 8px;\n" +
                        "            border-radius: 4px;\n" +
                        "            display: inline-block;\n" +
                        "        }\n" +
                        "        .footer {\n" +
                        "            background-color: #f8f9fa;\n" +
                        "            padding: 30px 40px;\n" +
                        "            text-align: center;\n" +
                        "            border-top: 1px solid #e9ecef;\n" +
                        "        }\n" +
                        "        .signature {\n" +
                        "            margin-top: 30px;\n" +
                        "            padding-top: 20px;\n" +
                        "            border-top: 2px solid #667eea;\n" +
                        "            text-align: right;\n" +
                        "            font-weight: 600;\n" +
                        "            color: #667eea;\n" +
                        "        }\n" +
                        "        .contact-info {\n" +
                        "            background-color: #fff3cd;\n" +
                        "            border: 1px solid #ffeaa7;\n" +
                        "            border-radius: 6px;\n" +
                        "            padding: 15px;\n" +
                        "            margin: 20px 0;\n" +
                        "            text-align: center;\n" +
                        "        }\n" +
                        "    </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <div class=\"email-container\">\n" +
                        "        <div class=\"header\">\n" +
                        "            <div class=\"logo\">🚀 DECASE</div>\n" +
                        "            <div>프로젝트 참여를 축하합니다!</div>\n" +
                        "        </div>\n" +
                        "        \n" +
                        "        <div class=\"content\">\n" +
                        "            <div class=\"greeting\">안녕하세요, %s님! 👋</div>\n" +
                        "            \n" +
                        "            <div class=\"main-message\">\n" +
                        "                <p>🎉 <strong>축하합니다!</strong></p>\n" +
                        "                <p>귀하께서 <span class=\"project-name\">%s</span> 프로젝트에 성공적으로 참여하셨음을 알려드립니다.</p>\n" +
                        "                <p>앞으로 Decase와 함께하시며 많은 성과와 발전이 있기를 진심으로 기원합니다. ✨</p>\n" +
                        "            </div>\n" +
                        "            \n" +
                        "            <div class=\"contact-info\">\n" +
                        "                <p>💬 <strong>문의사항이 있으시나요?</strong></p>\n" +
                        "                <p>프로젝트와 관련된 문의 사항이나 도움이 필요하시면 언제든지 연락 주시기 바랍니다.</p>\n" +
                        "            </div>\n" +
                        "            \n" +
                        "            <div class=\"signature\">\n" +
                        "                감사합니다.<br>\n" +
                        "                <strong>Decase 드림</strong> 💼\n" +
                        "            </div>\n" +
                        "        </div>\n" +
                        "        \n" +
                        "        <div class=\"footer\">\n" +
                        "            <p style=\"margin: 0; color: #6c757d; font-size: 14px;\">\n" +
                        "                이 메일은 Decase에서 자동으로 발송된 메일입니다.\n" +
                        "            </p>\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "</body>\n" +
                        "</html>",
                member.getName(),
                project.getName()
        );
    }
}
