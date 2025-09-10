package com.insightops.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 필터링 요청 DTO
 */
public record FilterRequest(
    LocalDate startDate,                    // 시작일
    LocalDate endDate,                      // 종료일
    String period,                          // 기간 단위 (daily, weekly, monthly)
    List<String> categories,                // 카테고리 필터 (null이면 전체)
    List<String> ageGroups,                 // 연령대 필터 (null이면 전체)
    List<String> genders,                   // 성별 필터 (null이면 전체)
    String sortBy,                          // 정렬 기준 (date, count, category)
    String sortOrder,                       // 정렬 순서 (asc, desc)
    Integer page,                           // 페이지 번호 (0부터 시작)
    Integer size                            // 페이지 크기
) {
    
    /**
     * 기본 생성자
     */
    public FilterRequest(LocalDate startDate, LocalDate endDate, String period) {
        this(startDate, endDate, period, null, null, null, "date", "asc", 0, 100);
    }
    
    /**
     * 카테고리 필터링용 생성자
     */
    public FilterRequest(LocalDate startDate, LocalDate endDate, String period, List<String> categories) {
        this(startDate, endDate, period, categories, null, null, "date", "asc", 0, 100);
    }
    
    /**
     * 전체 필터링용 생성자
     */
    public FilterRequest(LocalDate startDate, LocalDate endDate, String period, 
                        List<String> categories, List<String> ageGroups, List<String> genders) {
        this(startDate, endDate, period, categories, ageGroups, genders, "date", "asc", 0, 100);
    }
    
    /**
     * 필터가 적용되었는지 확인
     */
    public boolean hasCategoryFilter() {
        return categories != null && !categories.isEmpty();
    }
    
    public boolean hasAgeGroupFilter() {
        return ageGroups != null && !ageGroups.isEmpty();
    }
    
    public boolean hasGenderFilter() {
        return genders != null && !genders.isEmpty();
    }
    
    public boolean hasAnyFilter() {
        return hasCategoryFilter() || hasAgeGroupFilter() || hasGenderFilter();
    }
}
