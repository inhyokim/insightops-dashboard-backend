package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Instant;

/**
 * 전체 VoC 집계 테이블 (외부 API에서 매일 밤 업데이트)
 * voicebot.voc_raw 데이터를 API로 집계해서 저장
 */
@Entity
@Table(name = "agg_total", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"granularity", "bucket_start"}))
@Getter
@Setter
public class AggTotal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String granularity; // day/week/month
    
    @Column(nullable = false)
    private LocalDate bucketStart;
    
    @Column(nullable = false)
    private Long totalCount;
    
    @Column(name = "last_updated")
    private Instant lastUpdated; // 마지막 업데이트 시간
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}
