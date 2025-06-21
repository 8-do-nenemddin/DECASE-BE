package com.skala.decase.domain.mockup.controller.dto.request;

import java.util.List;

/**
 * 목업 생성을 위한 fast api request
 */
public record CreateMockUpRequest(

        String requirement_name, //"반응형 웹 기반 모바일 학습 지원",
        String type,  //"기능"
        List<CreateMockUpSourceRequest> sources,
        String description,  // "학습관리시스템(LMS)은 모바일 환경에서도 원활한 학습이 가능하도록 반응형 웹 기술을 적용하여 개발되어야 합니다.",
        String category_large,  //"웹 기반 금융 정보 시스템",
        String category_medium,  // "사용자 인터페이스 화면 개발"
        String category_small,  //"UI/UX 디자인 가이드라인 개발"
        String importance,  //"중
        String difficulty,  //"중"
        String requirement_id  //"요구사항 id 코드"
) {
}