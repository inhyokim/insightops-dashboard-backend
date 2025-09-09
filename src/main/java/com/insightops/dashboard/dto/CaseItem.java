package com.insightops.dashboard.dto;

import java.time.Instant;

/**
 * 상담 사례 데이터 (상세보기용)
 */
public record CaseItem(
    Long id,                    // VoC 이벤트 ID
    Instant receivedAtUtc,      // 접수 시간
    String small,               // Small 카테고리 이름
    String age,                 // 연령대
    String gender,              // 성별
    String summary              // 상담 요약
) {}

