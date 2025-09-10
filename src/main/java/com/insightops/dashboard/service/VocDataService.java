package com.insightops.dashboard.service;

import com.insightops.dashboard.client.VoicebotServiceClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Voicebot Service API를 통해 VoC 데이터를 조회하는 서비스
 */
@Service
public class VocDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(VocDataService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final VoicebotServiceClient voicebotClient;

    public VocDataService(VoicebotServiceClient voicebotClient) {
        this.voicebotClient = voicebotClient;
    }

    /**
     * Daily 집계 데이터 (최근 1일) - API 호출 with Retry
     */
    public Long getDailyVocCount(LocalDate date) {
        return executeWithRetry(() -> {
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
        }, "Daily VoC count", 0L);
    }
    
    /**
     * Weekly 집계 데이터 (최근 7일) - API 호출 with Retry
     */
    public Long getWeeklyVocCount(LocalDate endDate) {
        return executeWithRetry(() -> {
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
        }, "Weekly VoC count", 0L);
    }
    
    /**
     * Monthly 집계 데이터 (최근 30일) - API 호출 with Retry
     */
    public Long getMonthlyVocCount(LocalDate endDate) {
        return executeWithRetry(() -> {
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
        }, "Monthly VoC count", 0L);
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
     * 고급 집계: Period별 비교 데이터 조회
     */
    public Map<String, Object> getPeriodComparison(String period, LocalDate baseDate) {
        return executeWithRetry(() -> {
            Map<String, Object> request = new HashMap<>();
            request.put("period", period);
            request.put("baseDate", baseDate.toString());
            
            Map<String, Object> response = voicebotClient.getVocCountSummary(request);
            if (response != null && response.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                
                // 추가 분석 데이터 계산
                Long currentCount = Long.valueOf(data.get("currentCount").toString());
                Long previousCount = Long.valueOf(data.get("previousCount").toString());
                
                // 변화율 계산
                double deltaPercent = previousCount > 0 ? 
                    ((currentCount - previousCount) * 100.0 / previousCount) : 0.0;
                
                // 트렌드 분석
                String trend = calculateTrend(currentCount, previousCount);
                
                data.put("deltaPercent", deltaPercent);
                data.put("trend", trend);
                data.put("analysis", generateTrendAnalysis(currentCount, previousCount, deltaPercent));
                
                return data;
            }
            return new HashMap<>();
        }, "Period comparison", new HashMap<>());
    }
    
    /**
     * 배치 집계: 여러 기간의 데이터를 한번에 조회
     */
    public Map<String, Map<String, Object>> getBatchPeriodCounts(LocalDate baseDate) {
        Map<String, Map<String, Object>> results = new HashMap<>();
        
        // 병렬로 모든 기간 조회
        CompletableFuture<Map<String, Object>> dailyFuture = CompletableFuture.supplyAsync(() -> 
            getPeriodComparison("daily", baseDate));
        CompletableFuture<Map<String, Object>> weeklyFuture = CompletableFuture.supplyAsync(() -> 
            getPeriodComparison("weekly", baseDate));
        CompletableFuture<Map<String, Object>> monthlyFuture = CompletableFuture.supplyAsync(() -> 
            getPeriodComparison("monthly", baseDate));
        
        try {
            results.put("daily", dailyFuture.get(5, TimeUnit.SECONDS));
            results.put("weekly", weeklyFuture.get(5, TimeUnit.SECONDS));
            results.put("monthly", monthlyFuture.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            logger.error("배치 집계 조회 실패: {}", e.getMessage());
            // 실패한 경우 개별 조회로 Fallback
            results.put("daily", getPeriodComparison("daily", baseDate));
            results.put("weekly", getPeriodComparison("weekly", baseDate));
            results.put("monthly", getPeriodComparison("monthly", baseDate));
        }
        
        return results;
    }
    
    /**
     * 트렌드 계산
     */
    private String calculateTrend(Long current, Long previous) {
        if (previous == 0) return "NEW";
        
        double change = ((current - previous) * 100.0 / previous);
        
        if (change > 20) return "SHARP_INCREASE";
        if (change > 5) return "INCREASE";
        if (change > -5) return "STABLE";
        if (change > -20) return "DECREASE";
        return "SHARP_DECREASE";
    }
    
    /**
     * 트렌드 분석 텍스트 생성
     */
    private String generateTrendAnalysis(Long current, Long previous, double deltaPercent) {
        if (previous == 0) {
            return "새로운 데이터입니다.";
        }
        
        String direction = deltaPercent > 0 ? "증가" : "감소";
        String magnitude = Math.abs(deltaPercent) > 20 ? "급격한" : 
                          Math.abs(deltaPercent) > 5 ? "상당한" : "소폭";
        
        return String.format("이전 기간 대비 %s %s (%.1f%%)", magnitude, direction, deltaPercent);
    }
    
    /**
     * Retry 로직이 포함된 실행 메서드
     */
    private <T> T executeWithRetry(java.util.function.Supplier<T> operation, String operationName, T defaultValue) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                T result = operation.get();
                if (attempt > 1) {
                    logger.info("{} 성공 (시도 {}회)", operationName, attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                logger.warn("{} 실패 (시도 {}/{}): {}", operationName, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        logger.error("{} 최종 실패, 기본값 반환: {}", operationName, lastException != null ? lastException.getMessage() : "Unknown error");
        return defaultValue;
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