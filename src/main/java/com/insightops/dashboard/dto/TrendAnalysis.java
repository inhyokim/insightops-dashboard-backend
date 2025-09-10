package com.insightops.dashboard.dto;

import java.util.List;
import java.util.Map;

/**
 * 트렌드 분석 결과 DTO
 */
public record TrendAnalysis(
    String trendDirection,                  // 트렌드 방향 (increasing, decreasing, stable, volatile)
    String trendStrength,                   // 트렌드 강도 (strong, moderate, weak)
    Double changePercentage,                // 변화율 (%)
    String period,                          // 분석 기간
    List<String> significantEvents,         // 중요한 이벤트들
    Map<String, Object> seasonalPatterns,   // 계절성 패턴
    Map<String, Object> correlationData,    // 상관관계 데이터
    List<String> anomalies,                 // 이상치/이상 패턴
    String prediction,                      // 예측 결과
    String confidence,                      // 예측 신뢰도
    Map<String, Object> factors             // 영향 요인들
) {
    
    /**
     * 기본 생성자
     */
    public TrendAnalysis(String trendDirection, String trendStrength, Double changePercentage, String period) {
        this(trendDirection, trendStrength, changePercentage, period, null, null, null, null, null, null, null);
    }
    
    /**
     * 상세 분석용 생성자
     */
    public TrendAnalysis(String trendDirection, String trendStrength, Double changePercentage, String period,
                        List<String> significantEvents, Map<String, Object> seasonalPatterns, 
                        Map<String, Object> correlationData, List<String> anomalies, String prediction, 
                        String confidence, Map<String, Object> factors) {
        this.trendDirection = trendDirection;
        this.trendStrength = trendStrength;
        this.changePercentage = changePercentage;
        this.period = period;
        this.significantEvents = significantEvents;
        this.seasonalPatterns = seasonalPatterns;
        this.correlationData = correlationData;
        this.anomalies = anomalies;
        this.prediction = prediction;
        this.confidence = confidence;
        this.factors = factors;
    }
}
