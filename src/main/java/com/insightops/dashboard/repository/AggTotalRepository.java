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

    @Query(value = """
        SELECT bucket_start as bucketStart, total_count as totalCount 
        FROM agg_total 
        WHERE granularity = :granularity AND bucket_start BETWEEN :from AND :to 
        ORDER BY bucket_start
        """, nativeQuery = true)
    List<Point> findSeries(@Param("granularity") String granularity, 
                          @Param("from") LocalDate from, 
                          @Param("to") LocalDate to);

    @Query(value = """
        SELECT total_count 
        FROM agg_total 
        WHERE granularity='month' AND bucket_start = :month
        """, nativeQuery = true)
    Optional<Long> findMonthlyTotal(@Param("month") LocalDate month);
    
    @Query(value = """
        SELECT SUM(total_count) 
        FROM agg_total 
        WHERE granularity='day' AND bucket_start BETWEEN :from AND :to
        """, nativeQuery = true)
    Optional<Long> findTotalCountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
