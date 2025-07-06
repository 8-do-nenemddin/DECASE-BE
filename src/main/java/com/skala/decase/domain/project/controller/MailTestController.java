package com.skala.decase.domain.project.controller;

import com.skala.decase.domain.job.domain.JobName;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.service.MemberService;
import com.skala.decase.domain.project.domain.Project;
import com.skala.decase.domain.project.service.AIMailService;
import com.skala.decase.domain.project.service.ProjectService;
import com.skala.decase.global.model.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "MailTest API", description = "이메일 발송 테스트를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mail-test")
public class MailTestController {

    private final AIMailService aiMailService;
    private final MemberService memberService;
    private final ProjectService projectService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendMail() {
        Member member = memberService.findByMemberId(1L);
        Project project = projectService.findByProjectId(1L);
        
        aiMailService.sendMail(JobName.SRS, member, "COMPLETED", project,1);
        aiMailService.sendMail(JobName.UPDATE, member, "COMPLETED", project,1);
        aiMailService.sendMail(JobName.MOCKUP, member, "SUCCESS", project,1);
        aiMailService.sendMail(JobName.SCREEN_SPEC, member, "COMPLETED", project,1);
        aiMailService.sendMail(JobName.ASIS, member, "COMPLETED", project,1);

        aiMailService.sendMail(JobName.SRS, member, "FAILED", project,1);
        aiMailService.sendMail(JobName.UPDATE, member, "FAILED", project,1);
        aiMailService.sendMail(JobName.MOCKUP, member, "FAILED", project,1);
        aiMailService.sendMail(JobName.SCREEN_SPEC, member, "FAILED", project,1);
        aiMailService.sendMail(JobName.ASIS, member, "FAILED", project,1);

        return ResponseEntity.ok(ApiResponse.success("이메일 발송 테스트"));
    }
}
