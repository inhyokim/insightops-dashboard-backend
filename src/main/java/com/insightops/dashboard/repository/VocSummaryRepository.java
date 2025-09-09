package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.VocSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VocSummaryRepository extends JpaRepository<VocSummary, Long> {

    @Query("SELECT vs FROM VocSummary vs WHERE vs.vocEventId = :vocEventId")
    Optional<VocSummary> findByVocEventId(@Param("vocEventId") Long vocEventId);

    @Query("SELECT vs FROM VocSummary vs ORDER BY vs.createdAt DESC")
    List<VocSummary> findTop50ByOrderByCreatedAtDesc();
}
