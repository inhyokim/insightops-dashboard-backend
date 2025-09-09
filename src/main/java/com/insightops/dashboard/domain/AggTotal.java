package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "agg_total",
       uniqueConstraints = @UniqueConstraint(columnNames = {"granularity", "bucket_start"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AggTotal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "granularity", nullable = false, length = 10)
    private String granularity; // "day", "week", "month"

    @Column(name = "bucket_start", nullable = false)
    private LocalDate bucketStart;

    @Column(name = "total_count", nullable = false)
    private Long totalCount;
}

