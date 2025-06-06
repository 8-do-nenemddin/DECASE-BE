package com.skala.decase.domain.member.controller;

import com.skala.decase.domain.member.controller.dto.request.DeleteRequest;
import com.skala.decase.domain.member.controller.dto.request.DuplicationCheckRequest;
import com.skala.decase.domain.member.controller.dto.request.LogInRequest;
import com.skala.decase.domain.member.controller.dto.request.SignUpRequest;
import com.skala.decase.domain.member.controller.dto.response.DeleteResponse;
import com.skala.decase.domain.member.controller.dto.response.DuplicationCheckResponse;
import com.skala.decase.domain.member.controller.dto.response.MemberResponse;
import com.skala.decase.domain.member.domain.AuthApiDocument;
import com.skala.decase.domain.member.service.AuthService;
import com.skala.decase.global.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth API", description = "로그인/회원가입 관리를 위한 api 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     *
     * @param request
     * @return
     */
    @PostMapping("/signup")
    @AuthApiDocument.SignUpApiDoc
    public ResponseEntity<ApiResponse<String>> signUp(@RequestBody SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok().body(ApiResponse.created("회원가입 성공"));
    }

    /**
     * 로그인
     *
     * @param request
     * @return
     */
    @PostMapping("/login")
    @AuthApiDocument.LoginApiDoc
    public ResponseEntity<ApiResponse<MemberResponse>> login(@RequestBody LogInRequest request) {
        MemberResponse response = authService.login(request);
        return ResponseEntity.ok().body(ApiResponse.success(response));
    }

    @Operation(summary = "회원 탈퇴", description = "회원 정보를 삭제합니다.")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteMember(@PathVariable Long memberId, @RequestBody DeleteRequest request) {
        DeleteResponse response = authService.withdrawal(memberId, request);

        return ResponseEntity.ok()
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "회원 아이디 중복 확인", description = "회원 아이디가 중복되는지 확인합니다.")
    @PostMapping("/check")
    public ResponseEntity<ApiResponse<DuplicationCheckResponse>> checkIdDuplication(@RequestBody DuplicationCheckRequest request) {
        DuplicationCheckResponse response = authService.checkDuplication(request);

        return ResponseEntity.ok()
                .body(ApiResponse.success(response));
    }
}
