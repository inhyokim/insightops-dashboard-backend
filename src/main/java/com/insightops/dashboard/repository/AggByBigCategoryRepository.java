package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.AggByBigCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AggByBigCategoryRepository extends JpaRepository<AggByBigCategory, Long> {

    // Projection 인터페이스 - Big 카테고리 비중용
    interface ShareRow {
        String getName();
        Long getCnt();
    }

    /**
     * Big 카테고리별 비중 데이터 조회 (파이차트용)
     */
    @Query(value = """
        SELECT bc.name as name, SUM(a.count) as cnt
        FROM agg_by_big_category a
        JOIN dim_big_category bc ON bc.big_category_id = a.big_category_id
        WHERE a.granularity = :granularity
          AND a.bucket_start BETWEEN :from AND :to
        GROUP BY bc.name
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<ShareRow> findShare(@Param("granularity") String granularity,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to);
}

