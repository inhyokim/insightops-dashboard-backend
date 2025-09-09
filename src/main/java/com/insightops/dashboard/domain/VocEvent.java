package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * VoC 이벤트 원시 데이터
 */
@Entity
@Table(name = "voc_event", 
       indexes = {
           @Index(name = "idx_voc_event_consulting_date", columnList = "consultingDate"),
           @Index(name = "idx_voc_event_consulting_cat", columnList = "consulting_category_id,consultingDate")
       })
@Getter
@Setter
public class VocEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vocEventId;
    
    @Column(nullable = false)
    private String sourceId;
    
    @Column(nullable = false)
    private Instant receivedAtUtc;
    
    // 새로 추가: 상담 일자 (기간 필터링용)
    @Column(nullable = false)
    private LocalDate consultingDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "big_category_id", nullable = false)
    private DimBigCategory bigCategory;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulting_category_id", nullable = false)  // small_category_id -> consulting_category_id
    private DimSmallCategory consultingCategory;
    
    // 수정된 컬럼명
    private String clientAge;     // age_band -> client_age
    private String clientGender;  // gender -> client_gender
}
