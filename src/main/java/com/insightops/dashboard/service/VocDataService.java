package com.insightops.dashboard.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Voicebot DB의 voc_raw 테이블에서 데이터를 조회하는 서비스
 */
@Service
public class VocDataService {

    private final JdbcTemplate voicebotJdbcTemplate;

    public VocDataService(@Qualifier("voicebotJdbcTemplate") JdbcTemplate voicebotJdbcTemplate) {
        this.voicebotJdbcTemplate = voicebotJdbcTemplate;
    }

    /**
     * 전체 VoC 건수 조회 (특정 날짜 범위) - consulting_date 기준
     */
    public Long getTotalVocCount(LocalDate from, LocalDate to) {
        String sql = """
            SELECT COUNT(*) 
            FROM voc_raw 
            WHERE consulting_date BETWEEN ? AND ?
            """;
        
        return voicebotJdbcTemplate.queryForObject(sql, Long.class, from, to);
    }
    
    /**
     * 최근 N일 VoC 건수 조회 (특정 기준일로부터)
     */
    public Long getRecentVocCount(LocalDate baseDate, int days) {
        LocalDate fromDate = baseDate.minusDays(days - 1);
        return getTotalVocCount(fromDate, baseDate);
    }
    
    /**
     * Daily 집계 데이터 (최근 1일)
     */
    public Long getDailyVocCount(LocalDate date) {
        return getTotalVocCount(date, date);
    }
    
    /**
     * Weekly 집계 데이터 (최근 7일)
     */
    public Long getWeeklyVocCount(LocalDate endDate) {
        return getRecentVocCount(endDate, 7);
    }
    
    /**
     * Monthly 집계 데이터 (최근 30일)
     */
    public Long getMonthlyVocCount(LocalDate endDate) {
        return getRecentVocCount(endDate, 30);
    }

    /**
     * 일별 VoC 건수 조회 - consulting_date 기준
     */
    public List<Map<String, Object>> getDailyVocCounts(LocalDate from, LocalDate to) {
        String sql = """
            SELECT 
                consulting_date as bucket_date,
                COUNT(*) as total_count
            FROM voc_raw 
            WHERE consulting_date BETWEEN ? AND ?
            GROUP BY consulting_date
            ORDER BY bucket_date
            """;
        
        return voicebotJdbcTemplate.queryForList(sql, from, to);
    }

    /**
     * 주별 VoC 건수 조회 (월요일 기준) - consulting_date 기준
     */
    public List<Map<String, Object>> getWeeklyVocCounts(LocalDate from, LocalDate to) {
        String sql = """
            SELECT 
                DATE(DATE_SUB(consulting_date, INTERVAL WEEKDAY(consulting_date) DAY)) as bucket_date,
                COUNT(*) as total_count
            FROM voc_raw 
            WHERE consulting_date BETWEEN ? AND ?
            GROUP BY DATE(DATE_SUB(consulting_date, INTERVAL WEEKDAY(consulting_date) DAY))
            ORDER BY bucket_date
            """;
        
        return voicebotJdbcTemplate.queryForList(sql, from, to);
    }

    /**
     * 월별 VoC 건수 조회 - consulting_date 기준
     */
    public List<Map<String, Object>> getMonthlyVocCounts(LocalDate from, LocalDate to) {
        String sql = """
            SELECT 
                DATE(DATE_FORMAT(consulting_date, '%Y-%m-01')) as bucket_date,
                COUNT(*) as total_count
            FROM voc_raw 
            WHERE consulting_date BETWEEN ? AND ?
            GROUP BY DATE(DATE_FORMAT(consulting_date, '%Y-%m-01'))
            ORDER BY bucket_date
            """;
        
        return voicebotJdbcTemplate.queryForList(sql, from, to);
    }

    /**
     * 카테고리별 집계 조회 - consulting_date 기준
     */
    public List<Map<String, Object>> getCategoryAggregation(String granularity, LocalDate from, LocalDate to) {
        String datePart = switch (granularity.toLowerCase()) {
            case "week" -> "DATE(DATE_SUB(consulting_date, INTERVAL WEEKDAY(consulting_date) DAY))";
            case "month" -> "DATE(DATE_FORMAT(consulting_date, '%Y-%m-01'))";
            default -> "consulting_date"; // day
        };

        String sql = String.format("""
            SELECT 
                %s as bucket_date,
                consulting_category,
                client_age,
                client_gender,
                COUNT(*) as count
            FROM voc_raw 
            WHERE consulting_date BETWEEN ? AND ?
            GROUP BY %s, consulting_category, client_age, client_gender
            ORDER BY bucket_date, consulting_category
            """, datePart, datePart);
        
        return voicebotJdbcTemplate.queryForList(sql, from, to);
    }

    /**
     * Top 카테고리 조회 (월별) - consulting_date 기준
     */
    public Map<String, Object> getTopCategoryOfMonth(LocalDate monthStart, LocalDate monthEnd) {
        String sql = """
            SELECT 
                consulting_category,
                COUNT(*) as count,
                (SELECT COUNT(*) FROM voc_raw WHERE consulting_date BETWEEN ? AND ?) as total_count
            FROM voc_raw 
            WHERE consulting_date BETWEEN ? AND ?
            GROUP BY consulting_category
            ORDER BY count DESC
            LIMIT 1
            """;
        
        List<Map<String, Object>> results = voicebotJdbcTemplate.queryForList(sql, 
            monthStart, monthEnd, monthStart, monthEnd);
        
        return results.isEmpty() ? Map.of() : results.get(0);
    }

    /**
     * VoC 리스트 캐시용 데이터 조회 - consulting_date 기준
     */
    public List<Map<String, Object>> getVocListForCache(LocalDate from, LocalDate to, int limit) {
        String sql = """
            SELECT 
                id as voc_id,
                consulting_date,
                consulting_category,
                client_age,
                client_gender,
                'voicebot' as source_system,
                SUBSTRING(content, 1, 200) as summary_text,
                created_at,
                updated_at
            FROM voc_raw 
            WHERE consulting_date BETWEEN ? AND ?
            ORDER BY consulting_date DESC
            LIMIT ?
            """;
        
        return voicebotJdbcTemplate.queryForList(sql, from, to, limit);
    }

    /**
     * 데이터베이스 연결 테스트
     */
    public boolean testConnection() {
        try {
            voicebotJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            System.err.println("Voicebot DB 연결 실패: " + e.getMessage());
            return false;
        }
    }
}
