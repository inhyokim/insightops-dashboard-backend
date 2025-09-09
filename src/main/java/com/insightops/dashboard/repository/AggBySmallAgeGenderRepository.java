package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.AggBySmallAgeGender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AggBySmallAgeGenderRepository extends JpaRepository<AggBySmallAgeGender, Long> {

    // Projection 인터페이스 - Small 카테고리 트렌드용
    interface SmallTrend {
        String getSmallName();
        Long getCnt();
    }

    // Projection 인터페이스 - Top Small 카테고리 비중용
    interface TopSmallShare {
        String getSmallName();
        Long getCnt();
        Long getTotalCnt();
    }

    /**
     * Small 카테고리 트렌드 분석 (필터: 연령, 성별)
     */
    @Query(value = """
        SELECT sc.name as smallName, SUM(a.count) as cnt
        FROM agg_by_small_age_gender a
        JOIN dim_small_category sc ON sc.small_category_id = a.small_category_id
        WHERE a.granularity = :granularity
          AND a.bucket_start BETWEEN :from AND :to
          AND (:age IS NULL OR a.age_band = :age)
          AND (:gender IS NULL OR a.gender = :gender)
        GROUP BY sc.name
        ORDER BY cnt DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<SmallTrend> findSmallTrends(@Param("granularity") String granularity,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to,
                                    @Param("age") String age,
                                    @Param("gender") String gender,
                                    @Param("limit") int limit);

    /**
     * 이달 가장 많은 문의 Small 카테고리와 비중 조회 (오버뷰용)
     */
    @Query(value = """
        WITH sums AS (
          SELECT sc.name as smallName, SUM(a.count) as cnt
          FROM agg_by_small_age_gender a
          JOIN dim_small_category sc ON sc.small_category_id = a.small_category_id
          WHERE a.granularity = 'month' AND a.bucket_start = :firstDayOfMonth
          GROUP BY sc.name
        ),
        total AS (SELECT SUM(cnt) totalCnt FROM sums)
        SELECT s.smallName, s.cnt, t.totalCnt
        FROM sums s CROSS JOIN total t
        ORDER BY s.cnt DESC LIMIT 1
        """, nativeQuery = true)
    Optional<TopSmallShare> findTopSmallOfMonth(@Param("firstDayOfMonth") LocalDate firstDayOfMonth);
}

