package com.insightops.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "assignee_map",
       uniqueConstraints = @UniqueConstraint(columnNames = {"consulting_category_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssigneeMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulting_category_id", nullable = false)  // small_category_id -> consulting_category_id
    private DimSmallCategory consultingCategory;

    @Column(name = "assignee_email", length = 100)
    private String assigneeEmail;

    @Column(name = "assignee_name", length = 50)
    private String assigneeName;

    @Column(name = "assignee_team", length = 100)
    private String assigneeTeam;

    @Column(name = "assignee_phone", length = 20)
    private String assigneePhone;

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
}

