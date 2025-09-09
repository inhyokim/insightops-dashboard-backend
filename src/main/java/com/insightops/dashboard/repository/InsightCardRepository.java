package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.InsightCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsightCardRepository extends JpaRepository<InsightCard, Long> {

    /**
     * 인사이트 카드 Top 10 조회 (점수 순)
     */
    List<InsightCard> findTop10ByOrderByScoreDesc();
}

