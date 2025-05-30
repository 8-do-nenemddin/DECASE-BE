package com.skala.decase.domain.member.service;

import com.skala.decase.domain.member.controller.dto.response.MemberResponse;
import com.skala.decase.domain.member.domain.Member;
import com.skala.decase.domain.member.exception.MemberException;
import com.skala.decase.domain.member.mapper.MemberMapper;
import com.skala.decase.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    private final MemberMapper memberMapper;

    public Member findByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));

    }

    public MemberResponse findUserInfo(String memberId) {
        return memberMapper.toResponse(memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new MemberException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND)));
    }
}
