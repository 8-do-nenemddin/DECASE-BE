package com.skala.decase.domain.member.controller;


import com.skala.decase.domain.member.controller.dto.response.MemberResponse;
import com.skala.decase.domain.member.service.MemberService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth API", description = "회원 정보 관리를 위한 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 정보 조회", description = "회원 정보를 조회합니다.")
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<MemberResponse>> findUser(@PathVariable("memberId") String memberId) {
        MemberResponse response = memberService.findUserInfo(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
