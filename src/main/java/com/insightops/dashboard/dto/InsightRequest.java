package com.insightops.dashboard.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AI 인사이트 요청 DTO
 */
public record InsightRequest(
    String analysisType,                    // 분석 유형 (trend, category, age, gender, comprehensive)
    LocalDate startDate,                    // 시작일
    LocalDate endDate,                      // 종료일
    String period,                          // 기간 단위 (daily, weekly, monthly)
    List<String> categories,                // 분석할 카테고리 목록
    List<String> ageGroups,                 // 분석할 연령대 목록
    List<String> genders,                   // 분석할 성별 목록
    Map<String, Object> timeSeriesData,     // 시계열 데이터
    Map<String, Object> trendData,          // 트렌드 데이터
    String language,                        // 응답 언어 (ko, en)
    String detailLevel,                     // 상세 수준 (basic, detailed, comprehensive)
    List<String> focusAreas                 // 집중 분석 영역
) {
    
    /**
     * 기본 생성자
     */
    public InsightRequest(String analysisType, LocalDate startDate, LocalDate endDate, String period) {
        this(analysisType, startDate, endDate, period, null, null, null, null, null, "ko", "detailed", null);
    }
    
    /**
     * 트렌드 분석용 생성자
     */
    public InsightRequest(String analysisType, LocalDate startDate, LocalDate endDate, String period, 
                         Map<String, Object> timeSeriesData, Map<String, Object> trendData) {
        this(analysisType, startDate, endDate, period, null, null, null, timeSeriesData, trendData, "ko", "detailed", null);
    }
    
    /**
     * 필터링된 분석용 생성자
     */
    public InsightRequest(String analysisType, LocalDate startDate, LocalDate endDate, String period,
                         List<String> categories, List<String> ageGroups, List<String> genders) {
        this(analysisType, startDate, endDate, period, categories, ageGroups, genders, null, null, "ko", "detailed", null);
    }
    
    /**
     * 분석 유형이 트렌드인지 확인
     */
    public boolean isTrendAnalysis() {
        return "trend".equalsIgnoreCase(analysisType);
    }
    
    /**
     * 분석 유형이 카테고리인지 확인
     */
    public boolean isCategoryAnalysis() {
        return "category".equalsIgnoreCase(analysisType);
    }
    
    /**
     * 분석 유형이 종합인지 확인
     */
    public boolean isComprehensiveAnalysis() {
        return "comprehensive".equalsIgnoreCase(analysisType);
    }
    
    /**
     * 필터가 적용되었는지 확인
     */
    public boolean hasFilters() {
        return (categories != null && !categories.isEmpty()) ||
               (ageGroups != null && !ageGroups.isEmpty()) ||
               (genders != null && !genders.isEmpty());
    }
}
