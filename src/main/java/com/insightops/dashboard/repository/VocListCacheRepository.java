package com.insightops.dashboard.repository;

import com.insightops.dashboard.domain.VocListCache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VocListCacheRepository extends JpaRepository<VocListCache, String> {
    
    /**
     * VoC 리스트 조회 (필터링 + 페이징)
     */
    @Query("SELECT v FROM VocListCache v WHERE " +
           "(:from IS NULL OR v.consultingDate >= :from) AND " +
           "(:to IS NULL OR v.consultingDate <= :to) AND " +
           "(:category IS NULL OR v.consultingCategory = :category) AND " +
           "(:age IS NULL OR v.clientAge = :age) AND " +
           "(:gender IS NULL OR v.clientGender = :gender) " +
           "ORDER BY v.consultingDate DESC, v.createdAt DESC")
    Page<VocListCache> findVocListWithFilters(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("category") String category,
        @Param("age") String age,
        @Param("gender") String gender,
        Pageable pageable
    );
    
    /**
     * 카테고리별 통계
     */
    @Query("SELECT v.consultingCategory, COUNT(v) FROM VocListCache v " +
           "WHERE (:from IS NULL OR v.consultingDate >= :from) AND " +
           "(:to IS NULL OR v.consultingDate <= :to) " +
           "GROUP BY v.consultingCategory " +
           "ORDER BY COUNT(v) DESC")
    List<Object[]> getCategoryStats(@Param("from") LocalDate from, @Param("to") LocalDate to);
    
    /**
     * 연령대별 통계
     */
    @Query("SELECT v.clientAge, COUNT(v) FROM VocListCache v " +
           "WHERE (:from IS NULL OR v.consultingDate >= :from) AND " +
           "(:to IS NULL OR v.consultingDate <= :to) " +
           "GROUP BY v.clientAge " +
           "ORDER BY COUNT(v) DESC")
    List<Object[]> getAgeStats(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
