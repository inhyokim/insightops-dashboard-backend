package com.insightops.dashboard.controller;

import com.insightops.dashboard.domain.InsightCard;
import com.insightops.dashboard.domain.MessagePreviewCache;
import com.insightops.dashboard.dto.*;
import com.insightops.dashboard.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*") // 개발용 - 프론트엔드 분리로 인한 CORS 해결
public class DashboardController {

    private final DashboardService service;
    
    public DashboardController(DashboardService service) {
        this.service = service;
    }

    // 1. 오버뷰 (이달 VoC 건수, 증감률, Top 카테고리)
    @GetMapping("/overview")
    public OverviewDto overview(@RequestParam int year, @RequestParam int month) {
        return service.getOverview(YearMonth.of(year, month));
    }

    // 2. Big 카테고리 비중 (파이차트)
    @GetMapping("/category-share")
    public List<ShareItem> share(
            @RequestParam String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getBigCategoryShare(granularity, from, to);
    }

    // 3. 전체 VoC 변화량 (라인차트)
    @GetMapping("/voc-series")
    public List<SeriesPoint> series(
            @RequestParam String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getTotalSeries(granularity, from, to);
    }

    // 4. Small 카테고리 트렌드 (막대그래프 + 필터)
    @GetMapping("/small-trends")
    public List<SmallTrendItem> smallTrends(
            @RequestParam String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String age,
            @RequestParam(required = false) String gender,
            @RequestParam(defaultValue = "10") int limit) {
        return service.getSmallTrends(granularity, from, to, age, gender, limit);
    }

    // 5. 인사이트 카드 Top 10
    @GetMapping("/insights")
    public List<InsightCard> insights() {
        return service.getInsights();
    }

    // 6. 상담 상세 목록 (외부 정규화 서비스에서 조회)
    @GetMapping("/cases")
    public List<CaseItem> cases(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long smallCategoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getCases(from, to, smallCategoryId, page, size);
    }

    // 7. 메일 프리뷰 최근 목록
    @GetMapping("/messages/recent")
    public List<MessagePreviewCache> recentMessages() {
        return service.getRecentMessages();
    }

    // 8. 메일 발송 (외부 메일 서비스 호출)
    @PostMapping("/mail/send")
    public Map<String, Object> sendMail(
            @RequestParam Long smallCategoryId,
            @RequestParam String assigneeEmail,
            @RequestParam String subject,
            @RequestParam String body) {
        boolean success = service.sendMail(smallCategoryId, assigneeEmail, subject, body);
        return Map.of("success", success);
    }

    // 9. 메일 미리보기 생성 (외부 메일 서비스 호출)
    @GetMapping("/mail/preview")
    public Map<String, String> getMailPreview(
            @RequestParam Long smallCategoryId,
            @RequestParam String assigneeEmail) {
        String preview = service.generateMailPreview(smallCategoryId, assigneeEmail);
        return Map.of("preview", preview);
    }
}