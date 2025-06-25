package com.skala.decase.domain.requirement.controller.dto.request;


public record SourceCallbackReq(
    int source_page, 
    String original_text
) {
    
}
