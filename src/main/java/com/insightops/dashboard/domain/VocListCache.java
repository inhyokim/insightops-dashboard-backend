package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;

/**
 * VoC 리스트 캐시 테이블
 * voc_normalized DB의 메타데이터를 복사해온 캐시
 */
@Entity
@Table(name = "voc_list_cache",
       indexes = {
           @Index(name = "idx_voc_list_consulting_date", columnList = "consultingDate"),
           @Index(name = "idx_voc_list_category", columnList = "consultingCategory"),
           @Index(name = "idx_voc_list_age_gender", columnList = "clientAge,clientGender")
       })
@NoArgsConstructor
@AllArgsConstructor
public class VocListCache {
    
    @Id
    private String vocId; // voc_normalized의 PK (UUID 등)
    
    @Column(name = "consulting_date", nullable = false)
    private LocalDate consultingDate;
    
    @Column(name = "consulting_category", length = 100)
    private String consultingCategory; // 25개 카테고리 중 하나
    
    @Column(name = "client_age", length = 20)
    private String clientAge; // 연령대 (10대, 20대, 30대 등)
    
    @Column(name = "client_gender", length = 10)
    private String clientGender; // 성별 (남성, 여성, 미상 등)
    
    @Column(name = "source_system", length = 50)
    private String sourceSystem; // 데이터 출처 시스템
    
    @Column(name = "summary_text", length = 500)
    private String summaryText; // 간단한 요약 (리스트용)
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Manual getters (Lombok 대신)
    public String getVocId() { return vocId; }
    public LocalDate getConsultingDate() { return consultingDate; }
    public String getConsultingCategory() { return consultingCategory; }
    public String getClientAge() { return clientAge; }
    public String getClientGender() { return clientGender; }
    public String getSourceSystem() { return sourceSystem; }
    public String getSummaryText() { return summaryText; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    // Manual setters (Lombok 대신)
    public void setVocId(String vocId) { this.vocId = vocId; }
    public void setConsultingDate(LocalDate consultingDate) { this.consultingDate = consultingDate; }
    public void setConsultingCategory(String consultingCategory) { this.consultingCategory = consultingCategory; }
    public void setClientAge(String clientAge) { this.clientAge = clientAge; }
    public void setClientGender(String clientGender) { this.clientGender = clientGender; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
