package com.insightops.dashboard.dto;

/**
 * 오버뷰 화면 응답 DTO
 */
public record OverviewDto(
    long thisMonthCount,     // 이달의 VoC 건수
    long prevMonthCount,     // 전달 VoC 건수
    double deltaPercent,     // 전달 대비 증감률 (%)
    String topSmall,         // Top Small 카테고리 이름
    double topSmallShare     // Top Small 카테고리 비중 (%)
) {}

