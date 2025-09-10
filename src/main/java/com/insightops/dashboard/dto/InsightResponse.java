package com.insightops.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 인사이트 응답 DTO
 */
public record InsightResponse(
    String analysisType,                    // 분석 유형
    String summary,                         // 요약 인사이트
    List<String> keyFindings,               // 주요 발견사항
    List<String> recommendations,           // 추천사항
    Map<String, Object> trendAnalysis,      // 트렌드 분석 결과
    Map<String, Object> statisticalData,    // 통계 데이터
    List<String> riskFactors,               // 위험 요소
    List<String> opportunities,             // 기회 요소
    String confidence,                      // 신뢰도 (high, medium, low)
    String language,                        // 응답 언어
    LocalDateTime generatedAt,              // 생성 시간
    String model,                           // 사용된 AI 모델
    Map<String, Object> metadata            // 메타데이터
) {
    
    /**
     * 기본 생성자
     */
    public InsightResponse(String analysisType, String summary, List<String> keyFindings, 
                          List<String> recommendations, String confidence) {
        this(analysisType, summary, keyFindings, recommendations, null, null, null, null, 
             confidence, "ko", LocalDateTime.now(), "gpt-4o-mini", null);
    }
    
    /**
     * 트렌드 분석용 생성자
     */
    public InsightResponse(String analysisType, String summary, List<String> keyFindings, 
                          List<String> recommendations, Map<String, Object> trendAnalysis, 
                          Map<String, Object> statisticalData, String confidence) {
        this(analysisType, summary, keyFindings, recommendations, trendAnalysis, statisticalData, 
             null, null, confidence, "ko", LocalDateTime.now(), "gpt-4o-mini", null);
    }
    
    /**
     * 종합 분석용 생성자
     */
    public InsightResponse(String analysisType, String summary, List<String> keyFindings, 
                          List<String> recommendations, Map<String, Object> trendAnalysis, 
                          Map<String, Object> statisticalData, List<String> riskFactors, 
                          List<String> opportunities, String confidence) {
        this(analysisType, summary, keyFindings, recommendations, trendAnalysis, statisticalData, 
             riskFactors, opportunities, confidence, "ko", LocalDateTime.now(), "gpt-4o-mini", null);
    }
}
