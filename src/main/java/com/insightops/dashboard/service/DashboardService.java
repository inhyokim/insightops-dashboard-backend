package com.insightops.dashboard.service;

import com.insightops.dashboard.client.NormalizationServiceClient;
import com.insightops.dashboard.client.MailServiceClient;
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

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AggTotalRepository aggTotalRepo;
    private final AggByBigCategoryRepository aggBigRepo;
    private final AggBySmallAgeGenderRepository aggSmallRepo;
    private final InsightCardRepository insightRepo;
    private final MessagePreviewCacheRepository messageRepo;
    
    // 외부 서비스 클라이언트 추가
    private final NormalizationServiceClient normalizationClient;
    private final MailServiceClient mailClient;
    
    public DashboardService(AggTotalRepository aggTotalRepo,
                           AggByBigCategoryRepository aggBigRepo,
                           AggBySmallAgeGenderRepository aggSmallRepo,
                           InsightCardRepository insightRepo,
                           MessagePreviewCacheRepository messageRepo,
                           NormalizationServiceClient normalizationClient,
                           MailServiceClient mailClient) {
        this.aggTotalRepo = aggTotalRepo;
        this.aggBigRepo = aggBigRepo;
        this.aggSmallRepo = aggSmallRepo;
        this.insightRepo = insightRepo;
        this.messageRepo = messageRepo;
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
                                               String age, String gender, int limit) {
        
        return aggSmallRepo.findSmallTrends(granularity, from, to, age, gender, limit).stream()
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
     * F. 상담 사례 목록 + 요약 조회 - 외부 정규화 서비스 호출
     */
    public List<CaseItem> getCases(Instant from, Instant to, Long smallCategoryId, int page, int size) {
        // 외부 정규화 서비스에서 VoC 케이스 목록을 가져옴
        return normalizationClient.getVocCases(from, to, smallCategoryId, page, size);
    }

    /**
     * G. 메일 프리뷰 최근 50개 조회 - 로컬 캐시 + 외부 메일 서비스
     */
    public List<MessagePreviewCache> getRecentMessages() {
        // 로컬 캐시에서 먼저 조회
        List<MessagePreviewCache> cachedMessages = messageRepo.findTop50ByOrderByCreatedAtDesc();
        
        // 캐시가 비어있거나 적으면 외부 메일 서비스에서 최신 데이터 가져오기
        if (cachedMessages.isEmpty()) {
            // 외부 메일 서비스에서 최신 로그 가져와서 캐시 업데이트 로직
            // (현재는 캐시된 데이터만 반환)
        }
        
        return cachedMessages;
    }
    
    /**
     * H. 외부 서비스에서 집계 데이터 동기화 (스케줄러에서 호출)
     */
    @Transactional
    public void syncAggregationData(String granularity, String startDate, String endDate) {
        // 정규화 서비스에서 집계 데이터를 가져와서 로컬 캐시 테이블 업데이트
        Map<String, Object> aggregationData = normalizationClient.getAggregationData(granularity, startDate, endDate);
        
        // TODO: aggregationData를 파싱해서 agg_* 테이블에 저장
        // 실제 구현에서는 받은 데이터를 각 집계 테이블에 INSERT/UPDATE
    }
    
    /**
     * I. 메일 발송 요청 - 외부 메일 서비스 호출
     */
    public boolean sendMail(Long smallCategoryId, String assigneeEmail, String subject, String body) {
        return mailClient.sendMail(smallCategoryId, assigneeEmail, subject, body);
    }
    
    /**
     * J. 메일 미리보기 생성 - 외부 메일 서비스 호출
     */
    public String generateMailPreview(Long smallCategoryId, String assigneeEmail) {
        return mailClient.generateMailPreview(smallCategoryId, assigneeEmail);
    }
}