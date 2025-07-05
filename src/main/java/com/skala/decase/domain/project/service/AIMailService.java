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
        return String.format("[DECASE][%s] %s %s 알림", projectName, jobDesc, statusDesc);
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
            case "SUCCESS", "COMPLETED" -> "완료";
            case "FAILED" -> "실패";
            default -> status;
        };
    }

    private String getContent(JobName jobName, String status, Project project, int revisionCount) {
        String projectName = project.getName();
        String link = webUrl + "/projects/" + project.getProjectId() + "?revision=" + revisionCount;
        String jobDesc = getJobDescription(jobName);
        String statusDesc = getStatusDescription(status);
        String icon = status.equals("FAILED") ? "❌" : "✅";
        String btnColor = "#111";
        String btnTextColor = "#fff";
        String divider = "<hr style=\"border:0;border-top:1px solid #eee;margin:32px 0;\">";

        String detailMessage;
        if (jobName == JobName.SRS || jobName == JobName.UPDATE) {
            if (status.equals("SUCCESS") || status.equals("COMPLETED")) {
                detailMessage = "요구사항 정의서가 정상적으로 생성/업데이트되었습니다.\n\n팀원들과 함께 결과를 확인해보세요.";
            } else {
                detailMessage = "요구사항 정의서 생성/업데이트 중 예상치 못한 오류가 발생했습니다.\n\n잠시 후 다시 시도해주시거나, 문제가 지속될 경우 관리자에게 문의해 주세요.";
            }
        } else if (jobName == JobName.MOCKUP) {
            if (status.equals("SUCCESS") || status.equals("COMPLETED")) {
                detailMessage = "목업이 정상적으로 생성되었습니다.\n\n새로운 화면을 팀원들과 함께 검토해보세요.";
            } else {
                detailMessage = "목업 생성 중 오류가 발생했습니다.\n\n잠시 후 다시 시도해주시거나, 문제가 지속될 경우 관리자에게 문의해 주세요.";
            }
        } else if (jobName == JobName.ASIS) {
            if (status.equals("SUCCESS") || status.equals("COMPLETED")) {
                detailMessage = "현황 보고서가 정상적으로 생성되었습니다.\n\n팀원들과 함께 내용을 확인해보세요.";
            } else {
                detailMessage = "현황 보고서 생성 중 오류가 발생했습니다.\n\n잠시 후 다시 시도해주시거나, 문제가 지속될 경우 관리자에게 문의해 주세요.";
            }
        } else {
            if (status.equals("SUCCESS") || status.equals("COMPLETED")) {
                detailMessage = "작업이 정상적으로 처리되었습니다.";
            } else {
                detailMessage = "작업 처리 중 오류가 발생했습니다.\n\n잠시 후 다시 시도해주시거나, 문제가 지속될 경우 관리자에게 문의해 주세요.";
            }
        }

        // 줄바꿈 처리
        String detailMessageHtml = detailMessage.replace("\n", "<br/>");

        return """
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>DECASE 작업 결과 알림</title>
</head>
<body style="background:#fff; margin:0; padding:0; font-family:'Malgun Gothic','맑은 고딕',Arial,sans-serif; color:#222;">
<table width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:520px;margin:40px auto;border:1px solid #eee;border-radius:12px;background:#fff;">
    <tr>
        <td style="padding:40px 40px 24px 40px;">
            <h1 style="margin:0 0 8px 0;font-size:28px;font-weight:700;letter-spacing:-1px;">DECASE 알림</h1>
            <div style="font-size:15px;color:#888;margin-bottom:24px;">Break The Case</div>
            <div style="font-size:16px;margin-bottom:32px;">안녕하세요, <b>DECASE</b>에서 AI 작업 처리 결과를 안내드립니다.</div>
            <div style="font-size:20px;font-weight:600;margin-bottom:12px;">%s %s %s</div>
            <div style="font-size:16px;line-height:1.7;margin-bottom:16px;white-space:pre-line;">%s</div>
            %s
            <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#fafafa;border:1px solid #eee;border-radius:8px;padding:0;margin-bottom:32px;">
                <tr>
                    <td style="padding:20px 18px;">
                        <div style="font-size:15px;color:#666;margin-bottom:6px;">프로젝트</div>
                        <div style="font-size:18px;font-weight:600;">%s</div>
                    </td>
                </tr>
            </table>
            <a href="%s" style="display:inline-block;background:%s;color:%s;padding:14px 32px;border-radius:28px;text-decoration:none;font-weight:600;font-size:16px;letter-spacing:0.5px;">DECASE 바로가기</a>
            %s
            <div style="font-size:13px;color:#aaa;margin-top:32px;line-height:1.6;">
                본 메일은 <b>DECASE</b>에서 자동 발송된 알림입니다.<br><br><b>DECASE 팀 드림</b>
            </div>
        </td>
    </tr>
</table>
</body>
</html>
""".formatted(
    icon, jobDesc, statusDesc, // %s %s %s (아이콘, 작업명, 상태)
    detailMessageHtml,         // %s (본문)
    divider,                   // %s (구분선)
    projectName,               // %s (프로젝트명)
    link, btnColor, btnTextColor, // %s %s %s (링크, 버튼배경, 버튼글자색)
    divider                    // %s (구분선)
);
    }

}
