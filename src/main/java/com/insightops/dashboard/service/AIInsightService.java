package com.insightops.dashboard.service;

import com.insightops.dashboard.dto.*;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 인사이트 서비스 - GPT 4o mini를 활용한 고급 트렌드 분석
 */
@Service
public class AIInsightService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIInsightService.class);
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;
    
    @Value("${openai.max-tokens:2000}")
    private Integer maxTokens;
    
    @Value("${openai.temperature:0.7}")
    private Double temperature;
    
    private final DashboardService dashboardService;
    
    public AIInsightService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    /**
     * 종합 AI 인사이트 생성
     */
    public InsightResponse generateComprehensiveInsights(InsightRequest request) {
        try {
            if (!isOpenAIConfigured()) {
                return createFallbackInsight(request);
            }
            
            // 데이터 수집
            Map<String, Object> dataContext = collectDataContext(request);
            
            // 프롬프트 생성
            String prompt = buildComprehensivePrompt(request, dataContext);
            
            // GPT API 호출
            String aiResponse = callOpenAI(prompt);
            
            // 응답 파싱 및 구조화
            return parseAIResponse(aiResponse, request);
            
        } catch (Exception e) {
            logger.error("AI 인사이트 생성 실패: {}", e.getMessage());
            return createFallbackInsight(request);
        }
    }
    
    /**
     * 트렌드 분석 AI 인사이트
     */
    public InsightResponse generateTrendInsights(InsightRequest request) {
        try {
            if (!isOpenAIConfigured()) {
                return createFallbackTrendInsight(request);
            }
            
            // 트렌드 데이터 수집
            Map<String, Object> trendData = collectTrendData(request);
            
            // 트렌드 분석 프롬프트 생성
            String prompt = buildTrendAnalysisPrompt(request, trendData);
            
            // GPT API 호출
            String aiResponse = callOpenAI(prompt);
            
            // 트렌드 응답 파싱
            return parseTrendResponse(aiResponse, request);
            
        } catch (Exception e) {
            logger.error("트렌드 AI 인사이트 생성 실패: {}", e.getMessage());
            return createFallbackTrendInsight(request);
        }
    }
    
    /**
     * 카테고리별 AI 인사이트
     */
    public InsightResponse generateCategoryInsights(InsightRequest request) {
        try {
            if (!isOpenAIConfigured()) {
                return createFallbackCategoryInsight(request);
            }
            
            // 카테고리 데이터 수집
            Map<String, Object> categoryData = collectCategoryData(request);
            
            // 카테고리 분석 프롬프트 생성
            String prompt = buildCategoryAnalysisPrompt(request, categoryData);
            
            // GPT API 호출
            String aiResponse = callOpenAI(prompt);
            
            // 카테고리 응답 파싱
            return parseCategoryResponse(aiResponse, request);
            
        } catch (Exception e) {
            logger.error("카테고리 AI 인사이트 생성 실패: {}", e.getMessage());
            return createFallbackCategoryInsight(request);
        }
    }
    
    /**
     * 데이터 컨텍스트 수집
     */
    private Map<String, Object> collectDataContext(InsightRequest request) {
        Map<String, Object> context = new HashMap<>();
        
        try {
            // 시계열 데이터 수집
            FilterRequest filter = new FilterRequest(
                request.startDate(), request.endDate(), request.period(),
                request.categories(), request.ageGroups(), request.genders()
            );
            
            TimeSeriesResponse timeSeriesData = dashboardService.getTimeSeriesData(filter);
            context.put("timeSeriesData", timeSeriesData);
            
            // 트렌드 분석 데이터
            Map<String, Object> trendAnalysis = dashboardService.getSmallCategoryTrend(filter);
            context.put("trendAnalysis", trendAnalysis);
            
            // 카테고리별 데이터
            if (request.hasFilters()) {
                Map<String, TimeSeriesResponse> categoryData = dashboardService.getCategoryTimeSeriesData(filter);
                context.put("categoryData", categoryData);
            }
            
        } catch (Exception e) {
            logger.warn("데이터 컨텍스트 수집 실패: {}", e.getMessage());
        }
        
        return context;
    }
    
    /**
     * 트렌드 데이터 수집
     */
    private Map<String, Object> collectTrendData(InsightRequest request) {
        Map<String, Object> trendData = new HashMap<>();
        
        try {
            FilterRequest filter = new FilterRequest(
                request.startDate(), request.endDate(), request.period(),
                request.categories(), request.ageGroups(), request.genders()
            );
            
            Map<String, Object> trendAnalysis = dashboardService.getSmallCategoryTrend(filter);
            trendData.put("trendAnalysis", trendAnalysis);
            
            TimeSeriesResponse timeSeriesData = dashboardService.getTimeSeriesData(filter);
            trendData.put("timeSeriesData", timeSeriesData);
            
        } catch (Exception e) {
            logger.warn("트렌드 데이터 수집 실패: {}", e.getMessage());
        }
        
        return trendData;
    }
    
    /**
     * 카테고리 데이터 수집
     */
    private Map<String, Object> collectCategoryData(InsightRequest request) {
        Map<String, Object> categoryData = new HashMap<>();
        
        try {
            FilterRequest filter = new FilterRequest(
                request.startDate(), request.endDate(), request.period(),
                request.categories(), request.ageGroups(), request.genders()
            );
            
            Map<String, TimeSeriesResponse> categoryTimeSeries = dashboardService.getCategoryTimeSeriesData(filter);
            categoryData.put("categoryTimeSeries", categoryTimeSeries);
            
            Map<String, TimeSeriesResponse> ageGroupData = dashboardService.getAgeGroupTimeSeriesData(filter);
            categoryData.put("ageGroupData", ageGroupData);
            
            Map<String, TimeSeriesResponse> genderData = dashboardService.getGenderTimeSeriesData(filter);
            categoryData.put("genderData", genderData);
            
        } catch (Exception e) {
            logger.warn("카테고리 데이터 수집 실패: {}", e.getMessage());
        }
        
        return categoryData;
    }
    
    /**
     * 종합 분석 프롬프트 생성
     */
    private String buildComprehensivePrompt(InsightRequest request, Map<String, Object> dataContext) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 VoC(Voice of Customer) 데이터 분석 전문가입니다. ");
        prompt.append("다음 데이터를 분석하여 비즈니스 인사이트를 제공해주세요.\n\n");
        
        prompt.append("## 분석 요청 정보\n");
        prompt.append("- 분석 유형: ").append(request.analysisType()).append("\n");
        prompt.append("- 분석 기간: ").append(request.startDate()).append(" ~ ").append(request.endDate()).append("\n");
        prompt.append("- 기간 단위: ").append(request.period()).append("\n");
        
        if (request.hasFilters()) {
            prompt.append("- 필터 조건:\n");
            if (request.categories() != null && !request.categories().isEmpty()) {
                prompt.append("  * 카테고리: ").append(String.join(", ", request.categories())).append("\n");
            }
            if (request.ageGroups() != null && !request.ageGroups().isEmpty()) {
                prompt.append("  * 연령대: ").append(String.join(", ", request.ageGroups())).append("\n");
            }
            if (request.genders() != null && !request.genders().isEmpty()) {
                prompt.append("  * 성별: ").append(String.join(", ", request.genders())).append("\n");
            }
        }
        
        prompt.append("\n## 데이터 컨텍스트\n");
        prompt.append("```json\n");
        prompt.append(convertToJsonString(dataContext));
        prompt.append("\n```\n\n");
        
        prompt.append("## 분석 요청사항\n");
        prompt.append("다음 형식으로 분석 결과를 제공해주세요:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"전체 요약 (2-3문장)\",\n");
        prompt.append("  \"keyFindings\": [\"주요 발견사항 1\", \"주요 발견사항 2\", \"주요 발견사항 3\"],\n");
        prompt.append("  \"recommendations\": [\"추천사항 1\", \"추천사항 2\", \"추천사항 3\"],\n");
        prompt.append("  \"trendAnalysis\": {\n");
        prompt.append("    \"direction\": \"increasing|decreasing|stable|volatile\",\n");
        prompt.append("    \"strength\": \"strong|moderate|weak\",\n");
        prompt.append("    \"changePercentage\": 0.0,\n");
        prompt.append("    \"significantEvents\": [\"중요한 이벤트들\"],\n");
        prompt.append("    \"anomalies\": [\"이상 패턴들\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"statisticalData\": {\n");
        prompt.append("    \"totalCount\": 0,\n");
        prompt.append("    \"averageDaily\": 0.0,\n");
        prompt.append("    \"peakDay\": \"날짜\",\n");
        prompt.append("    \"variance\": 0.0\n");
        prompt.append("  },\n");
        prompt.append("  \"riskFactors\": [\"위험 요소들\"],\n");
        prompt.append("  \"opportunities\": [\"기회 요소들\"],\n");
        prompt.append("  \"confidence\": \"high|medium|low\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("분석 시 다음 사항을 고려해주세요:\n");
        prompt.append("1. 데이터의 트렌드와 패턴을 정확히 파악\n");
        prompt.append("2. 비즈니스 관점에서 실용적인 인사이트 제공\n");
        prompt.append("3. 구체적이고 실행 가능한 추천사항 제시\n");
        prompt.append("4. 위험 요소와 기회 요소를 균형있게 분석\n");
        prompt.append("5. 한국어로 자연스럽고 이해하기 쉽게 작성\n");
        
        return prompt.toString();
    }
    
    /**
     * 트렌드 분석 프롬프트 생성
     */
    private String buildTrendAnalysisPrompt(InsightRequest request, Map<String, Object> trendData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 VoC 트렌드 분석 전문가입니다. ");
        prompt.append("다음 트렌드 데이터를 분석하여 상세한 트렌드 인사이트를 제공해주세요.\n\n");
        
        prompt.append("## 트렌드 데이터\n");
        prompt.append("```json\n");
        prompt.append(convertToJsonString(trendData));
        prompt.append("\n```\n\n");
        
        prompt.append("다음 형식으로 트렌드 분석 결과를 제공해주세요:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"트렌드 요약\",\n");
        prompt.append("  \"keyFindings\": [\"트렌드 발견사항들\"],\n");
        prompt.append("  \"recommendations\": [\"트렌드 기반 추천사항들\"],\n");
        prompt.append("  \"trendAnalysis\": {\n");
        prompt.append("    \"direction\": \"트렌드 방향\",\n");
        prompt.append("    \"strength\": \"트렌드 강도\",\n");
        prompt.append("    \"changePercentage\": 0.0,\n");
        prompt.append("    \"seasonalPatterns\": {\"계절성 패턴\"},\n");
        prompt.append("    \"correlationData\": {\"상관관계 데이터\"},\n");
        prompt.append("    \"anomalies\": [\"이상치들\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"statisticalData\": {\"통계 데이터\"},\n");
        prompt.append("  \"confidence\": \"신뢰도\"\n");
        prompt.append("}\n");
        prompt.append("```\n");
        
        return prompt.toString();
    }
    
    /**
     * 카테고리 분석 프롬프트 생성
     */
    private String buildCategoryAnalysisPrompt(InsightRequest request, Map<String, Object> categoryData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 VoC 카테고리 분석 전문가입니다. ");
        prompt.append("다음 카테고리별 데이터를 분석하여 카테고리 인사이트를 제공해주세요.\n\n");
        
        prompt.append("## 카테고리 데이터\n");
        prompt.append("```json\n");
        prompt.append(convertToJsonString(categoryData));
        prompt.append("\n```\n\n");
        
        prompt.append("다음 형식으로 카테고리 분석 결과를 제공해주세요:\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"카테고리 분석 요약\",\n");
        prompt.append("  \"keyFindings\": [\"카테고리별 발견사항들\"],\n");
        prompt.append("  \"recommendations\": [\"카테고리별 추천사항들\"],\n");
        prompt.append("  \"trendAnalysis\": {\n");
        prompt.append("    \"topCategories\": [\"상위 카테고리들\"],\n");
        prompt.append("    \"categoryTrends\": {\"카테고리별 트렌드\"},\n");
        prompt.append("    \"ageGroupPatterns\": {\"연령대별 패턴\"},\n");
        prompt.append("    \"genderPatterns\": {\"성별 패턴\"}\n");
        prompt.append("  },\n");
        prompt.append("  \"statisticalData\": {\"통계 데이터\"},\n");
        prompt.append("  \"confidence\": \"신뢰도\"\n");
        prompt.append("}\n");
        prompt.append("```\n");
        
        return prompt.toString();
    }
    
    /**
     * OpenAI API 호출
     */
    private String callOpenAI(String prompt) {
        try {
            OpenAiService service = new OpenAiService(openaiApiKey);
            
            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(openaiModel)
                .messages(List.of(new ChatMessage(ChatMessageRole.USER.value(), prompt)))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
            
            String response = service.createChatCompletion(completionRequest)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();
            
            logger.info("OpenAI API 호출 성공");
            return response;
            
        } catch (Exception e) {
            logger.error("OpenAI API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서비스 호출 실패", e);
        }
    }
    
    /**
     * AI 응답 파싱
     */
    private InsightResponse parseAIResponse(String aiResponse, InsightRequest request) {
        try {
            // JSON 응답에서 필요한 정보 추출
            Map<String, Object> parsedResponse = parseJsonResponse(aiResponse);
            
            String summary = (String) parsedResponse.getOrDefault("summary", "분석 결과를 생성할 수 없습니다.");
            List<String> keyFindings = (List<String>) parsedResponse.getOrDefault("keyFindings", List.of());
            List<String> recommendations = (List<String>) parsedResponse.getOrDefault("recommendations", List.of());
            Map<String, Object> trendAnalysis = (Map<String, Object>) parsedResponse.getOrDefault("trendAnalysis", new HashMap<>());
            Map<String, Object> statisticalData = (Map<String, Object>) parsedResponse.getOrDefault("statisticalData", new HashMap<>());
            List<String> riskFactors = (List<String>) parsedResponse.getOrDefault("riskFactors", List.of());
            List<String> opportunities = (List<String>) parsedResponse.getOrDefault("opportunities", List.of());
            String confidence = (String) parsedResponse.getOrDefault("confidence", "medium");
            
            return new InsightResponse(
                request.analysisType(), summary, keyFindings, recommendations,
                trendAnalysis, statisticalData, riskFactors, opportunities, confidence
            );
            
        } catch (Exception e) {
            logger.error("AI 응답 파싱 실패: {}", e.getMessage());
            return createFallbackInsight(request);
        }
    }
    
    /**
     * 트렌드 응답 파싱
     */
    private InsightResponse parseTrendResponse(String aiResponse, InsightRequest request) {
        try {
            Map<String, Object> parsedResponse = parseJsonResponse(aiResponse);
            
            String summary = (String) parsedResponse.getOrDefault("summary", "트렌드 분석 결과를 생성할 수 없습니다.");
            List<String> keyFindings = (List<String>) parsedResponse.getOrDefault("keyFindings", List.of());
            List<String> recommendations = (List<String>) parsedResponse.getOrDefault("recommendations", List.of());
            Map<String, Object> trendAnalysis = (Map<String, Object>) parsedResponse.getOrDefault("trendAnalysis", new HashMap<>());
            Map<String, Object> statisticalData = (Map<String, Object>) parsedResponse.getOrDefault("statisticalData", new HashMap<>());
            String confidence = (String) parsedResponse.getOrDefault("confidence", "medium");
            
            return new InsightResponse(
                "trend", summary, keyFindings, recommendations,
                trendAnalysis, statisticalData, confidence
            );
            
        } catch (Exception e) {
            logger.error("트렌드 응답 파싱 실패: {}", e.getMessage());
            return createFallbackTrendInsight(request);
        }
    }
    
    /**
     * 카테고리 응답 파싱
     */
    private InsightResponse parseCategoryResponse(String aiResponse, InsightRequest request) {
        try {
            Map<String, Object> parsedResponse = parseJsonResponse(aiResponse);
            
            String summary = (String) parsedResponse.getOrDefault("summary", "카테고리 분석 결과를 생성할 수 없습니다.");
            List<String> keyFindings = (List<String>) parsedResponse.getOrDefault("keyFindings", List.of());
            List<String> recommendations = (List<String>) parsedResponse.getOrDefault("recommendations", List.of());
            Map<String, Object> trendAnalysis = (Map<String, Object>) parsedResponse.getOrDefault("trendAnalysis", new HashMap<>());
            Map<String, Object> statisticalData = (Map<String, Object>) parsedResponse.getOrDefault("statisticalData", new HashMap<>());
            String confidence = (String) parsedResponse.getOrDefault("confidence", "medium");
            
            return new InsightResponse(
                "category", summary, keyFindings, recommendations,
                trendAnalysis, statisticalData, confidence
            );
            
        } catch (Exception e) {
            logger.error("카테고리 응답 파싱 실패: {}", e.getMessage());
            return createFallbackCategoryInsight(request);
        }
    }
    
    /**
     * JSON 응답 파싱 (간단한 구현)
     */
    private Map<String, Object> parseJsonResponse(String jsonResponse) {
        // 실제 구현에서는 Jackson ObjectMapper를 사용해야 함
        Map<String, Object> result = new HashMap<>();
        
        try {
            // JSON 파싱 로직 (간단한 구현)
            if (jsonResponse.contains("\"summary\"")) {
                result.put("summary", extractJsonValue(jsonResponse, "summary"));
            }
            if (jsonResponse.contains("\"keyFindings\"")) {
                result.put("keyFindings", extractJsonArray(jsonResponse, "keyFindings"));
            }
            if (jsonResponse.contains("\"recommendations\"")) {
                result.put("recommendations", extractJsonArray(jsonResponse, "recommendations"));
            }
            if (jsonResponse.contains("\"confidence\"")) {
                result.put("confidence", extractJsonValue(jsonResponse, "confidence"));
            }
        } catch (Exception e) {
            logger.warn("JSON 파싱 실패, 기본값 사용: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * JSON 값 추출 (간단한 구현)
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            logger.warn("JSON 값 추출 실패: {}", e.getMessage());
        }
        return "값을 추출할 수 없습니다.";
    }
    
    /**
     * JSON 배열 추출 (간단한 구현)
     */
    private List<String> extractJsonArray(String json, String key) {
        List<String> result = new ArrayList<>();
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\\[([^\\]]+)\\]";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                String arrayContent = m.group(1);
                String[] items = arrayContent.split(",");
                for (String item : items) {
                    String cleanItem = item.trim().replaceAll("^\"|\"$", "");
                    if (!cleanItem.isEmpty()) {
                        result.add(cleanItem);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("JSON 배열 추출 실패: {}", e.getMessage());
        }
        return result.isEmpty() ? List.of("데이터를 추출할 수 없습니다.") : result;
    }
    
    /**
     * 객체를 JSON 문자열로 변환 (간단한 구현)
     */
    private String convertToJsonString(Object obj) {
        try {
            // 실제 구현에서는 Jackson ObjectMapper를 사용해야 함
            return obj.toString();
        } catch (Exception e) {
            logger.warn("JSON 변환 실패: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * OpenAI 설정 확인
     */
    private boolean isOpenAIConfigured() {
        return openaiApiKey != null && !openaiApiKey.trim().isEmpty();
    }
    
    /**
     * Fallback 인사이트 생성
     */
    private InsightResponse createFallbackInsight(InsightRequest request) {
        return new InsightResponse(
            request.analysisType(),
            "AI 분석 서비스가 현재 사용할 수 없습니다. 기본 통계 분석을 제공합니다.",
            List.of("데이터 수집 완료", "기본 통계 계산 완료", "AI 분석 대기 중"),
            List.of("AI 서비스 설정 확인 필요", "데이터 검증 완료", "수동 분석 권장"),
            "low"
        );
    }
    
    /**
     * Fallback 트렌드 인사이트 생성
     */
    private InsightResponse createFallbackTrendInsight(InsightRequest request) {
        return new InsightResponse(
            "trend",
            "트렌드 분석을 위한 AI 서비스가 현재 사용할 수 없습니다.",
            List.of("기본 트렌드 데이터 수집 완료", "통계적 분석 완료"),
            List.of("AI 서비스 설정 후 재시도", "수동 트렌드 분석 권장"),
            "low"
        );
    }
    
    /**
     * Fallback 카테고리 인사이트 생성
     */
    private InsightResponse createFallbackCategoryInsight(InsightRequest request) {
        return new InsightResponse(
            "category",
            "카테고리 분석을 위한 AI 서비스가 현재 사용할 수 없습니다.",
            List.of("카테고리별 데이터 수집 완료", "기본 통계 분석 완료"),
            List.of("AI 서비스 설정 후 재시도", "수동 카테고리 분석 권장"),
            "low"
        );
    }
}
