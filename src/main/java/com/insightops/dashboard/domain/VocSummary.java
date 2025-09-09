package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * VoC 요약 데이터
 */
@Entity
@Table(name = "voc_summary")
@Getter
@Setter
public class VocSummary {
    
    @Id
    private Long vocEventId; // 1:1
    
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "voc_event_id")
    private VocEvent vocEvent;
    
    @Lob
    private String summaryText;
    
    private String assignedDepartment;
    
    private Instant createdAt;
    
    // Lombok이 제대로 동작하지 않을 경우를 위한 수동 getter
    public String getSummaryText() {
        return summaryText;
    }
}
