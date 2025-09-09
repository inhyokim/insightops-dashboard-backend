package com.insightops.dashboard.dto;

/**
 * 카테고리 비중 데이터 (파이차트용)
 */
public record ShareItem(
    String name,      // 카테고리 이름
    long count,       // 건수
    double ratio      // 비중 (%)
) {}

