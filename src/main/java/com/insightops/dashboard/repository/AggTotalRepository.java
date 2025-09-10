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

    interface Point {
        LocalDate getBucketStart();
        Long getTotalCount();
    }
    
    interface OverviewData {
        Long getTotalCount();
        Long getPrevCount();
        Double getDeltaPercent();
    }

    // === 새로운 스키마용 메서드들 ===
    
    /**
     * 특정 기간 타입의 최신 집계 데이터 조회 (오버뷰용)
     */
    @Query(value = """
        SELECT 
            total_count as totalCount,
            prev_count as prevCount,
            CASE 
                WHEN prev_count = 0 THEN 0.0
                ELSE ((total_count - prev_count) * 100.0 / prev_count)
            END as deltaPercent
        FROM agg_total 
        WHERE period_type = :periodType 
        ORDER BY aggregation_date DESC 
        LIMIT 1
        """, nativeQuery = true)
    Optional<OverviewData> findLatestByPeriodType(@Param("periodType") String periodType);
    
    /**
     * 특정 날짜의 특정 기간 타입 데이터 조회
     */
    Optional<AggTotal> findByPeriodTypeAndAggregationDate(String periodType, LocalDate aggregationDate);
    
    // === 기존 호환성을 위한 deprecated 메서드들 ===
    
    @Deprecated
    @Query(value = """
        SELECT aggregation_date as bucketStart, total_count as totalCount 
        FROM agg_total 
        WHERE period_type = :granularity AND aggregation_date BETWEEN :from AND :to 
        ORDER BY aggregation_date
        """, nativeQuery = true)
    List<Point> findSeries(@Param("granularity") String granularity, 
                          @Param("from") LocalDate from, 
                          @Param("to") LocalDate to);

    @Deprecated
    @Query(value = """
        SELECT total_count 
        FROM agg_total 
        WHERE period_type='monthly' AND aggregation_date = :month
        """, nativeQuery = true)
    Optional<Long> findMonthlyTotal(@Param("month") LocalDate month);
    
    @Deprecated
    @Query(value = """
        SELECT total_count 
        FROM agg_total 
        WHERE period_type='daily' AND aggregation_date BETWEEN :from AND :to
        ORDER BY aggregation_date DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Long> findTotalCountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
