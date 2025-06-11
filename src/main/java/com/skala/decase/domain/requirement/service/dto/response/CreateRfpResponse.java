package com.skala.decase.domain.requirement.service.dto.response;

public record CreateRfpResponse(

        //ReqIdCode
        String id,  //"REQ-001",
        //type
        String type,  //"기능"
        //name
        String description_name, //"반응형 웹 기반 모바일 학습 지원",
        //description
        String description_content,  // "학습관리시스템(LMS)은 모바일 환경에서도 원활한 학습이 가능하도록 반응형 웹 기술을 적용하여 개발되어야 합니다.",
        String target_task,  //"학습관리시스템(LMS) 개발",
        String rfp_page,  //2
        String processing_detail,
        // "반응형 웹 구현을 위해 HTML5, CSS3, JavaScript와 같은 최신 웹 기술을 활용하여 다양한 화면 크기와 해상도에 적응할 수 있는 유연한 레이아웃을 설계합니다
        String raw_text, // "- 모바일에서도 학습이 가능하도록 반응형 웹으로 구성"
        //level1
        String category_large,  //"웹 기반 금융 정보 시스템",
        //level2
        String category_medium,  // "사용자 인터페이스 화면 개발"
        //level3
        String category_small,  //"UI/UX 디자인 가이드라인 개발"
        //difficulty
        String difficulty,  //"중"
        //priority
        String importance  //"중
) {
}