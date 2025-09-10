package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Instant;

/**
 * 카테고리+연령+성별 집계 테이블 (트렌드 분석용)
 * 외부 API에서 매일 밤 25개 카테고리별로 집계 업데이트
 */
@Entity
@Table(name = "agg_by_category_age_gender",
       indexes = {
           @Index(name = "idx_cat_age_gender_bucket", columnList = "granularity,bucketStart"),
           @Index(name = "idx_cat_age_gender_filter", columnList = "consultingCategory,clientAge,clientGender")
       })
@Getter
@Setter  
public class AggByCategoryAgeGender {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String granularity; // day/week/month
    
    @Column(nullable = false)
    private LocalDate bucketStart;
    
    // 직접 문자열로 저장 (25개 카테고리)
    @Column(name = "consulting_category", length = 100, nullable = false)
    private String consultingCategory;
    
    @Column(name = "client_age", length = 20)
    private String clientAge; // 연령대 필터용
    
    @Column(name = "client_gender", length = 10) 
    private String clientGender; // 성별 필터용
    
    @Column(nullable = false)
    private Long count;
    
    @Column(name = "last_updated")
    private Instant lastUpdated;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
}
