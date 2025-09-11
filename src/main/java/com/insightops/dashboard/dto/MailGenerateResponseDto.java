package com.insightops.dashboard.dto;

/**
 * 메일 생성 응답 DTO
 */
public record MailGenerateResponseDto(
    String subject,
    String content,
    String categoryId,
    boolean success,
    String message
) {
}
