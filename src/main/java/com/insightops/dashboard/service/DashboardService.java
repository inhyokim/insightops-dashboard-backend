package com.insightops.dashboard.service;

import com.insightops.dashboard.client.MailServiceClient;
import com.insightops.dashboard.client.NormalizationServiceClient;
import com.insightops.dashboard.client.VoicebotServiceClient;
import com.insightops.dashboard.domain.InsightCard;
import com.insightops.dashboard.domain.MessagePreviewCache;
import com.insightops.dashboard.dto.*;
import com.insightops.dashboard.repository.*;
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
    private final AggByBigCategoryRepository aggBigRepo;
    private final AggByCategoryAgeGenderRepository aggCategoryRepo;
    private final InsightCardRepository insightRepo;
    private final MessagePreviewCacheRepository messageRepo;
    private final VocListCacheRepository vocListRepo;
    
    // 외부 서비스 클라이언트
    private final VoicebotServiceClient voicebotClient;
    private final NormalizationServiceClient normalizationClient;
    private final MailServiceClient mailClient;
    
    public DashboardService(AggTotalRepository aggTotalRepo,
                           AggByBigCategoryRepository aggBigRepo,
                           AggByCategoryAgeGenderRepository aggCategoryRepo,
                           InsightCardRepository insightRepo,
                           MessagePreviewCacheRepository messageRepo,
                           VocListCacheRepository vocListRepo,
                           VoicebotServiceClient voicebotClient,
                           NormalizationServiceClient normalizationClient,
                           MailServiceClient mailClient) {
        this.aggTotalRepo = aggTotalRepo;
        this.aggBigRepo = aggBigRepo;
        this.aggCategoryRepo = aggCategoryRepo;
        this.insightRepo = insightRepo;
        this.messageRepo = messageRepo;
        this.vocListRepo = vocListRepo;
        this.voicebotClient = voicebotClient;
        this.normalizationClient = normalizationClient;
        this.mailClient = mailClient;
    }

    /**
     * A. 오버뷰 화면 데이터 조회 (로컬 집계 캐시 사용)
     */
    public OverviewDto getOverview(YearMonth yearMonth) {
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        LocalDate prevFirstDay = yearMonth.minusMonths(1).atDay(1);
        LocalDate prevLastDay = yearMonth.minusMonths(1).atEndOfMonth();
        
        // 이달 총 VoC 건수 (agg_total에서 조회)
        long thisCnt = aggTotalRepo.findTotalCountBetween(firstDay, lastDay).orElse(0L);
        // 지난달 총 VoC 건수
        long prevCnt = aggTotalRepo.findTotalCountBetween(prevFirstDay, prevLastDay).orElse(0L);
        
        // 증감률 계산
        double delta = prevCnt == 0 ? 0.0 : ((double)(thisCnt - prevCnt) / prevCnt) * 100.0;
        
        // Top 카테고리 조회 (agg_by_category_age_gender에서)
        var topSmall = aggCategoryRepo.findTopSmallOfMonth(firstDay);
        String topCategory = topSmall.map(row -> row.getSmallName()).orElse("정보 없음");
        double topRatio = topSmall.map(row -> 
            row.getTotalCnt() == 0 ? 0.0 : (double)row.getCnt() / row.getTotalCnt() * 100.0
        ).orElse(0.0);

        return new OverviewDto(thisCnt, prevCnt, delta, topCategory, topRatio);
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
        // 임시 더미 데이터
        return List.of(
            new SeriesPoint(LocalDate.of(2024, 1, 1), 100L),
            new SeriesPoint(LocalDate.of(2024, 1, 2), 120L),
            new SeriesPoint(LocalDate.of(2024, 1, 3), 110L),
            new SeriesPoint(LocalDate.of(2024, 1, 4), 150L)
        );
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
     * F. 상담 사례 목록 조회 - 임시 더미 데이터
     */
    public List<CaseItem> getCases(LocalDate from, LocalDate to, String consultingCategory, int page, int size) {
        // 임시 더미 데이터
        return List.of(
            new CaseItem(1L, "src-001", "2024-01-15", "조회/안내", "도난/분실 신청", "20대", "여성", "카드 분실 신고 접수"),
            new CaseItem(2L, "src-002", "2024-01-15", "즉시처리", "이용내역 안내", "30대", "남성", "최근 결제 내역 문의"),
            new CaseItem(3L, "src-003", "2024-01-14", "변경/등록요청", "한도 안내", "40대", "여성", "한도 상향 요청")
        );
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
