package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.MessagePreviewCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagePreviewCacheRepository extends JpaRepository<MessagePreviewCache, Long> {

    /**
     * 최근 메일 프리뷰 50개 조회 (생성일 순)
     */
    List<MessagePreviewCache> findTop50ByOrderByCreatedAtDesc();
}

