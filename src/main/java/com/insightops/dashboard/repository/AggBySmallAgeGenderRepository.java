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

    interface TopSmallRow {
        String getSmallName();
        Long getCnt();
        Long getTotalCnt();
    }

    interface SmallTrendRow {
        String getSmallName();
        Long getCnt();
    }

    @Query(value = """
        SELECT sc.name as smallName, SUM(agg.count) as cnt, 
               (SELECT SUM(count) FROM agg_by_small_age_gender WHERE bucket_start = :month) as totalCnt
        FROM agg_by_small_age_gender agg
        JOIN dim_small_category sc ON agg.consulting_category_id = sc.small_category_id
        WHERE agg.granularity = 'month' AND agg.bucket_start = :month
        GROUP BY sc.small_category_id, sc.name
        ORDER BY cnt DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<TopSmallRow> findTopSmallOfMonth(@Param("month") LocalDate month);

    @Query(value = """
        SELECT sc.name as smallName, SUM(agg.count) as cnt
        FROM agg_by_small_age_gender agg
        JOIN dim_small_category sc ON agg.consulting_category_id = sc.small_category_id
        WHERE agg.granularity = :granularity 
        AND agg.bucket_start BETWEEN :from AND :to
        AND (:clientAge IS NULL OR agg.client_age = :clientAge)
        AND (:clientGender IS NULL OR agg.client_gender = :clientGender)
        GROUP BY sc.small_category_id, sc.name
        ORDER BY cnt DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<SmallTrendRow> findSmallTrends(@Param("granularity") String granularity,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to,
                                       @Param("clientAge") String clientAge,
                                       @Param("clientGender") String clientGender,
                                       @Param("limit") int limit);
}
