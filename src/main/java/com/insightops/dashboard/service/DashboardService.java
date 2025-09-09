package com.insightops.dashboard.service;

import com.insightops.dashboard.client.MailServiceClient;
import com.insightops.dashboard.client.NormalizationServiceClient;
import com.insightops.dashboard.domain.InsightCard;
import com.insightops.dashboard.domain.MessagePreviewCache;
import com.insightops.dashboard.dto.*;
import com.insightops.dashboard.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 서비스 - 인사이트 & 메일 관련 로직
 * 집계 쿼리는 DashboardQueryService로 분리
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AggTotalRepository aggTotalRepo;
    private final AggByBigCategoryRepository aggBigRepo;
    private final AggBySmallAgeGenderRepository aggSmallRepo;
    private final InsightCardRepository insightRepo;
    private final MessagePreviewCacheRepository messageRepo;
    private final VocEventRepository vocEventRepo;
    private final VocSummaryRepository vocSummaryRepo;
    
    // 외부 서비스 클라이언트
    private final NormalizationServiceClient normalizationClient;
    private final MailServiceClient mailClient;
    
    public DashboardService(AggTotalRepository aggTotalRepo,
                           AggByBigCategoryRepository aggBigRepo,
                           AggBySmallAgeGenderRepository aggSmallRepo,
                           InsightCardRepository insightRepo,
                           MessagePreviewCacheRepository messageRepo,
                           VocEventRepository vocEventRepo,
                           VocSummaryRepository vocSummaryRepo,
                           NormalizationServiceClient normalizationClient,
                           MailServiceClient mailClient) {
        this.aggTotalRepo = aggTotalRepo;
        this.aggBigRepo = aggBigRepo;
        this.aggSmallRepo = aggSmallRepo;
        this.insightRepo = insightRepo;
        this.messageRepo = messageRepo;
        this.vocEventRepo = vocEventRepo;
        this.vocSummaryRepo = vocSummaryRepo;
        this.normalizationClient = normalizationClient;
        this.mailClient = mailClient;
    }

    /**
     * A. 오버뷰 화면 데이터 조회 (로컬 캐시 사용)
     */
    public OverviewDto getOverview(YearMonth yearMonth) {
        LocalDate first = yearMonth.atDay(1);
        LocalDate prevFirst = yearMonth.minusMonths(1).atDay(1);

        // 이달 VoC 건수 (로컬 집계 캐시)
        long thisCnt = aggTotalRepo.findMonthlyTotal(first).orElse(0L);
        // 전달 VoC 건수 (로컬 집계 캐시)
        long prevCnt = aggTotalRepo.findMonthlyTotal(prevFirst).orElse(0L);
        
        // 증감률 계산
        double delta = (prevCnt == 0) ? 100.0 : ((double)(thisCnt - prevCnt) / prevCnt) * 100.0;

        // Top Small 카테고리 조회 (로컬 집계 캐시)
        var top = aggSmallRepo.findTopSmallOfMonth(first)
                .map(t -> Map.entry(t.getSmallName(), 
                    (double)t.getCnt() / Math.max(1.0, t.getTotalCnt()) * 100.0))
                .orElse(Map.entry("-", 0.0));

        return new OverviewDto(thisCnt, prevCnt, delta, top.getKey(), top.getValue());
    }

    /**
     * B. Big 카테고리 비중 데이터 조회 (파이차트용) - 로컬 캐시
     */
    public List<ShareItem> getBigCategoryShare(String granularity, LocalDate from, LocalDate to) {
        var rows = aggBigRepo.findShare(granularity, from, to);
        long total = rows.stream().mapToLong(AggByBigCategoryRepository.ShareRow::getCnt).sum();
        
        return rows.stream()
            .map(r -> new ShareItem(
                r.getName(), 
                r.getCnt(), 
                total == 0 ? 0.0 : (r.getCnt() * 100.0 / total)
            ))
            .toList();
    }

    /**
     * C. 전체 VoC 변화량 시계열 데이터 조회 (라인차트용) - 로컬 캐시
     */
    public List<SeriesPoint> getTotalSeries(String granularity, LocalDate from, LocalDate to) {
        return aggTotalRepo.findSeries(granularity, from, to).stream()
            .map(p -> new SeriesPoint(p.getBucketStart(), p.getTotalCount()))
            .toList();
    }

    /**
     * D. Small 카테고리 트렌드 분석 (필터: 연령, 성별) - 로컬 캐시
     */
    public List<SmallTrendItem> getSmallTrends(String granularity, LocalDate from, LocalDate to,
                                               String clientAge, String clientGender, int limit) {
        return aggSmallRepo.findSmallTrends(granularity, from, to, clientAge, clientGender, limit).stream()
            .map(r -> new SmallTrendItem(r.getSmallName(), r.getCnt()))
            .toList();
    }

    /**
     * E. 인사이트 카드 Top 10 조회 - 로컬 캐시
     */
    public List<InsightCard> getInsights() {
        return insightRepo.findTop10ByOrderByScoreDesc();
    }

    /**
     * F. 상담 사례 목록 + 요약 조회 - 로컬 데이터 조회
     */
    public List<CaseItem> getCases(LocalDate from, LocalDate to, Long consultingCategoryId, int page, int size) {
        int offset = page * size;
        
        // VocEvent 조회
        var vocEvents = vocEventRepo.findVocEvents(from, to, consultingCategoryId, offset, size);
        
        return vocEvents.stream()
            .map(ve -> {
                // VocSummary 조회
                var summary = vocSummaryRepo.findByVocEventId(ve.getVocEventId())
                    .map(vs -> vs.getSummaryText())
                    .orElse("요약 정보 없음");
                
                return new CaseItem(
                    ve.getVocEventId(),
                    ve.getSourceId(),
                    ve.getConsultingDate().toString(),
                    ve.getBigCategoryName(),
                    ve.getConsultingCategoryName(),
                    ve.getClientAge(),
                    ve.getClientGender(),
                    summary
                );
            })
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
    public MailPreviewDto generateMailPreview(Long vocEventId) {
        return mailClient.generateMailPreview(vocEventId);
    }
    
    /**
     * I. 메일 발송 (외부 Mail Service 호출)
     */
    public void sendMail(MailSendRequestDto request) {
        mailClient.sendMail(request);
    }
}