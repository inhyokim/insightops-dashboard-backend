package com.insightops.dashboard.dto;

/**
 * 상담 사례 데이터 (상세보기용)
 * Normalization Service에서 받아오는 데이터 형식에 맞춤
 */
public record CaseItem(
    Long vocEventId,            // VoC 이벤트 ID
    String sourceId,            // 소스 ID
    String consultingDate,      // 상담 일자
    String bigCategoryName,     // Big 카테고리 이름
    String consultingCategoryName, // Consulting 카테고리 이름 (Small 카테고리)
    String clientAge,           // 고객 연령대 (예: "20", "30", "40")
    String clientGender,        // 고객 성별 (예: "여자", "남자")
    String analysisResult       // 분석 결과 (summary 대신 analysis_result)
) {}

