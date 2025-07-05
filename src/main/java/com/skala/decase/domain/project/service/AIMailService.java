package com.skala.decase.domain.project.service;

import com.skala.decase.domain.job.domain.JobName;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.exception.ProjectException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * AI 작업 완료 성공/실패 메일 알림
 */
@Service
@RequiredArgsConstructor
public class AIMailService {

    private final JavaMailSender javaMailSender;

    @Value("${mail-alert.web-url}")
    private String webUrl;

    /**
     * @param jobName 작업 종류
     * @param member  메일 수신자 (지금은 PM만)
     * @param status  작업 성공/실패 여부 상태
     */
    @Async
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendMail(JobName jobName, Member member, String status, Project project, int revisionCount) {
        MimeMessage mimeMailMessage = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMailMessage, false, "UTF-8");
            mimeMessageHelper.setTo(member.getEmail());
            mimeMessageHelper.setSubject(getSubject(jobName, status, project));

            String content = getContent(jobName, status, project, revisionCount);
            mimeMessageHelper.setText(content, true); // 내용

            javaMailSender.send(mimeMailMessage);
        } catch (MessagingException e) {
            throw new ProjectException("수신자를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private String getSubject(JobName jobName, String status, Project project) {
        String projectName = project.getName();
        String jobDesc = getJobDescription(jobName);
        String statusDesc = getStatusDescription(status);
        return String.format("[%s] %s %s", projectName, jobDesc, statusDesc);
    }

    private String getJobDescription(JobName jobName) {
        return switch (jobName) {
            case SRS -> "요구사항 정의서 생성";
            case UPDATE -> "요구사항 정의서 업데이트";
            case MOCKUP -> "목업 생성";
            case SCREEN_SPEC -> "화면 정의서 생성";
            case ASIS -> "현황 보고서 생성";
            default -> "작업";
        };
    }

    private String getStatusDescription(String status) {
        return switch (status) {
            case "SUCCESS", "COMPLETED" -> "성공";
            case "FAILED" -> "실패";
            default -> status;
        };
    }

    private String getContent(JobName jobName, String status, Project project, int revicionCount) {
        String projectName = project.getName();
        String link = webUrl + "/projects/" + project.getProjectId() + "?revision=" + revicionCount;  //프로젝트 바로가기 링크
        String jobDesc = getJobDescription(jobName);
        String statusDesc = getStatusDescription(status);
        String mainMessage;
        String detailMessage;
        String buttonText = "프로젝트 바로가기";
        String icon = status.equals("FAILED") ? "❌" : "✅";
        String color = status.equals("FAILED") ? "#e74c3c" : "#667eea";

        // 작업/상태별 안내 메시지
        if (jobName == JobName.SRS || jobName == JobName.UPDATE) {
            if (status.equals("SUCCESS") || status.equals("COMPLETED")) {
                mainMessage = "요구사항 정의서가 성공적으로 처리되었습니다.";
                detailMessage = "요구사항 정의서가 정상적으로 생성/업데이트되었습니다.";
            } else {
                mainMessage = "요구사항 정의서 처리에 실패하였습니다.";
                detailMessage = "요구사항 정의서 생성/업데이트 중 오류가 발생하였습니다.";
            }
        } else if (jobName == JobName.MOCKUP) {
            if (status.equals("SUCCESS") || status.equals("COMPLETED")) {
                mainMessage = "목업이 성공적으로 생성되었습니다.";
                detailMessage = "목업이 정상적으로 생성되었습니다.";
            } else {
                mainMessage = "목업 생성에 실패하였습니다.";
                detailMessage = "목업 생성 중 오류가 발생하였습니다.";
            }
        } else if (jobName == JobName.ASIS) {
            if (status.equals("SUCCESS") || status.equals("COMPLETED")) {
                mainMessage = "현황 보고서가 성공적으로 생성되었습니다.";
                detailMessage = "현황 보고서가 정상적으로 생성되었습니다.";
            } else {
                mainMessage = "현황 보고서 생성에 실패하였습니다.";
                detailMessage = "현황 보고서 생성 중 오류가 발생하였습니다.";
            }
        } else {
            mainMessage = "작업 결과 알림";
            detailMessage = "작업이 처리되었습니다.";
        }

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
                                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background: linear-gradient(135deg, %s 0%%, #764ba2 100%%); border-radius: 15px 15px 0 0;">
                                    <tr>
                                        <td style="padding: 40px; text-align: center; color: white;">
                                            <h1 style="margin: 0; font-size: 42px; font-weight: bold; color: white; text-shadow: 2px 2px 4px rgba(0,0,0,0.3);">DECASE %s</h1>
                                            <p style="margin: 10px 0 0 0; font-size: 16px; color: rgba(255,255,255,0.9);">프로젝트 협업 플랫폼</p>
                                        </td>
                                    </tr>
                                </table>
                
                                <!-- 메인 콘텐츠 -->
                                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: white;">
                                    <tr>
                                        <td style="padding: 40px;">
                                            <p style="margin: 0 0 20px 0; font-size: 18px; color: #333;">안녕하세요!</p>
                
                                            <h2 style="margin: 0 0 20px 0; font-size: 24px; color: %s; text-align: center;">%s %s</h2>
                
                                            <!-- 프로젝트 정보 박스 -->
                                            <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background: linear-gradient(135deg, rgba(102,126,234,0.1) 0%%, rgba(240,147,251,0.1) 100%%); border-radius: 12px; border: 2px solid #667eea; margin: 20px 0;">
                                                <tr>
                                                    <td style="padding: 30px; text-align: center;">
                                                        <h3 style="margin: 0 0 15px 0; font-size: 22px; color: #333; font-weight: bold;">%s</h3>
                                                        <p style="margin: 0; font-size: 16px; color: #666; line-height: 1.5;">%s</p>
                                                    </td>
                                                </tr>
                                            </table>
                
                                            <!-- 장식 구분선 -->
                                            <p style="text-align: center; margin: 30px 0; font-size: 20px; color: #ccc;">%s</p>
                
                                            <!-- 버튼 -->
                                            <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                                <tr>
                                                    <td style="text-align: center; padding: 20px 0;">
                                                        <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 16px 35px; border-radius: 30px; text-decoration: none; font-weight: bold; font-size: 16px; box-shadow: 0 4px 15px rgba(102,126,234,0.4);">🚀 %s</a>
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
                                            이 메일은 Decase에서 자동으로 발송된 작업 결과 알림 메일입니다.<br>
                                            문의사항이 있으시면 언제든지 연락 주세요.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(color, statusDesc, color, jobDesc, statusDesc, projectName,
                mainMessage + "<br>" + detailMessage, icon, link, buttonText);
    }

}
