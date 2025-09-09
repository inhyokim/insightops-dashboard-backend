package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.AggTotal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AggTotalRepository extends JpaRepository<AggTotal, Long> {

    // Projection 인터페이스 - 시계열 데이터용
    interface Point {
        LocalDate getBucketStart();
        Long getTotalCount();
    }

    /**
     * 전체 VoC 변화량 시계열 데이터 조회 (라인차트용)
     */
    @Query(value = """
        SELECT bucket_start as bucketStart, total_count as totalCount
        FROM agg_total
        WHERE granularity = :granularity
          AND bucket_start BETWEEN :from AND :to
        ORDER BY bucket_start
        """, nativeQuery = true)
    List<Point> findSeries(@Param("granularity") String granularity, 
                          @Param("from") LocalDate from, 
                          @Param("to") LocalDate to);

    /**
     * 특정 월의 총 VoC 건수 조회 (오버뷰용)
     */
    @Query(value = """
        SELECT total_count FROM agg_total
        WHERE granularity = 'month' AND bucket_start = :month
        """, nativeQuery = true)
    Optional<Long> findMonthlyTotal(@Param("month") LocalDate month);
}

