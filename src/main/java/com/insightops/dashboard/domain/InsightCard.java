package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulting_category_id")  // small_category_id -> consulting_category_id
    private DimSmallCategory consultingCategory;

    @Column(name = "client_age", length = 20)  // age_band -> client_age
    private String clientAge;

    @Column(name = "delta_percent")
    private Double deltaPercent;

    @Column(name = "score")
    private Double score;

    @PrePersist
    protected void onCreate() {
        generatedAt = Instant.now();
    }
}

