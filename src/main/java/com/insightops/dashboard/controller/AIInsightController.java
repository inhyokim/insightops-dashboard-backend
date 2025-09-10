package com.insightops.dashboard.controller;

import com.insightops.dashboard.dto.InsightRequest;
import com.insightops.dashboard.dto.InsightResponse;
import com.insightops.dashboard.service.AIInsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AI 인사이트 컨트롤러 - GPT 4o mini를 활용한 고급 트렌드 분석
 */
@RestController
@RequestMapping("/api/ai-insights")
@CrossOrigin(origins = "*")
public class AIInsightController {
    
    private static final Logger logger = LoggerFactory.getLogger(AIInsightController.class);
    
    private final AIInsightService aiInsightService;
    
    public AIInsightController(AIInsightService aiInsightService) {
        this.aiInsightService = aiInsightService;
    }
    
    /**
     * 1. 종합 AI 인사이트 생성
     * POST /api/ai-insights/comprehensive
     */
    @PostMapping("/comprehensive")
    public ResponseEntity<InsightResponse> generateComprehensiveInsights(@RequestBody InsightRequest request) {
        try {
            logger.info("종합 AI 인사이트 생성 요청: {}", request.analysisType());
            InsightResponse response = aiInsightService.generateComprehensiveInsights(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("종합 AI 인사이트 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("종합 인사이트 생성 실패"));
        }
    }
    
    /**
     * 2. 트렌드 분석 AI 인사이트
     * GET /api/ai-insights/trend?startDate=2025-09-01&endDate=2025-09-10&period=weekly
     */
    @GetMapping("/trend")
    public ResponseEntity<InsightResponse> generateTrendInsights(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> genders,
            @RequestParam(defaultValue = "ko") String language,
            @RequestParam(defaultValue = "detailed") String detailLevel) {
        
        try {
            InsightRequest request = new InsightRequest(
                "trend", startDate, endDate, period, categories, ageGroups, genders, 
                null, null, language, detailLevel, null
            );
            
            logger.info("트렌드 AI 인사이트 생성 요청: {} ~ {}", startDate, endDate);
            InsightResponse response = aiInsightService.generateTrendInsights(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("트렌드 AI 인사이트 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("트렌드 인사이트 생성 실패"));
        }
    }
    
    /**
     * 3. 카테고리별 AI 인사이트
     * GET /api/ai-insights/category?startDate=2025-09-01&endDate=2025-09-10&period=monthly
     */
    @GetMapping("/category")
    public ResponseEntity<InsightResponse> generateCategoryInsights(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> genders,
            @RequestParam(defaultValue = "ko") String language,
            @RequestParam(defaultValue = "detailed") String detailLevel) {
        
        try {
            InsightRequest request = new InsightRequest(
                "category", startDate, endDate, period, categories, ageGroups, genders, 
                null, null, language, detailLevel, null
            );
            
            logger.info("카테고리 AI 인사이트 생성 요청: {} ~ {}", startDate, endDate);
            InsightResponse response = aiInsightService.generateCategoryInsights(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("카테고리 AI 인사이트 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("카테고리 인사이트 생성 실패"));
        }
    }
    
    /**
     * 4. 연령대별 AI 인사이트
     * GET /api/ai-insights/age-group?startDate=2025-09-01&endDate=2025-09-10&period=weekly
     */
    @GetMapping("/age-group")
    public ResponseEntity<InsightResponse> generateAgeGroupInsights(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> genders,
            @RequestParam(defaultValue = "ko") String language,
            @RequestParam(defaultValue = "detailed") String detailLevel) {
        
        try {
            InsightRequest request = new InsightRequest(
                "age", startDate, endDate, period, categories, ageGroups, genders, 
                null, null, language, detailLevel, null
            );
            
            logger.info("연령대 AI 인사이트 생성 요청: {} ~ {}", startDate, endDate);
            InsightResponse response = aiInsightService.generateCategoryInsights(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("연령대 AI 인사이트 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("연령대 인사이트 생성 실패"));
        }
    }
    
    /**
     * 5. 성별 AI 인사이트
     * GET /api/ai-insights/gender?startDate=2025-09-01&endDate=2025-09-10&period=monthly
     */
    @GetMapping("/gender")
    public ResponseEntity<InsightResponse> generateGenderInsights(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> ageGroups,
            @RequestParam(required = false) List<String> genders,
            @RequestParam(defaultValue = "ko") String language,
            @RequestParam(defaultValue = "detailed") String detailLevel) {
        
        try {
            InsightRequest request = new InsightRequest(
                "gender", startDate, endDate, period, categories, ageGroups, genders, 
                null, null, language, detailLevel, null
            );
            
            logger.info("성별 AI 인사이트 생성 요청: {} ~ {}", startDate, endDate);
            InsightResponse response = aiInsightService.generateCategoryInsights(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("성별 AI 인사이트 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("성별 인사이트 생성 실패"));
        }
    }
    
    /**
     * 6. 커스텀 AI 인사이트 (POST 방식)
     * POST /api/ai-insights/custom
     */
    @PostMapping("/custom")
    public ResponseEntity<InsightResponse> generateCustomInsights(@RequestBody InsightRequest request) {
        try {
            logger.info("커스텀 AI 인사이트 생성 요청: {}", request.analysisType());
            
            InsightResponse response;
            switch (request.analysisType().toLowerCase()) {
                case "trend" -> response = aiInsightService.generateTrendInsights(request);
                case "category" -> response = aiInsightService.generateCategoryInsights(request);
                default -> response = aiInsightService.generateComprehensiveInsights(request);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("커스텀 AI 인사이트 생성 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("커스텀 인사이트 생성 실패"));
        }
    }
    
    /**
     * 7. AI 인사이트 히스토리 조회 (향후 구현)
     * GET /api/ai-insights/history
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getInsightHistory(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        // TODO: AI 인사이트 히스토리 조회 구현
        Map<String, Object> response = Map.of(
            "message", "AI 인사이트 히스토리 기능은 향후 구현 예정입니다.",
            "page", page,
            "size", size,
            "totalElements", 0,
            "content", List.of()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 8. AI 서비스 상태 확인
     * GET /api/ai-insights/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAIServiceStatus() {
        try {
            Map<String, Object> status = Map.of(
                "service", "AI Insight Service",
                "status", "active",
                "model", "gpt-4o-mini",
                "features", List.of(
                    "comprehensive_analysis",
                    "trend_analysis", 
                    "category_analysis",
                    "age_group_analysis",
                    "gender_analysis"
                ),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("AI 서비스 상태 확인 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "service", "AI Insight Service",
                "status", "error",
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 에러 응답 생성
     */
    private InsightResponse createErrorResponse(String errorMessage) {
        return new InsightResponse(
            "error",
            errorMessage,
            List.of("서비스 오류 발생"),
            List.of("잠시 후 다시 시도해주세요", "관리자에게 문의하세요"),
            "low"
        );
    }
}
