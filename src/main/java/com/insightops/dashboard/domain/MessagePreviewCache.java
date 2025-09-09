package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "message_preview_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessagePreviewCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "small_category_id")
    private DimSmallCategory smallCategory;

    @Column(name = "to_email", length = 100)
    private String toEmail;

    @Column(name = "subject", length = 500)
    private String subject;

    @Lob
    @Column(name = "body_md")
    private String bodyMd;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

