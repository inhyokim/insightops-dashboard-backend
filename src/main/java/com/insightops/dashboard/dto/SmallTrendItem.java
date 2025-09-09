package com.insightops.dashboard.dto;

/**
 * Small 카테고리 트렌드 데이터 (막대그래프용)
 */
public record SmallTrendItem(
    String small,     // Small 카테고리 이름
    long count        // 건수
) {}

