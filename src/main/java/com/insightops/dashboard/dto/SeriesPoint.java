package com.insightops.dashboard.dto;

import java.time.LocalDate;

/**
 * 시계열 데이터 포인트 (라인차트용)
 */
public record SeriesPoint(
    LocalDate x,    // X축 (날짜)
    long y          // Y축 (값)
) {}

