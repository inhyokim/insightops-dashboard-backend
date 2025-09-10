package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "insight_card")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsightCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insight_id")
    private Long insightId;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "title", length = 200)
    private String title;

    @Lob
    @Column(name = "body")
    private String body;

    // 직접 문자열로 저장 (외부 카테고리 참조 제거)
    @Column(name = "consulting_category", length = 100)
    private String consultingCategory; // 25개 카테고리 중 하나

    @Column(name = "client_age", length = 20)
    private String clientAge;

    @Column(name = "delta_percent")
    private Double deltaPercent; // 변동률 (%)

    @Column(name = "score")
    private Double score; // AI 신뢰도 점수 (0-100)

    @Column(name = "insight_type", length = 50)
    private String insightType; // "급증", "급감", "신규트렌드", "계절성" 등

    @Column(name = "period_start")
    private LocalDate periodStart; // 분석 기간 시작

    @Column(name = "period_end")
    private LocalDate periodEnd; // 분석 기간 끝

    @Column(name = "previous_count")
    private Long previousCount; // 이전 기간 건수

    @Column(name = "current_count")  
    private Long currentCount; // 현재 기간 건수

    @PrePersist
    protected void onCreate() {
        generatedAt = Instant.now();
    }
}

