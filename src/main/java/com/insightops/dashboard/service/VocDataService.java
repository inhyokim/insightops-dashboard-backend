package com.insightops.dashboard.service;

import com.insightops.dashboard.client.VoicebotServiceClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Voicebot Service API를 통해 VoC 데이터를 조회하는 서비스
 */
@Service
public class VocDataService {

    private final VoicebotServiceClient voicebotClient;

    public VocDataService(VoicebotServiceClient voicebotClient) {
        this.voicebotClient = voicebotClient;
    }

    /**
     * Daily 집계 데이터 (최근 1일) - API 호출
     */
    public Long getDailyVocCount(LocalDate date) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("period", "daily");
            request.put("baseDate", date.toString());
            
            Map<String, Object> response = voicebotClient.getVocCountSummary(request);
            if (response != null && response.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                Object count = data.get("currentCount");
                return count != null ? Long.valueOf(count.toString()) : 0L;
            }
            return 0L;
        } catch (Exception e) {
            System.err.println("Daily VoC count API 호출 실패: " + e.getMessage());
            return 0L; // 실패 시 0 반환
        }
    }
    
    /**
     * Weekly 집계 데이터 (최근 7일) - API 호출
     */
    public Long getWeeklyVocCount(LocalDate endDate) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("period", "weekly");
            request.put("baseDate", endDate.toString());
            
            Map<String, Object> response = voicebotClient.getVocCountSummary(request);
            if (response != null && response.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                Object count = data.get("currentCount");
                return count != null ? Long.valueOf(count.toString()) : 0L;
            }
            return 0L;
        } catch (Exception e) {
            System.err.println("Weekly VoC count API 호출 실패: " + e.getMessage());
            return 0L;
        }
    }
    
    /**
     * Monthly 집계 데이터 (최근 30일) - API 호출
     */
    public Long getMonthlyVocCount(LocalDate endDate) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("period", "monthly");
            request.put("baseDate", endDate.toString());
            
            Map<String, Object> response = voicebotClient.getVocCountSummary(request);
            if (response != null && response.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                Object count = data.get("currentCount");
                return count != null ? Long.valueOf(count.toString()) : 0L;
            }
            return 0L;
        } catch (Exception e) {
            System.err.println("Monthly VoC count API 호출 실패: " + e.getMessage());
            return 0L;
        }
    }

    /**
     * Period별 VoC 건수 조회 (API 호출)
     */
    public Map<String, Object> getPeriodCountSummary(String period, LocalDate baseDate) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("period", period);
            request.put("baseDate", baseDate.toString());
            
            return voicebotClient.getVocCountSummary(request);
        } catch (Exception e) {
            System.err.println(period + " VoC count summary API 호출 실패: " + e.getMessage());
            
            // 실패 시 기본값 반환
            Map<String, Object> fallback = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("period", period);
            data.put("baseDate", baseDate.toString());
            data.put("currentCount", 0L);
            data.put("previousCount", 0L);
            data.put("deltaPercent", 0.0);
            fallback.put("success", false);
            fallback.put("data", data);
            return fallback;
        }
    }

    /**
     * 데이터베이스 연결 테스트 (더 이상 사용하지 않음)
     */
    public boolean testConnection() {
        try {
            // Voicebot 서비스 health check API 호출
            Map<String, Object> response = voicebotClient.healthCheck();
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            System.err.println("Voicebot 서비스 연결 실패: " + e.getMessage());
            return false;
        }
    }
}