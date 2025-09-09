package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 전체 VoC 집계 테이블
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
}
