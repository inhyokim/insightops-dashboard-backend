package com.insightops.dashboard.controller;

import com.insightops.dashboard.client.NormalizationServiceClient;
import com.insightops.dashboard.domain.InsightCard;
import com.insightops.dashboard.domain.MessagePreviewCache;
import com.insightops.dashboard.dto.*;
import com.insightops.dashboard.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 대시보드 컨트롤러 - 모든 대시보드 API
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    private final DashboardService dashboardService;
    private final NormalizationServiceClient normalizationClient;
    
    public DashboardController(DashboardService dashboardService, NormalizationServiceClient normalizationClient) {
        this.dashboardService = dashboardService;
        this.normalizationClient = normalizationClient;
    }

    /**
     * 1. 오버뷰 (토글: daily/weekly/monthly)
     * GET /api/dashboard/overview?period=daily
     */
    @GetMapping("/overview")
    public ResponseEntity<OverviewDto> getOverview(
            @RequestParam(defaultValue = "daily") String period) {
        OverviewDto overview = dashboardService.getOverview(period);
        return ResponseEntity.ok(overview);
    }
    
    /**
     * 1-1. 오버뷰 (기존 호환성용 - deprecated)
     * GET /api/dashboard/overview-legacy?yearMonth=2024-01
     */
    @Deprecated
    @GetMapping("/overview-legacy")
    public ResponseEntity<OverviewDto> getOverviewLegacy(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        OverviewDto overview = dashboardService.getOverview(yearMonth);
        return ResponseEntity.ok(overview);
    }

    /**
     * 2. Big 카테고리 비중 (파이차트)
     * GET /api/dashboard/big-category-share?granularity=month&from=2024-01-01&to=2024-01-31
     */
    @GetMapping("/big-category-share")
    public ResponseEntity<List<ShareItem>> getBigCategoryShare(
            @RequestParam String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<ShareItem> shareItems = dashboardService.getBigCategoryShare(granularity, from, to);
        return ResponseEntity.ok(shareItems);
    }

    /**
     * 3. 전체 VoC 변화량 (라인차트)
     * GET /api/dashboard/total-series?granularity=month&from=2024-01-01&to=2024-01-31
     */
    @GetMapping("/total-series")
    public ResponseEntity<List<SeriesPoint>> getTotalSeries(
            @RequestParam String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<SeriesPoint> seriesPoints = dashboardService.getTotalSeries(granularity, from, to);
        return ResponseEntity.ok(seriesPoints);
    }

    /**
     * 4. Small 카테고리 트렌드 (막대그래프 + 필터)
     * GET /api/dashboard/small-trends?granularity=month&from=2024-01-01&to=2024-01-31&clientAge=20대&clientGender=여성&limit=10
     */
    @GetMapping("/small-trends")
    public ResponseEntity<List<SmallTrendItem>> getSmallTrends(
            @RequestParam String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String clientAge,
            @RequestParam(required = false) String clientGender,
            @RequestParam(defaultValue = "10") int limit) {
        List<SmallTrendItem> smallTrends = dashboardService.getSmallTrends(granularity, from, to, clientAge, clientGender, limit);
        return ResponseEntity.ok(smallTrends);
    }

    /**
     * 5. 인사이트 카드 Top 10 조회
     * GET /api/dashboard/insights
     */
    @GetMapping("/insights")
    public ResponseEntity<List<InsightCard>> getInsights() {
        List<InsightCard> insights = dashboardService.getInsights();
        return ResponseEntity.ok(insights);
    }

    /**
     * 6. 상담 사례 목록 + 요약 조회 (로컬 데이터)
     * GET /api/dashboard/cases?from=2024-01-01&to=2024-01-31&consultingCategory=1&page=0&size=20
     */
    @GetMapping("/cases")
    public ResponseEntity<List<CaseItem>> getCases(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String consultingCategory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<CaseItem> cases = dashboardService.getCases(from, to, consultingCategory, page, size);
        return ResponseEntity.ok(cases);
    }

    /**
     * 7. 메일 프리뷰 최근 50개 조회
     * GET /api/dashboard/mail/recent
     */
    @GetMapping("/mail/recent")
    public ResponseEntity<List<MessagePreviewCache>> getRecentMessages() {
        List<MessagePreviewCache> messages = dashboardService.getRecentMessages();
        return ResponseEntity.ok(messages);
    }

    /**
     * 8. 메일 초안 생성 (외부 Mail Service 호출)
     * POST /api/dashboard/mail/preview
     */
    @PostMapping("/mail/preview")
    public ResponseEntity<MailPreviewDto> generateMailPreview(@RequestBody MailGenerationRequestDto request) {
        MailPreviewDto preview = dashboardService.generateMailPreview(request.getVocId());
        return ResponseEntity.ok(preview);
    }

    /**
     * 9. 메일 발송 (외부 Mail Service 호출)
     * POST /api/dashboard/mail/send
     */
    @PostMapping("/mail/send")
    public ResponseEntity<Void> sendMail(@RequestBody MailSendRequestDto request) {
        dashboardService.sendMail(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 9-1. 카테고리 기반 메일 생성 (새로운 Mail Contents Service 호출)
     * POST /api/dashboard/mail/generate
     */
    @PostMapping("/mail/generate")
    public ResponseEntity<MailGenerateResponseDto> generateMailByCategory(@RequestBody MailGenerateRequestDto request) {
        MailGenerateResponseDto response = dashboardService.generateMailByCategory(request.categoryId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 10. VoC 상세보기 (Normalization Service API 호출)
     * GET /api/dashboard/voc-detail/{vocEventId}
     */
    @GetMapping("/voc-detail/{vocEventId}")
    public ResponseEntity<Map<String, String>> getVocDetail(@PathVariable Long vocEventId) {
        String analysisResult = dashboardService.getVocAnalysisResult(vocEventId);
        return ResponseEntity.ok(Map.of("analysis_result", analysisResult));
    }
    
    /**
     * 11. Top Small Category 조회
     * GET /api/dashboard/top-small-category?period=daily&baseDate=2025-09-10
     */
    @GetMapping("/top-small-category")
    public ResponseEntity<Map<String, Object>> getTopSmallCategory(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        Map<String, Object> result = dashboardService.getTopSmallCategory(period, baseDate);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 12. Period별 비교 데이터 조회
     * GET /api/dashboard/period-comparison?period=weekly&baseDate=2025-09-10
     */
    @GetMapping("/period-comparison")
    public ResponseEntity<Map<String, Object>> getPeriodComparison(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        Map<String, Object> result = dashboardService.getPeriodComparison(period, baseDate);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 13. 배치 집계 데이터 조회
     * GET /api/dashboard/batch-counts?baseDate=2025-09-10
     */
    @GetMapping("/batch-counts")
    public ResponseEntity<Map<String, Map<String, Object>>> getBatchCounts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        Map<String, Map<String, Object>> result = dashboardService.getBatchCounts(baseDate);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 14. 시계열 데이터 조회 (필터링 지원)
     * GET /api/dashboard/timeseries?startDate=2025-09-01&endDate=2025-09-10&period=daily
     */
    @GetMapping("/timeseries")
    public ResponseEntity<TimeSeriesResponse> getTimeSeriesData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> genders,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer size) {
        
        FilterRequest filter = new FilterRequest(
            startDate, endDate, period, categories, ageGroups, genders,
            sortBy, sortOrder, page, size
        );
        
        TimeSeriesResponse result = dashboardService.getTimeSeriesData(filter);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 15. 카테고리별 시계열 데이터 조회
     * GET /api/dashboard/category-timeseries?startDate=2025-09-01&endDate=2025-09-10&period=weekly
     */
    @GetMapping("/category-timeseries")
    public ResponseEntity<Map<String, TimeSeriesResponse>> getCategoryTimeSeriesData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> genders) {
        
        FilterRequest filter = new FilterRequest(
            startDate, endDate, period, categories, ageGroups, genders
        );
        
        Map<String, TimeSeriesResponse> result = dashboardService.getCategoryTimeSeriesData(filter);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 16. 연령대별 시계열 데이터 조회
     * GET /api/dashboard/age-timeseries?startDate=2025-09-01&endDate=2025-09-10&period=monthly
     */
    @GetMapping("/age-timeseries")
    public ResponseEntity<Map<String, TimeSeriesResponse>> getAgeGroupTimeSeriesData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> genders) {
        
        FilterRequest filter = new FilterRequest(
            startDate, endDate, period, categories, ageGroups, genders
        );
        
        Map<String, TimeSeriesResponse> result = dashboardService.getAgeGroupTimeSeriesData(filter);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 17. 성별 시계열 데이터 조회
     * GET /api/dashboard/gender-timeseries?startDate=2025-09-01&endDate=2025-09-10&period=daily
     */
    @GetMapping("/gender-timeseries")
    public ResponseEntity<Map<String, TimeSeriesResponse>> getGenderTimeSeriesData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> genders) {
        
        FilterRequest filter = new FilterRequest(
            startDate, endDate, period, categories, ageGroups, genders
        );
        
        Map<String, TimeSeriesResponse> result = dashboardService.getGenderTimeSeriesData(filter);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 18. Small Category 트렌드 분석
     * GET /api/dashboard/small-category-trend?startDate=2025-09-01&endDate=2025-09-10&period=weekly
     */
    @GetMapping("/small-category-trend")
    public ResponseEntity<Map<String, Object>> getSmallCategoryTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> genders) {
        
        FilterRequest filter = new FilterRequest(
            startDate, endDate, period, categories, ageGroups, genders
        );
        
        Map<String, Object> result = dashboardService.getSmallCategoryTrend(filter);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 19. 필터링된 VoC 목록 조회 (POST 방식 - 복잡한 필터 지원)
     * POST /api/dashboard/filtered-cases
     */
    @PostMapping("/filtered-cases")
    public ResponseEntity<Map<String, Object>> getFilteredCases(@RequestBody FilterRequest filter) {
        try {
            // Normalization Service에서 필터링된 데이터 조회
            List<CaseItem> cases = normalizationClient.getVocEventsWithSummary(
                filter.startDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                filter.endDate().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                null, filter.page() + 1, filter.size()
            );
            
            // 필터링 적용 (서버 사이드 필터링)
            List<CaseItem> filteredCases = applyFilters(cases, filter);
            
            // 결과 구성
            Map<String, Object> result = new HashMap<>();
            result.put("cases", filteredCases);
            result.put("totalCount", filteredCases.size());
            result.put("filter", filter);
            result.put("page", filter.page());
            result.put("size", filter.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("필터링된 VoC 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "데이터 조회 실패"));
        }
    }
    
    /**
     * 필터링 적용 (서버 사이드)
     */
    private List<CaseItem> applyFilters(List<CaseItem> cases, FilterRequest filter) {
        return cases.stream()
            .filter(caseItem -> {
                // 카테고리 필터
                if (filter.hasCategoryFilter() && 
                    !filter.categories().contains(caseItem.consultingCategoryName())) {
                    return false;
                }
                
                // 연령대 필터
                if (filter.hasAgeGroupFilter() && 
                    !filter.ageGroups().contains(caseItem.clientAge())) {
                    return false;
                }
                
                // 성별 필터
                if (filter.hasGenderFilter() && 
                    !filter.genders().contains(caseItem.clientGender())) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
}