package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Small 카테고리+연령+성별 집계 테이블
 */
@Entity
@Table(name = "agg_by_small_age_gender",
       indexes = @Index(name = "idx_small_age_gender_bucket", columnList = "granularity,bucket_start"))
@Getter
@Setter  
public class AggBySmallAgeGender {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String granularity;
    
    private LocalDate bucketStart;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulting_category_id", nullable = false)  // small_category_id -> consulting_category_id
    private DimSmallCategory consultingCategory;
    
    private String clientAge; // age_band -> client_age
    
    private String clientGender; // gender -> client_gender
    
    private Long count;
}
