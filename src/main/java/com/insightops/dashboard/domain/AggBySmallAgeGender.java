package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "agg_by_small_age_gender",
       indexes = @Index(name = "idx_small_age_gender_bucket", columnList = "granularity,bucket_start"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AggBySmallAgeGender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "granularity", nullable = false, length = 10)
    private String granularity;

    @Column(name = "bucket_start", nullable = false)
    private LocalDate bucketStart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "small_category_id", nullable = false)
    private DimSmallCategory smallCategory;

    @Column(name = "age_band", length = 20)
    private String ageBand;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "count", nullable = false)
    private Long count;
}

