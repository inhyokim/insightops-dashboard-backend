package com.insightops.dashboard.service;

import com.insightops.dashboard.client.AdminServiceClient;
import com.insightops.dashboard.client.MailServiceClient;
import com.insightops.dashboard.client.NormalizationServiceClient;
import com.insightops.dashboard.client.VoicebotServiceClient;
import com.insightops.dashboard.domain.InsightCard;
import com.insightops.dashboard.domain.MessagePreviewCache;
import com.insightops.dashboard.dto.*;
import com.insightops.dashboard.repository.*;
import com.insightops.dashboard.service.VocDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 대시보드 서비스 - MSA 구조에 맞게 외부 API 호출
 * insightops_dashboard DB는 집계/캐시 전용
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    @Value("${spring.profiles.active:production}")
    private String activeProfile;

    // 로컬 집계/캐시 리포지토리
    private final AggTotalRepository aggTotalRepo;
    private final AggByCategoryAgeGenderRepository aggCategoryRepo;
    private final InsightCardRepository insightRepo;
    private final MessagePreviewCacheRepository messageRepo;
    private final VocListCacheRepository vocListRepo;
    
    // 외부 서비스 클라이언트
    private final VoicebotServiceClient voicebotClient;
    private final NormalizationServiceClient normalizationClient;
    private final MailServiceClient mailClient;
    private final AdminServiceClient adminClient;
    private final VocDataService vocDataService;
    
    public DashboardService(AggTotalRepository aggTotalRepo,
                           AggByCategoryAgeGenderRepository aggCategoryRepo,
                           InsightCardRepository insightRepo,
                           MessagePreviewCacheRepository messageRepo,
                           VocListCacheRepository vocListRepo,
                           VoicebotServiceClient voicebotClient,
                           NormalizationServiceClient normalizationClient,
                           MailServiceClient mailClient,
                           AdminServiceClient adminClient,
                           VocDataService vocDataService) {
        this.aggTotalRepo = aggTotalRepo;
        this.aggCategoryRepo = aggCategoryRepo;
        this.insightRepo = insightRepo;
        this.messageRepo = messageRepo;
        this.vocListRepo = vocListRepo;
        this.voicebotClient = voicebotClient;
        this.normalizationClient = normalizationClient;
        this.mailClient = mailClient;
        this.adminClient = adminClient;
        this.vocDataService = vocDataService;
    }

    /**
     * A. 오버뷰 화면 데이터 조회 - period별 토글 지원 (daily/weekly/monthly)
     */
    public OverviewDto getOverview(String period) {
        // 기본값 설정
        if (period == null || period.isEmpty()) {
            period = "daily";
        }
        
        // 로컬 개발환경에서는 Mock 데이터 반환
        if ("local".equals(activeProfile)) {
            return generateMockOverviewData(period);
        }
        
        try {
            // 새로운 스키마에서 최신 집계 데이터 조회
            var overviewData = aggTotalRepo.findLatestByPeriodType(period);
            
            if (overviewData.isPresent()) {
                var data = overviewData.get();
                
                // Top 카테고리 조회 (안전한 처리)
                String topCategory = "정보 없음";
                double topRatio = 0.0;
                
                try {
                    LocalDate now = LocalDate.now();
                    LocalDate firstDay = now.withDayOfMonth(1);
                    var topSmall = aggCategoryRepo.findTopSmallOfMonth(firstDay);
                    topCategory = topSmall.map(row -> row.getSmallName()).orElse("정보 없음");
                    topRatio = topSmall.map(row ->
                        row.getTotalCnt() == 0 ? 0.0 : (double)row.getCnt() / row.getTotalCnt() * 100.0
                    ).orElse(0.0);
                } catch (Exception e) {
                    logger.warn("Top 카테고리 조회 실패, 기본값 사용: {}", e.getMessage());
                }
                
                return new OverviewDto(
                    data.getTotalCount(), 
                    data.getPrevCount(), 
                    data.getDeltaPercent(), 
                    topCategory, 
                    topRatio
                );
            } else {
                // 집계 데이터가 없는 경우 실시간 계산
                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);
                
                Long currentCount = 0L;
                Long prevCount = 0L;
                
                // Period별 실시간 계산
                switch (period.toLowerCase()) {
                    case "daily":
                        currentCount = vocDataService.getDailyVocCount(yesterday);
                        prevCount = vocDataService.getDailyVocCount(yesterday.minusDays(1));
                        break;
                    case "weekly":
                        currentCount = vocDataService.getWeeklyVocCount(yesterday);
                        prevCount = vocDataService.getWeeklyVocCount(yesterday.minusDays(7));
                        break;
                    case "monthly":
                        currentCount = vocDataService.getMonthlyVocCount(yesterday);
                        prevCount = vocDataService.getMonthlyVocCount(yesterday.minusDays(30));
                        break;
                    default:
                        currentCount = vocDataService.getDailyVocCount(yesterday);
                        prevCount = vocDataService.getDailyVocCount(yesterday.minusDays(1));
                }
                
                // 증감률 계산
                double deltaPercent = prevCount == 0 ? 0.0 : ((double)(currentCount - prevCount) / prevCount) * 100.0;
                
                return new OverviewDto(currentCount, prevCount, deltaPercent, "정보 없음", 0.0);
            }
        } catch (Exception e) {
            logger.error("오버뷰 데이터 조회 중 오류: {}", e.getMessage(), e);
            // 오류 발생 시 Mock 데이터 반환
            return generateMockOverviewData(period);
        }
    }
    
    /**
     * Mock 오버뷰 데이터 생성 (로컬 개발환경용)
     */
    private OverviewDto generateMockOverviewData(String period) {
        // Period별 Mock 데이터
        Long currentCount = switch (period.toLowerCase()) {
            case "daily" -> 211L;
            case "weekly" -> 1487L;
            case "monthly" -> 6330L;
            default -> 211L;
        };
        
        Long prevCount = switch (period.toLowerCase()) {
            case "daily" -> 257L;
            case "weekly" -> 1623L;
            case "monthly" -> 5890L;
            default -> 257L;
        };
        
        double deltaPercent = prevCount == 0 ? 0.0 : ((double)(currentCount - prevCount) / prevCount) * 100.0;
        
        return new OverviewDto(currentCount, prevCount, deltaPercent, "이용내역 안내", 28.5);
    }
    
    /**
     * A. 오버뷰 화면 데이터 조회 (기존 호환성을 위한 deprecated 메서드)
     */
    @Deprecated
    public OverviewDto getOverview(YearMonth yearMonth) {
        return getOverview("monthly");
    }

    /**
     * B. Big 카테고리 비중 데이터 조회 (파이차트용) - Normalization API 기반
     */
    public List<ShareItem> getBigCategoryShare(String granularity, LocalDate from, LocalDate to) {
        // 로컬 개발환경에서는 Mock 데이터 반환
        if ("local".equals(activeProfile)) {
            return generateMockBigCategoryShare();
        }
        
        try {
            // Normalization Service에서 voc_normalized 데이터 조회
            List<CaseItem> vocList = normalizationClient.getVocEventsWithSummary(
                from.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                to.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                null, 1, 10000);
            
            // Small Category를 Big Category로 그룹핑하여 집계
            Map<String, Long> bigCategoryMap = vocList.stream()
                .collect(Collectors.groupingBy(
                    voc -> mapSmallToBigCategory(voc.consultingCategoryName()),
                    Collectors.counting()
                ));
            
            // 총 건수 계산
            long totalCount = bigCategoryMap.values().stream().mapToLong(Long::longValue).sum();
            
            // ShareItem으로 변환하여 비중 계산
            return bigCategoryMap.entrySet().stream()
                .map(entry -> {
                    String bigCategory = entry.getKey();
                    Long count = entry.getValue();
                    double ratio = totalCount == 0 ? 0.0 : (double) count / totalCount * 100.0;
                    return new ShareItem(bigCategory, count, Math.round(ratio * 10) / 10.0);
                })
                .sorted((a, b) -> Long.compare(b.count(), a.count())) // 건수 내림차순 정렬
                .toList();
        } catch (Exception e) {
            logger.error("빅카테고리 비중 데이터 조회 오류: {}", e.getMessage());
            return generateMockBigCategoryShare(); // Mock 데이터 반환
        }
    }
    
    /**
     * Mock Big Category Share 데이터 생성
     */
    private List<ShareItem> generateMockBigCategoryShare() {
        return List.of(
            new ShareItem("조회/안내", 45L, 35.2),
            new ShareItem("즉시처리", 32L, 25.0),
            new ShareItem("변경/등록요청", 25L, 19.5),
            new ShareItem("금융상품/대출연계", 15L, 11.7),
            new ShareItem("기타", 11L, 8.6)
        );
    }

    /**
     * C. 전체 VoC 변화량 시계열 데이터 조회 (라인차트용) - 로컬 집계 캐시
     */
    public List<SeriesPoint> getTotalSeries(String granularity, LocalDate from, LocalDate to) {
        // 로컬 개발환경에서는 Mock 데이터 반환
        if ("local".equals(activeProfile)) {
            return generateMockTotalSeries(granularity, from, to);
        }
        
        try {
            var points = aggTotalRepo.findSeries(granularity, from, to);
            
            return points.stream()
                .map(point -> new SeriesPoint(point.getBucketStart(), point.getTotalCount()))
                .toList();
        } catch (Exception e) {
            logger.error("시계열 데이터 조회 실패: {}", e.getMessage());
            return generateMockTotalSeries(granularity, from, to); // Mock 데이터 반환
        }
    }
    
    /**
     * Mock 시계열 데이터 생성
     */
    private List<SeriesPoint> generateMockTotalSeries(String granularity, LocalDate from, LocalDate to) {
        List<SeriesPoint> mockData = new ArrayList<>();
        LocalDate current = from;
        
        while (!current.isAfter(to)) {
            Long count = 150L + (current.getDayOfMonth() % 50); // 150~199 범위의 Mock 데이터
            mockData.add(new SeriesPoint(current, count));
            
            // 다음 날짜로 이동
            current = switch (granularity.toLowerCase()) {
                case "daily" -> current.plusDays(1);
                case "weekly" -> current.plusWeeks(1);
                case "monthly" -> current.plusMonths(1);
                default -> current.plusDays(1);
            };
        }
        
        return mockData;
    }

    /**
     * D. Small 카테고리 트렌드 분석 (필터: 연령, 성별) - 로컬 집계 캐시
     */
    public List<SmallTrendItem> getSmallTrends(String granularity, LocalDate from, LocalDate to,
                                               String clientAge, String clientGender, int limit) {
        try {
            var trends = aggCategoryRepo.findSmallTrends(granularity, from, to, clientAge, clientGender, limit);
            
            return trends.stream()
                .map(row -> new SmallTrendItem(row.getSmallName(), row.getCnt()))
                .toList();
        } catch (Exception e) {
            logger.error("Small 카테고리 트렌드 조회 실패: {}", e.getMessage());
            return List.of(); // 빈 리스트 반환
        }
    }

    /**
     * E. 인사이트 카드 Top 10 조회 - 로컬 캐시
     */
    public List<InsightCard> getInsights() {
        // 로컬 개발환경에서는 Mock 데이터 반환
        if ("local".equals(activeProfile)) {
            return generateMockInsights();
        }
        
        try {
            return insightRepo.findTop10ByOrderByScoreDesc();
        } catch (Exception e) {
            logger.error("인사이트 카드 조회 실패: {}", e.getMessage());
            return generateMockInsights(); // Mock 데이터 반환
        }
    }
    
    /**
     * Mock 인사이트 데이터 생성
     */
    private List<InsightCard> generateMockInsights() {
        // InsightCard 생성을 위해 필요한 필드들을 확인해야 하지만, 임시로 빈 리스트 반환
        return List.of();
    }

    /**
     * F. 상담 사례 목록 조회 - VocListCache에서 조회
     */
    public List<CaseItem> getCases(LocalDate from, LocalDate to, String consultingCategory, int page, int size) {
        // VocListCache에서 필터링하여 조회
        List<com.insightops.dashboard.domain.VocListCache> vocList;
        
        if (consultingCategory != null && !consultingCategory.trim().isEmpty()) {
            // 카테고리 필터링 적용
            vocList = vocListRepo.findByConsultingDateBetweenAndConsultingCategoryOrderByConsultingDateDesc(
                from, to, consultingCategory);
        } else {
            // 전체 조회
            vocList = vocListRepo.findByConsultingDateBetweenOrderByConsultingDateDesc(from, to);
        }
        
        // 페이징 처리 (간단한 방식)
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, vocList.size());
        
        if (startIndex >= vocList.size()) {
            return List.of(); // 빈 리스트 반환
        }
        
        List<com.insightops.dashboard.domain.VocListCache> pagedList = vocList.subList(startIndex, endIndex);
        
        // VocListCache를 CaseItem으로 변환
        return pagedList.stream()
            .map(voc -> new CaseItem(
                voc.getVocId().hashCode() % 10000L, // 임시 ID (실제로는 sequence나 별도 ID 사용)
                voc.getSourceSystem(),
                voc.getConsultingDate().toString(),
                mapSmallToBigCategory(voc.getConsultingCategory()), // Big Category 매핑
                voc.getConsultingCategory(), // Small Category
                voc.getClientAge(),
                voc.getClientGender(),
                voc.getSummaryText()
            ))
            .toList();
    }

    /**
     * G. 메일 프리뷰 최근 50개 조회 - 로컬 캐시
     */
    public List<MessagePreviewCache> getRecentMessages() {
        return messageRepo.findTop50ByOrderByCreatedAtDesc();
    }
    
    /**
     * H. 메일 초안 생성 (외부 Mail Service 호출)
     */
    public MailPreviewDto generateMailPreview(String vocId) {
        return mailClient.generateMailPreview(Long.valueOf(vocId));
    }
    
    /**
     * I. 메일 발송 (외부 Mail Service 호출)
     */
    public void sendMail(MailSendRequestDto request) {
        mailClient.sendMail(request);
    }
    
    /**
     * J. VoC 상세보기 - 분석 결과 조회 (Normalization Service API 호출)
     */
    public String getVocAnalysisResult(Long vocEventId) {
        return normalizationClient.getVocAnalysisResult(vocEventId);
    }
    
    /**
     * K. Top Small Category 계산 로직
     */
    public Map<String, Object> getTopSmallCategory(String period, LocalDate baseDate) {
        try {
            // Normalization Service에서 데이터 조회
            LocalDate from = calculatePeriodStart(period, baseDate);
            LocalDate to = baseDate;
            
            List<CaseItem> vocList = normalizationClient.getVocEventsWithSummary(
                from.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                to.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                null, 1, 10000);
            
            // Small Category별 집계
            Map<String, Long> categoryCounts = vocList.stream()
                .collect(Collectors.groupingBy(
                    CaseItem::consultingCategoryName,
                    Collectors.counting()
                ));
            
            // Top Category 찾기
            String topCategory = categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("정보 없음");
            
            Long topCount = categoryCounts.getOrDefault(topCategory, 0L);
            Long totalCount = categoryCounts.values().stream().mapToLong(Long::longValue).sum();
            double topShare = totalCount > 0 ? (topCount * 100.0 / totalCount) : 0.0;
            
            Map<String, Object> result = new HashMap<>();
            result.put("topCategory", topCategory);
            result.put("topCount", topCount);
            result.put("totalCount", totalCount);
            result.put("topShare", topShare);
            result.put("period", period);
            result.put("baseDate", baseDate.toString());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Top Small Category 계산 실패: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("topCategory", "정보 없음");
            fallback.put("topCount", 0L);
            fallback.put("totalCount", 0L);
            fallback.put("topShare", 0.0);
            fallback.put("period", period);
            fallback.put("baseDate", baseDate.toString());
            return fallback;
        }
    }
    
    /**
     * L. 기간별 시작일 계산
     */
    private LocalDate calculatePeriodStart(String period, LocalDate baseDate) {
        return switch (period.toLowerCase()) {
            case "daily" -> baseDate.minusDays(1);
            case "weekly" -> baseDate.minusWeeks(1);
            case "monthly" -> baseDate.minusMonths(1);
            default -> baseDate.minusDays(1);
        };
    }
    
    /**
     * M. Period별 비교 데이터 조회
     */
    public Map<String, Object> getPeriodComparison(String period, LocalDate baseDate) {
        return vocDataService.getPeriodComparison(period, baseDate);
    }
    
    /**
     * N. 배치 집계 데이터 조회
     */
    public Map<String, Map<String, Object>> getBatchCounts(LocalDate baseDate) {
        return vocDataService.getBatchPeriodCounts(baseDate);
    }
    
    /**
     * O. 시계열 데이터 조회 (필터링 지원)
     */
    public TimeSeriesResponse getTimeSeriesData(FilterRequest filter) {
        try {
            return vocDataService.getTimeSeriesData(filter);
        } catch (Exception e) {
            logger.error("시계열 데이터 조회 실패: {}", e.getMessage());
            return getTimeSeriesDataDefault(filter);
        }
    }
    
    /**
     * P. 카테고리별 시계열 데이터 조회
     */
    public Map<String, TimeSeriesResponse> getCategoryTimeSeriesData(FilterRequest filter) {
        try {
            return vocDataService.getCategoryTimeSeriesData(filter);
        } catch (Exception e) {
            logger.error("카테고리별 시계열 데이터 조회 실패: {}", e.getMessage());
            return getCategoryTimeSeriesDataDefault(filter);
        }
    }
    
    /**
     * Q. 연령대별 시계열 데이터 조회
     */
    public Map<String, TimeSeriesResponse> getAgeGroupTimeSeriesData(FilterRequest filter) {
        try {
            return vocDataService.getAgeGroupTimeSeriesData(filter);
        } catch (Exception e) {
            logger.error("연령대별 시계열 데이터 조회 실패: {}", e.getMessage());
            return getAgeGroupTimeSeriesDataDefault(filter);
        }
    }
    
    /**
     * R. 성별 시계열 데이터 조회
     */
    public Map<String, TimeSeriesResponse> getGenderTimeSeriesData(FilterRequest filter) {
        try {
            return vocDataService.getGenderTimeSeriesData(filter);
        } catch (Exception e) {
            logger.error("성별 시계열 데이터 조회 실패: {}", e.getMessage());
            return getGenderTimeSeriesDataDefault(filter);
        }
    }
    
    /**
     * S. Small Category 트렌드 분석 (필터링 + 시계열)
     */
    public Map<String, Object> getSmallCategoryTrend(FilterRequest filter) {
        try {
            // 시계열 데이터 조회
            TimeSeriesResponse timeSeriesData = getTimeSeriesData(filter);
            
            // 트렌드 분석
            Map<String, Object> trendAnalysis = analyzeTrend(timeSeriesData.data());
            
            // 결과 구성
            Map<String, Object> result = new HashMap<>();
            result.put("timeSeriesData", timeSeriesData);
            result.put("trendAnalysis", trendAnalysis);
            result.put("filter", filter);
            result.put("summary", generateTrendSummary(trendAnalysis));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Small Category 트렌드 분석 실패: {}", e.getMessage());
            return getSmallCategoryTrendDefault(filter);
        }
    }
    
    // ========== Fallback 메서드들 ==========
    
    /**
     * 시계열 데이터 캐시에서 조회
     */
    private TimeSeriesResponse getTimeSeriesDataFromCache(FilterRequest filter) {
        // TODO: 캐시에서 데이터 조회 로직 구현
        logger.warn("시계열 데이터 캐시 조회 - 구현 필요");
        return getTimeSeriesDataDefault(filter);
    }
    
    /**
     * 시계열 데이터 기본값 반환
     */
    private TimeSeriesResponse getTimeSeriesDataDefault(FilterRequest filter) {
        List<TimeSeriesItem> emptyData = List.of();
        return new TimeSeriesResponse(emptyData, filter, filter.period());
    }
    
    /**
     * 카테고리별 시계열 데이터 캐시에서 조회
     */
    private Map<String, TimeSeriesResponse> getCategoryTimeSeriesDataFromCache(FilterRequest filter) {
        logger.warn("카테고리별 시계열 데이터 캐시 조회 - 구현 필요");
        return getCategoryTimeSeriesDataDefault(filter);
    }
    
    /**
     * 카테고리별 시계열 데이터 기본값 반환
     */
    private Map<String, TimeSeriesResponse> getCategoryTimeSeriesDataDefault(FilterRequest filter) {
        Map<String, TimeSeriesResponse> defaultData = new HashMap<>();
        List<String> categories = filter.hasCategoryFilter() ? filter.categories() : List.of("전체");
        
        for (String category : categories) {
            defaultData.put(category, getTimeSeriesDataDefault(filter));
        }
        
        return defaultData;
    }
    
    /**
     * 연령대별 시계열 데이터 캐시에서 조회
     */
    private Map<String, TimeSeriesResponse> getAgeGroupTimeSeriesDataFromCache(FilterRequest filter) {
        logger.warn("연령대별 시계열 데이터 캐시 조회 - 구현 필요");
        return getAgeGroupTimeSeriesDataDefault(filter);
    }
    
    /**
     * 연령대별 시계열 데이터 기본값 반환
     */
    private Map<String, TimeSeriesResponse> getAgeGroupTimeSeriesDataDefault(FilterRequest filter) {
        Map<String, TimeSeriesResponse> defaultData = new HashMap<>();
        List<String> ageGroups = filter.hasAgeGroupFilter() ? filter.ageGroups() : List.of("20", "30", "40");
        
        for (String ageGroup : ageGroups) {
            defaultData.put(ageGroup, getTimeSeriesDataDefault(filter));
        }
        
        return defaultData;
    }
    
    /**
     * 성별 시계열 데이터 캐시에서 조회
     */
    private Map<String, TimeSeriesResponse> getGenderTimeSeriesDataFromCache(FilterRequest filter) {
        logger.warn("성별 시계열 데이터 캐시 조회 - 구현 필요");
        return getGenderTimeSeriesDataDefault(filter);
    }
    
    /**
     * 성별 시계열 데이터 기본값 반환
     */
    private Map<String, TimeSeriesResponse> getGenderTimeSeriesDataDefault(FilterRequest filter) {
        Map<String, TimeSeriesResponse> defaultData = new HashMap<>();
        List<String> genders = filter.hasGenderFilter() ? filter.genders() : List.of("남자", "여자");
        
        for (String gender : genders) {
            defaultData.put(gender, getTimeSeriesDataDefault(filter));
        }
        
        return defaultData;
    }
    
    /**
     * Small Category 트렌드 분석 기본값 반환
     */
    private Map<String, Object> getSmallCategoryTrendDefault(FilterRequest filter) {
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("timeSeriesData", getTimeSeriesDataDefault(filter));
        defaultResult.put("trendAnalysis", Map.of("trend", "STABLE", "change", 0.0));
        defaultResult.put("filter", filter);
        defaultResult.put("summary", "데이터를 불러올 수 없습니다.");
        return defaultResult;
    }
    
    // ========== 분석 메서드들 ==========
    
    /**
     * 트렌드 분석
     */
    private Map<String, Object> analyzeTrend(List<TimeSeriesItem> data) {
        if (data.isEmpty()) {
            return Map.of("trend", "NO_DATA", "change", 0.0, "direction", "STABLE");
        }
        
        // 첫 번째와 마지막 값 비교
        TimeSeriesItem first = data.get(0);
        TimeSeriesItem last = data.get(data.size() - 1);
        
        double change = first.count() > 0 ? 
            ((last.count() - first.count()) * 100.0 / first.count()) : 0.0;
        
        String trend = determineTrend(change);
        String direction = change > 0 ? "INCREASING" : change < 0 ? "DECREASING" : "STABLE";
        
        return Map.of(
            "trend", trend,
            "change", change,
            "direction", direction,
            "firstCount", first.count(),
            "lastCount", last.count(),
            "totalPeriods", data.size()
        );
    }
    
    /**
     * 트렌드 분류
     */
    private String determineTrend(double change) {
        if (Math.abs(change) > 50) return "SHARP_CHANGE";
        if (Math.abs(change) > 20) return "SIGNIFICANT_CHANGE";
        if (Math.abs(change) > 5) return "MODERATE_CHANGE";
        return "STABLE";
    }
    
    /**
     * 트렌드 요약 생성
     */
    private String generateTrendSummary(Map<String, Object> trendAnalysis) {
        String trend = (String) trendAnalysis.get("trend");
        Double change = (Double) trendAnalysis.get("change");
        String direction = (String) trendAnalysis.get("direction");
        
        String directionText = switch (direction) {
            case "INCREASING" -> "증가";
            case "DECREASING" -> "감소";
            default -> "유지";
        };
        
        return String.format("트렌드: %s, 변화율: %.1f%%, 방향: %s", trend, change, directionText);
    }
    
    /**
     * Small Category를 Big Category로 매핑하는 유틸리티 메서드
     */
    private String mapSmallToBigCategory(String consultingCategory) {
        if (consultingCategory == null) {
            return "기타";
        }
        
        // 1. 조회/안내
        if (consultingCategory.equals("이용내역 안내") ||
            consultingCategory.equals("한도 안내") ||
            consultingCategory.equals("가상계좌 안내") ||
            consultingCategory.equals("서비스 이용방법 안내") ||
            consultingCategory.equals("결제대금 안내") ||
            consultingCategory.equals("약관 안내") ||
            consultingCategory.equals("상품 안내")) {
            return "조회/안내";
        }
        
        // 2. 즉시처리
        if (consultingCategory.equals("도난/분실 신청/해제") ||
            consultingCategory.equals("승인취소/매출취소 안내") ||
            consultingCategory.equals("선결제/즉시출금") ||
            consultingCategory.equals("연체대금 즉시출금") ||
            consultingCategory.equals("결제일 안내/변경")) {
            return "즉시처리";
        }
        
        // 3. 변경/등록요청
        if (consultingCategory.equals("한도상향 접수/처리") ||
            consultingCategory.equals("결제계좌 안내/변경") ||
            consultingCategory.equals("포인트/마일리지 전환등록") ||
            consultingCategory.equals("증명서/확인서 발급") ||
            consultingCategory.equals("가상계좌 예약/취소")) {
            return "변경/등록요청";
        }
        
        // 4. 금융상품/대출연계
        if (consultingCategory.equals("단기카드대출 안내/실행") ||
            consultingCategory.equals("장기카드대출 안내") ||
            consultingCategory.equals("심사 진행사항 안내")) {
            return "금융상품/대출연계";
        }
        
        // 5. 정부/공공지원
        if (consultingCategory.equals("정부지원 바우처 (등유, 임신 등)") ||
            consultingCategory.equals("도시가스")) {
            return "정부/공공지원";
        }
        
        // 6. 이벤트/프로모션
        if (consultingCategory.equals("이벤트 안내")) {
            return "이벤트/프로모션";
        }
        
        // 7. 약정관리
        if (consultingCategory.equals("일부결제 대금이월약정 안내") ||
            consultingCategory.equals("일부결제대금이월약정 해지")) {
            return "약정관리";
        }
        
        // 매핑되지 않은 카테고리는 기타로 분류
        return "기타";
    }
}
