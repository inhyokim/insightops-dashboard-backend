package com.insightops.dashboard.dto;

/**
 * 상담 사례 데이터 (상세보기용)
 */
public record CaseItem(
    Long vocEventId,            // VoC 이벤트 ID
    String sourceId,            // 소스 ID
    String consultingDate,      // 상담 일자
    String bigCategoryName,     // Big 카테고리 이름
    String consultingCategoryName, // Consulting 카테고리 이름 (Small 카테고리)
    String clientAge,           // 고객 연령대
    String clientGender,        // 고객 성별
    String summary              // 상담 요약
) {}

