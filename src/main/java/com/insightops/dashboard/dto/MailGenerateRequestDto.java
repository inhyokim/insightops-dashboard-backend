package com.insightops.dashboard.dto;

/**
 * 메일 생성 요청 DTO
 */
public record MailGenerateRequestDto(
    String categoryId
) {
    // Record는 자동으로 constructor, getters, equals, hashCode, toString을 생성
}
