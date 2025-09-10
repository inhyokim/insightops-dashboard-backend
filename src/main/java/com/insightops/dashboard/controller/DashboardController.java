package com.insightops.dashboard.controller;

import com.insightops.dashboard.domain.InsightCard;
import com.insightops.dashboard.domain.MessagePreviewCache;
import com.insightops.dashboard.dto.*;
import com.insightops.dashboard.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 컨트롤러 - 모든 대시보드 API
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;
    
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
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
     * 10. VoC 상세보기 (Normalization Service API 호출)
     * GET /api/dashboard/voc-detail/{vocEventId}
     */
    @GetMapping("/voc-detail/{vocEventId}")
    public ResponseEntity<Map<String, String>> getVocDetail(@PathVariable Long vocEventId) {
        String analysisResult = dashboardService.getVocAnalysisResult(vocEventId);
        return ResponseEntity.ok(Map.of("analysis_result", analysisResult));
    }
}