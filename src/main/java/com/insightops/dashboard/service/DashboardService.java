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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 대시보드 서비스 - MSA 구조에 맞게 외부 API 호출
 * insightops_dashboard DB는 집계/캐시 전용
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

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
        
        try {
            // 새로운 스키마에서 최신 집계 데이터 조회
            var overviewData = aggTotalRepo.findLatestByPeriodType(period);
            
            if (overviewData.isPresent()) {
                var data = overviewData.get();
                
                // Top 카테고리 조회 (기존 로직 유지 - 월별 기준)
                LocalDate now = LocalDate.now();
                LocalDate firstDay = now.withDayOfMonth(1);
                var topSmall = aggCategoryRepo.findTopSmallOfMonth(firstDay);
                String topCategory = topSmall.map(row -> row.getSmallName()).orElse("정보 없음");
                double topRatio = topSmall.map(row ->
                    row.getTotalCnt() == 0 ? 0.0 : (double)row.getCnt() / row.getTotalCnt() * 100.0
                ).orElse(0.0);
                
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
            System.err.println("오버뷰 데이터 조회 중 오류: " + e.getMessage());
            // 오류 발생 시 기본값 반환
            return new OverviewDto(0L, 0L, 0.0, "정보 없음", 0.0);
        }
    }
    
    /**
     * A. 오버뷰 화면 데이터 조회 (기존 호환성을 위한 deprecated 메서드)
     */
    @Deprecated
    public OverviewDto getOverview(YearMonth yearMonth) {
        return getOverview("monthly");
    }

    /**
     * B. Big 카테고리 비중 데이터 조회 (파이차트용) - 로컬 집계 캐시
     */
    public List<ShareItem> getBigCategoryShare(String granularity, LocalDate from, LocalDate to) {
        // agg_by_category_age_gender에서 consulting_category별 집계를 조회
        var categoryTrends = aggCategoryRepo.findSmallTrends(granularity, from, to, null, null, 100);
        
        // Small Category를 Big Category로 그룹핑하여 집계
        Map<String, Long> bigCategoryMap = categoryTrends.stream()
            .collect(Collectors.groupingBy(
                row -> mapSmallToBigCategory(row.getSmallName()),
                Collectors.summingLong(AggByCategoryAgeGenderRepository.SmallTrendRow::getCnt)
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
    }

    /**
     * C. 전체 VoC 변화량 시계열 데이터 조회 (라인차트용) - 로컬 집계 캐시
     */
    public List<SeriesPoint> getTotalSeries(String granularity, LocalDate from, LocalDate to) {
        var points = aggTotalRepo.findSeries(granularity, from, to);
        
        return points.stream()
            .map(point -> new SeriesPoint(point.getBucketStart(), point.getTotalCount()))
            .toList();
    }

    /**
     * D. Small 카테고리 트렌드 분석 (필터: 연령, 성별) - 로컬 집계 캐시
     */
    public List<SmallTrendItem> getSmallTrends(String granularity, LocalDate from, LocalDate to,
                                               String clientAge, String clientGender, int limit) {
        var trends = aggCategoryRepo.findSmallTrends(granularity, from, to, clientAge, clientGender, limit);
        
        return trends.stream()
            .map(row -> new SmallTrendItem(row.getSmallName(), row.getCnt()))
            .toList();
    }

    /**
     * E. 인사이트 카드 Top 10 조회 - 로컬 캐시
     */
    public List<InsightCard> getInsights() {
        return insightRepo.findTop10ByOrderByScoreDesc();
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
