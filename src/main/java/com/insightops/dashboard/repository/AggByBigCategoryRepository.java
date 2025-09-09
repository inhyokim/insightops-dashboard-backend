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

    interface ShareRow {
        String getName();
        Long getCnt();
    }

    @Query(value = """
        SELECT bc.name as name, SUM(agg.count) as cnt
        FROM agg_by_big_category agg
        JOIN dim_big_category bc ON agg.big_category_id = bc.big_category_id
        WHERE agg.granularity = :granularity AND agg.bucket_start BETWEEN :from AND :to
        GROUP BY bc.big_category_id, bc.name
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<ShareRow> findShare(@Param("granularity") String granularity,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to);
}
