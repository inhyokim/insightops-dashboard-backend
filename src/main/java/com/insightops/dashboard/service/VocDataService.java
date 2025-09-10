package com.insightops.dashboard.service;

import com.insightops.dashboard.client.VoicebotServiceClient;
import com.insightops.dashboard.dto.FilterRequest;
import com.insightops.dashboard.dto.TimeSeriesItem;
import com.insightops.dashboard.dto.TimeSeriesResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
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
     * 시계열 데이터 집계 (필터링 지원)
     */
    public TimeSeriesResponse getTimeSeriesData(FilterRequest filter) {
        return executeWithRetry(() -> {
            // 기간별 날짜 범위 생성
            List<LocalDate> dateRange = generateDateRange(filter.startDate(), filter.endDate(), filter.period());
            
            // 각 날짜별로 데이터 조회 및 집계
            List<TimeSeriesItem> timeSeriesData = new ArrayList<>();
            
            for (LocalDate date : dateRange) {
                Long count = getFilteredCount(date, filter);
                
                TimeSeriesItem item = new TimeSeriesItem(
                    date, 
                    count, 
                    filter.period(),
                    filter.hasCategoryFilter() ? String.join(",", filter.categories()) : null,
                    filter.hasAgeGroupFilter() ? String.join(",", filter.ageGroups()) : null,
                    filter.hasGenderFilter() ? String.join(",", filter.genders()) : null
                );
                
                timeSeriesData.add(item);
            }
            
            // 정렬 적용
            timeSeriesData = applySorting(timeSeriesData, filter);
            
            // 페이지네이션 적용
            List<TimeSeriesItem> pagedData = applyPagination(timeSeriesData, filter);
            
            return new TimeSeriesResponse(pagedData, filter, filter.period());
            
        }, "Time series data", createEmptyTimeSeriesResponse(filter));
    }
    
    /**
     * 카테고리별 시계열 데이터 집계
     */
    public Map<String, TimeSeriesResponse> getCategoryTimeSeriesData(FilterRequest filter) {
        return executeWithRetry(() -> {
            Map<String, TimeSeriesResponse> categoryData = new HashMap<>();
            
            // 카테고리 목록 조회 (필터가 있으면 해당 카테고리만, 없으면 전체)
            List<String> categories = filter.hasCategoryFilter() ? 
                filter.categories() : getAllCategories();
            
            // 각 카테고리별로 시계열 데이터 생성
            for (String category : categories) {
                FilterRequest categoryFilter = createCategoryFilter(filter, category);
                TimeSeriesResponse categoryResponse = getTimeSeriesData(categoryFilter);
                categoryData.put(category, categoryResponse);
            }
            
            return categoryData;
            
        }, "Category time series data", new HashMap<>());
    }
    
    /**
     * 연령대별 시계열 데이터 집계
     */
    public Map<String, TimeSeriesResponse> getAgeGroupTimeSeriesData(FilterRequest filter) {
        return executeWithRetry(() -> {
            Map<String, TimeSeriesResponse> ageGroupData = new HashMap<>();
            
            List<String> ageGroups = filter.hasAgeGroupFilter() ? 
                filter.ageGroups() : Arrays.asList("20", "30", "40", "50", "60");
            
            for (String ageGroup : ageGroups) {
                FilterRequest ageFilter = createAgeGroupFilter(filter, ageGroup);
                TimeSeriesResponse ageResponse = getTimeSeriesData(ageFilter);
                ageGroupData.put(ageGroup, ageResponse);
            }
            
            return ageGroupData;
            
        }, "Age group time series data", new HashMap<>());
    }
    
    /**
     * 성별 시계열 데이터 집계
     */
    public Map<String, TimeSeriesResponse> getGenderTimeSeriesData(FilterRequest filter) {
        return executeWithRetry(() -> {
            Map<String, TimeSeriesResponse> genderData = new HashMap<>();
            
            List<String> genders = filter.hasGenderFilter() ? 
                filter.genders() : Arrays.asList("남자", "여자");
            
            for (String gender : genders) {
                FilterRequest genderFilter = createGenderFilter(filter, gender);
                TimeSeriesResponse genderResponse = getTimeSeriesData(genderFilter);
                genderData.put(gender, genderResponse);
            }
            
            return genderData;
            
        }, "Gender time series data", new HashMap<>());
    }
    
    /**
     * 기간별 날짜 범위 생성
     */
    private List<LocalDate> generateDateRange(LocalDate startDate, LocalDate endDate, String period) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            dates.add(current);
            
            switch (period.toLowerCase()) {
                case "daily" -> current = current.plusDays(1);
                case "weekly" -> current = current.plusWeeks(1);
                case "monthly" -> current = current.plusMonths(1);
                default -> current = current.plusDays(1);
            }
        }
        
        return dates;
    }
    
    /**
     * 필터링된 건수 조회
     */
    private Long getFilteredCount(LocalDate date, FilterRequest filter) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("date", date.toString());
            request.put("period", filter.period());
            
            // 필터 조건 추가
            if (filter.hasCategoryFilter()) {
                request.put("categories", filter.categories());
            }
            if (filter.hasAgeGroupFilter()) {
                request.put("ageGroups", filter.ageGroups());
            }
            if (filter.hasGenderFilter()) {
                request.put("genders", filter.genders());
            }
            
            Map<String, Object> response = voicebotClient.getVocCountSummary(request);
            if (response != null && response.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                Object count = data.get("currentCount");
                return count != null ? Long.valueOf(count.toString()) : 0L;
            }
            return 0L;
        } catch (Exception e) {
            logger.warn("필터링된 건수 조회 실패 ({}): {}", date, e.getMessage());
            return 0L;
        }
    }
    
    /**
     * 정렬 적용
     */
    private List<TimeSeriesItem> applySorting(List<TimeSeriesItem> data, FilterRequest filter) {
        return data.stream()
            .sorted((a, b) -> {
                int comparison = 0;
                
                switch (filter.sortBy().toLowerCase()) {
                    case "date" -> comparison = a.date().compareTo(b.date());
                    case "count" -> comparison = a.count().compareTo(b.count());
                    case "category" -> {
                        String categoryA = a.category() != null ? a.category() : "";
                        String categoryB = b.category() != null ? b.category() : "";
                        comparison = categoryA.compareTo(categoryB);
                    }
                    default -> comparison = a.date().compareTo(b.date());
                }
                
                return "desc".equalsIgnoreCase(filter.sortOrder()) ? -comparison : comparison;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 페이지네이션 적용
     */
    private List<TimeSeriesItem> applyPagination(List<TimeSeriesItem> data, FilterRequest filter) {
        int start = filter.page() * filter.size();
        int end = Math.min(start + filter.size(), data.size());
        
        if (start >= data.size()) {
            return new ArrayList<>();
        }
        
        return data.subList(start, end);
    }
    
    /**
     * 카테고리 필터 생성
     */
    private FilterRequest createCategoryFilter(FilterRequest original, String category) {
        List<String> singleCategory = Arrays.asList(category);
        return new FilterRequest(
            original.startDate(), original.endDate(), original.period(),
            singleCategory, original.ageGroups(), original.genders(),
            original.sortBy(), original.sortOrder(), original.page(), original.size()
        );
    }
    
    /**
     * 연령대 필터 생성
     */
    private FilterRequest createAgeGroupFilter(FilterRequest original, String ageGroup) {
        List<String> singleAgeGroup = Arrays.asList(ageGroup);
        return new FilterRequest(
            original.startDate(), original.endDate(), original.period(),
            original.categories(), singleAgeGroup, original.genders(),
            original.sortBy(), original.sortOrder(), original.page(), original.size()
        );
    }
    
    /**
     * 성별 필터 생성
     */
    private FilterRequest createGenderFilter(FilterRequest original, String gender) {
        List<String> singleGender = Arrays.asList(gender);
        return new FilterRequest(
            original.startDate(), original.endDate(), original.period(),
            original.categories(), original.ageGroups(), singleGender,
            original.sortBy(), original.sortOrder(), original.page(), original.size()
        );
    }
    
    /**
     * 전체 카테고리 목록 조회 (실제로는 Normalization Service에서 조회해야 함)
     */
    private List<String> getAllCategories() {
        // TODO: Normalization Service에서 카테고리 목록 조회
        return Arrays.asList(
            "상품문의", "배송문의", "결제문의", "교환/반품", "기술지원",
            "계정문의", "서비스이용", "불만접수", "제안사항", "기타"
        );
    }
    
    /**
     * 빈 시계열 응답 생성
     */
    private TimeSeriesResponse createEmptyTimeSeriesResponse(FilterRequest filter) {
        return new TimeSeriesResponse(new ArrayList<>(), filter, filter.period());
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