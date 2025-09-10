package com.insightops.dashboard.dto;

import java.util.List;
import java.util.Map;

/**
 * 시계열 데이터 응답 DTO
 */
public record TimeSeriesResponse(
    List<TimeSeriesItem> data,              // 시계열 데이터
    FilterRequest filter,                   // 적용된 필터
    Long totalCount,                        // 전체 건수
    Integer totalPages,                     // 전체 페이지 수
    Integer currentPage,                    // 현재 페이지
    Integer pageSize,                       // 페이지 크기
    Map<String, Object> summary,            // 요약 정보
    String period                           // 기간 단위
) {
    
    /**
     * 기본 생성자
     */
    public TimeSeriesResponse(List<TimeSeriesItem> data, FilterRequest filter, String period) {
        this(data, filter, 
             data.stream().mapToLong(TimeSeriesItem::count).sum(),
             1, 0, data.size(),
             Map.of("totalItems", data.size()),
             period);
    }
    
    /**
     * 페이지네이션 포함 생성자
     */
    public TimeSeriesResponse(List<TimeSeriesItem> data, FilterRequest filter, 
                             Long totalCount, Integer totalPages, Integer currentPage, 
                             Integer pageSize, String period) {
        this(data, filter, totalCount, totalPages, currentPage, pageSize,
             Map.of("totalItems", data.size(), "hasNext", currentPage < totalPages - 1),
             period);
    }
}
