package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "agg_by_big_category",
       uniqueConstraints = @UniqueConstraint(columnNames = {"granularity", "bucket_start", "big_category_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AggByBigCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "granularity", nullable = false, length = 10)
    private String granularity;

    @Column(name = "bucket_start", nullable = false)
    private LocalDate bucketStart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "big_category_id", nullable = false)
    private DimBigCategory bigCategory;

    @Column(name = "count", nullable = false)
    private Long count;
}

