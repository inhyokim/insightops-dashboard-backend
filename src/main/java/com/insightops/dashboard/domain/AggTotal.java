package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Instant;

/**
 * 전체 VoC 집계 테이블 (매일 자정 업데이트)
 * daily(최근1일), weekly(최근7일), monthly(최근30일) 집계 저장
 */
@Entity
@Table(name = "agg_total", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"period_type", "aggregation_date"}))
public class AggTotal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "period_type", nullable = false)
    private String periodType; // daily, weekly, monthly
    
    @Column(name = "aggregation_date", nullable = false)
    private LocalDate aggregationDate; // 집계 기준일 (매일 업데이트)
    
    @Column(name = "total_count", nullable = false)
    private Long totalCount; // 해당 기간 총 VoC 건수
    
    @Column(name = "prev_count")
    private Long prevCount; // 이전 기간 대비 비교용 건수
    
    @Column(name = "last_updated")
    private Instant lastUpdated; // 마지막 업데이트 시간
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
    
    // Manual getters and setters (Lombok 대신)
    public Long getId() { return id; }
    public String getPeriodType() { return periodType; }
    public LocalDate getAggregationDate() { return aggregationDate; }
    public Long getTotalCount() { return totalCount; }
    public Long getPrevCount() { return prevCount; }
    public Instant getLastUpdated() { return lastUpdated; }
    
    public void setId(Long id) { this.id = id; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public void setAggregationDate(LocalDate aggregationDate) { this.aggregationDate = aggregationDate; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public void setPrevCount(Long prevCount) { this.prevCount = prevCount; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    
    // 기존 호환성을 위한 deprecated 메서드들
    @Deprecated
    public String getGranularity() { return periodType; }
    @Deprecated
    public LocalDate getBucketStart() { return aggregationDate; }
    @Deprecated
    public void setGranularity(String granularity) { this.periodType = granularity; }
    @Deprecated
    public void setBucketStart(LocalDate bucketStart) { this.aggregationDate = bucketStart; }
}
