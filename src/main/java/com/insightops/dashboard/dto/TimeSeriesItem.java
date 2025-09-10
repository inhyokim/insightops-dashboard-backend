package com.insightops.dashboard.dto;

import java.time.LocalDate;

/**
 * 시계열 데이터 아이템
 */
public record TimeSeriesItem(
    LocalDate date,           // 날짜
    Long count,              // 건수
    String period,           // 기간 (daily, weekly, monthly)
    String category,         // 카테고리 (선택적)
    String ageGroup,         // 연령대 (선택적)
    String gender            // 성별 (선택적)
) {
    
    /**
     * 기본 생성자 (전체 집계용)
     */
    public TimeSeriesItem(LocalDate date, Long count, String period) {
        this(date, count, period, null, null, null);
    }
    
    /**
     * 카테고리별 집계용 생성자
     */
    public TimeSeriesItem(LocalDate date, Long count, String period, String category) {
        this(date, count, period, category, null, null);
    }
}
