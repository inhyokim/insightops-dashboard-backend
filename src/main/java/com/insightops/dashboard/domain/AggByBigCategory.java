package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Big 카테고리별 집계 테이블
 */
@Entity
@Table(name = "agg_by_big_category",
       uniqueConstraints = @UniqueConstraint(columnNames = {"granularity", "bucket_start", "big_category_id"}))
@Getter
@Setter
public class AggByBigCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String granularity;
    
    private LocalDate bucketStart;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "big_category_id", nullable = false)
    private DimBigCategory bigCategory;
    
    private Long count;
}
