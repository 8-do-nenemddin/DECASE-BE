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
        String projectName = projectInvitation.getProject().getName();

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="margin: 0; padding: 20px; font-family: 'Malgun Gothic', '맑은 고딕', Arial, sans-serif; background-color: #f5f5f5;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width: 600px; margin: 0 auto;">
                    <tr>
                        <td>
                            <!-- 헤더 -->
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 15px 15px 0 0;">
                                <tr>
                                    <td style="padding: 40px; text-align: center; color: white;">
                                        <h1 style="margin: 0; font-size: 42px; font-weight: bold; color: white; text-shadow: 2px 2px 4px rgba(0,0,0,0.3);">DECASE</h1>
                                        <p style="margin: 10px 0 0 0; font-size: 16px; color: rgba(255,255,255,0.9);">프로젝트 협업 플랫폼</p>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- 메인 콘텐츠 -->
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: white;">
                                <tr>
                                    <td style="padding: 40px;">
                                        <p style="margin: 0 0 20px 0; font-size: 18px; color: #333;">안녕하세요!</p>
                                        
                                        <h2 style="margin: 0 0 20px 0; font-size: 24px; color: #667eea; text-align: center;">요구 사항 정의서 자동화 서비스 DECASE입니다.</h2>
                                        
                                        <!-- 프로젝트 정보 박스 -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background: linear-gradient(135deg, rgba(102,126,234,0.1) 0%%, rgba(240,147,251,0.1) 100%%); border-radius: 12px; border: 2px solid #667eea; margin: 20px 0;">
                                            <tr>
                                                <td style="padding: 30px; text-align: center;">
                                                    <h3 style="margin: 0 0 15px 0; font-size: 22px; color: #333; font-weight: bold;">%s</h3>
                                                    <p style="margin: 0; font-size: 16px; color: #666; line-height: 1.5;">프로젝트에 초대되었습니다.<br>함께 멋진 프로젝트를 만들어보세요!</p>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- 장식 구분선 -->
                                        <p style="text-align: center; margin: 30px 0; font-size: 20px; color: #ccc;">✨ ⭐ ✨</p>
                                        
                                        <!-- 버튼 -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                            <tr>
                                                <td style="text-align: center; padding: 20px 0;">
                                                    <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 16px 35px; border-radius: 30px; text-decoration: none; font-weight: bold; font-size: 16px; box-shadow: 0 4px 15px rgba(102,126,234,0.4);">🚀 프로젝트 참여하기</a>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- 푸터 -->
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: #f8f9fa; border-radius: 0 0 15px 15px;">
                                <tr>
                                    <td style="padding: 25px; text-align: center; color: #888; font-size: 13px; line-height: 1.6;">
                                        <strong style="color: #555;">DECASE 팀</strong><br>
                                        이 메일은 Decase에서 자동으로 발송된 초대 메일입니다.<br>
                                        문의사항이 있으시면 언제든지 연락 주세요.
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(projectName, link);
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
                        "</head>\n" +
                        "<body style=\"margin: 0; padding: 20px; font-family: 'Malgun Gothic', '맑은 고딕', Arial, sans-serif; background-color: #f5f5f5;\">\n" +
                        "    <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"max-width: 600px; margin: 0 auto;\">\n" +
                        "        <tr>\n" +
                        "            <td>\n" +
                        "                <!-- 헤더 -->\n" +
                        "                <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); border-radius: 15px 15px 0 0;\">\n" +
                        "                    <tr>\n" +
                        "                        <td style=\"padding: 40px; text-align: center; color: white;\">\n" +
                        "                            <h1 style=\"margin: 0; font-size: 42px; font-weight: bold; color: white; text-shadow: 2px 2px 4px rgba(0,0,0,0.3);\">🚀 DECASE</h1>\n" +
                        "                            <p style=\"margin: 10px 0 0 0; font-size: 16px; color: rgba(255,255,255,0.9);\">프로젝트 참여를 축하합니다!</p>\n" +
                        "                        </td>\n" +
                        "                    </tr>\n" +
                        "                </table>\n" +
                        "                \n" +
                        "                <!-- 메인 콘텐츠 -->\n" +
                        "                <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color: white;\">\n" +
                        "                    <tr>\n" +
                        "                        <td style=\"padding: 40px;\">\n" +
                        "                            <p style=\"margin: 0 0 20px 0; font-size: 18px; color: #333; font-weight: bold;\">안녕하세요, %s님! 👋</p>\n" +
                        "                            \n" +
                        "                            <!-- 축하 메시지 박스 -->\n" +
                        "                            <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background: linear-gradient(135deg, rgba(102,126,234,0.1) 0%%, rgba(240,147,251,0.1) 100%%); border-radius: 12px; border: 2px solid #667eea; margin: 20px 0;\">\n" +
                        "                                <tr>\n" +
                        "                                    <td style=\"padding: 30px;\">\n" +
                        "                                        <h2 style=\"margin: 0 0 15px 0; font-size: 22px; color: #667eea;\">🎉 축하합니다!</h2>\n" +
                        "                                        <p style=\"margin: 0 0 15px 0; font-size: 16px; color: #333; line-height: 1.6;\">귀하께서 <strong style=\"color: #667eea; background-color: rgba(102,126,234,0.1); padding: 2px 6px; border-radius: 4px;\">%s</strong> 프로젝트에 성공적으로 참여하셨음을 알려드립니다.</p>\n" +
                        "                                        <p style=\"margin: 0; font-size: 16px; color: #333; line-height: 1.6;\">앞으로 Decase와 함께하시며 많은 성과와 발전이 있기를 진심으로 기원합니다. ✨</p>\n" +
                        "                                    </td>\n" +
                        "                                </tr>\n" +
                        "                            </table>\n" +
                        "                            \n" +
                        "                            <!-- 문의 안내 박스 -->\n" +
                        "                            <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color: #fff3cd; border: 2px solid #ffeaa7; border-radius: 8px; margin: 20px 0;\">\n" +
                        "                                <tr>\n" +
                        "                                    <td style=\"padding: 20px; text-align: center;\">\n" +
                        "                                        <p style=\"margin: 0 0 10px 0; font-size: 16px; color: #856404; font-weight: bold;\">💬 문의사항이 있으시나요?</p>\n" +
                        "                                        <p style=\"margin: 0; font-size: 14px; color: #856404; line-height: 1.5;\">프로젝트와 관련된 문의 사항이나 도움이 필요하시면 언제든지 연락 주시기 바랍니다.</p>\n" +
                        "                                    </td>\n" +
                        "                                </tr>\n" +
                        "                            </table>\n" +
                        "                            \n" +
                        "                            <!-- 장식 구분선 -->\n" +
                        "                            <p style=\"text-align: center; margin: 30px 0; font-size: 20px; color: #ccc;\">✨ ⭐ ✨</p>\n" +
                        "                            \n" +
                        "                            <!-- 서명 -->\n" +
                        "                            <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin-top: 30px; border-top: 2px solid #667eea; padding-top: 20px;\">\n" +
                        "                                <tr>\n" +
                        "                                    <td style=\"text-align: right; padding: 20px 0;\">\n" +
                        "                                        <p style=\"margin: 0; font-size: 16px; color: #667eea; font-weight: bold; line-height: 1.4;\">감사합니다.<br><strong>Decase 드림</strong> 💼</p>\n" +
                        "                                    </td>\n" +
                        "                                </tr>\n" +
                        "                            </table>\n" +
                        "                        </td>\n" +
                        "                    </tr>\n" +
                        "                </table>\n" +
                        "                \n" +
                        "                <!-- 푸터 -->\n" +
                        "                <table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color: #f8f9fa; border-radius: 0 0 15px 15px;\">\n" +
                        "                    <tr>\n" +
                        "                        <td style=\"padding: 25px; text-align: center; color: #888; font-size: 13px; line-height: 1.6;\">\n" +
                        "                            <p style=\"margin: 0;\">이 메일은 Decase에서 자동으로 발송된 메일입니다.</p>\n" +
                        "                        </td>\n" +
                        "                    </tr>\n" +
                        "                </table>\n" +
                        "            </td>\n" +
                        "        </tr>\n" +
                        "    </table>\n" +
                        "</body>\n" +
                        "</html>",
                member.getName(),
                project.getName()
        );
    }
}
