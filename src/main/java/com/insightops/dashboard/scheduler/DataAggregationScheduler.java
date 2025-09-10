package com.insightops.dashboard.scheduler;

import com.insightops.dashboard.domain.AggByCategoryAgeGender;
import com.insightops.dashboard.domain.AggTotal;
import com.insightops.dashboard.domain.VocListCache;
import com.insightops.dashboard.repository.AggByCategoryAgeGenderRepository;
import com.insightops.dashboard.repository.AggTotalRepository;
import com.insightops.dashboard.repository.VocListCacheRepository;
import com.insightops.dashboard.service.VocDataService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 매일 자정에 실행되는 데이터 집계 스케줄러
 * voicebot DB의 voc_raw 테이블에서 데이터를 읽어 집계 테이블에 저장
 */
@Component
@ConditionalOnProperty(value = "scheduler.data-aggregation.enabled", havingValue = "true", matchIfMissing = true)
public class DataAggregationScheduler {

    private final VocDataService vocDataService;
    private final AggTotalRepository aggTotalRepo;
    private final AggByCategoryAgeGenderRepository aggCategoryRepo;
    private final VocListCacheRepository vocListRepo;

    public DataAggregationScheduler(VocDataService vocDataService,
                                   AggTotalRepository aggTotalRepo,
                                   AggByCategoryAgeGenderRepository aggCategoryRepo,
                                   VocListCacheRepository vocListRepo) {
        this.vocDataService = vocDataService;
        this.aggTotalRepo = aggTotalRepo;
        this.aggCategoryRepo = aggCategoryRepo;
        this.vocListRepo = vocListRepo;
    }

    /**
     * 매일 자정에 실행 - daily/weekly/monthly 집계 모두 수행
     */
    @Scheduled(cron = "${scheduler.data-aggregation.cron:0 0 0 * * ?}")
    @Transactional
    public void aggregateVocData() {
        System.out.println("=== 데이터 집계 스케줄러 시작 ===");
        
        // voicebot DB 연결 테스트
        if (!vocDataService.testConnection()) {
            System.err.println("Voicebot DB 연결 실패 - 집계 작업 중단");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        try {
            // 1. Daily/Weekly/Monthly 총 집계 (매일 실행)
            aggregatePeriodTotals(yesterday);
            
            // 2. 카테고리별 집계 데이터 생성 (기존 로직 유지)
            aggregateCategoryData("day", yesterday, yesterday);
            
            if (today.getDayOfWeek().getValue() == 1) { // 주별 집계 (월요일)
                aggregateCategoryData("week", yesterday.minusDays(6), yesterday);
            }
            
            if (today.getDayOfMonth() == 1) { // 월별 집계 (매월 1일)
                LocalDate lastMonthStart = yesterday.withDayOfMonth(1);
                aggregateCategoryData("month", lastMonthStart, yesterday);
            }
            
            // 3. VoC 리스트 캐시 업데이트
            updateVocListCache(yesterday, yesterday);
            
            System.out.println("=== 데이터 집계 스케줄러 완료 ===");
            
        } catch (Exception e) {
            System.err.println("데이터 집계 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 매일 실행되는 Period 별 집계 (Daily/Weekly/Monthly)
     */
    private void aggregatePeriodTotals(LocalDate baseDate) {
        System.out.println("Period 집계 시작 - 기준일: " + baseDate);
        
        // Daily 집계 (최근 1일)
        Long dailyCount = vocDataService.getDailyVocCount(baseDate);
        Long prevDailyCount = vocDataService.getDailyVocCount(baseDate.minusDays(1));
        saveOrUpdateAggTotal("daily", baseDate, dailyCount, prevDailyCount);
        
        // Weekly 집계 (최근 7일)
        Long weeklyCount = vocDataService.getWeeklyVocCount(baseDate);
        Long prevWeeklyCount = vocDataService.getWeeklyVocCount(baseDate.minusDays(7));
        saveOrUpdateAggTotal("weekly", baseDate, weeklyCount, prevWeeklyCount);
        
        // Monthly 집계 (최근 30일)
        Long monthlyCount = vocDataService.getMonthlyVocCount(baseDate);
        Long prevMonthlyCount = vocDataService.getMonthlyVocCount(baseDate.minusDays(30));
        saveOrUpdateAggTotal("monthly", baseDate, monthlyCount, prevMonthlyCount);
        
        System.out.println("Period 집계 완료 - Daily: " + dailyCount + ", Weekly: " + weeklyCount + ", Monthly: " + monthlyCount);
    }
    
    /**
     * AggTotal 데이터 저장 또는 업데이트
     */
    private void saveOrUpdateAggTotal(String periodType, LocalDate aggregationDate, Long totalCount, Long prevCount) {
        Optional<AggTotal> existing = aggTotalRepo.findByPeriodTypeAndAggregationDate(periodType, aggregationDate);
        
        AggTotal aggTotal;
        if (existing.isPresent()) {
            aggTotal = existing.get();
            System.out.println("기존 " + periodType + " 집계 업데이트: " + aggregationDate);
        } else {
            aggTotal = new AggTotal();
            aggTotal.setPeriodType(periodType);
            aggTotal.setAggregationDate(aggregationDate);
            System.out.println("새로운 " + periodType + " 집계 생성: " + aggregationDate);
        }
        
        aggTotal.setTotalCount(totalCount);
        aggTotal.setPrevCount(prevCount);
        
        aggTotalRepo.save(aggTotal);
    }

    /**
     * 수동 실행용 메서드 (테스트/초기화 용도)
     */
    public void aggregateDataForDateRange(LocalDate from, LocalDate to) {
        System.out.println("=== 수동 데이터 집계 시작: " + from + " ~ " + to + " ===");
        
        try {
            // 각 날짜별로 Daily/Weekly/Monthly 집계
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                aggregatePeriodTotals(date);
                aggregateCategoryData("day", date, date);
                updateVocListCache(date, date);
            }
            
            // 주별/월별 카테고리 집계는 범위에 따라 실행 (기존 로직 유지)
            aggregateCategoryData("week", from, to);
            aggregateCategoryData("month", from, to);
            
            System.out.println("=== 수동 데이터 집계 완료 ===");
            
        } catch (Exception e) {
            System.err.println("수동 데이터 집계 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // === 기존 호환성을 위한 deprecated 메서드들 ===
    
    @Deprecated
    private void aggregateDailyTotal(LocalDate date) {
        // 새로운 방식으로 리다이렉트
        aggregatePeriodTotals(date);
    }

    @Deprecated
    private void aggregateWeeklyTotal(LocalDate from, LocalDate to) {
        // 새로운 방식으로 리다이렉트
        aggregatePeriodTotals(to);
    }

    @Deprecated
    private void aggregateMonthlyTotal(LocalDate from, LocalDate to) {
        // 새로운 방식으로 리다이렉트
        aggregatePeriodTotals(to);
    }

    private void aggregateCategoryData(String granularity, LocalDate from, LocalDate to) {
        // TODO: API 기반으로 변경 후 수정 필요
        // List<Map<String, Object>> categoryData = vocDataService.getCategoryAggregation(granularity, from, to);
        List<Map<String, Object>> categoryData = List.of(); // 임시 비어있는 리스트
        
        for (Map<String, Object> row : categoryData) {
            AggByCategoryAgeGender agg = new AggByCategoryAgeGender();
            agg.setGranularity(granularity);
            agg.setBucketStart((LocalDate) row.get("bucket_date"));
            agg.setConsultingCategory((String) row.get("consulting_category"));
            agg.setClientAge((String) row.get("client_age"));
            agg.setClientGender((String) row.get("client_gender"));
            agg.setCount(((Number) row.get("count")).longValue());
            
            aggCategoryRepo.save(agg);
        }
        
        System.out.println(granularity + " 카테고리 집계 저장: " + categoryData.size() + "개 레코드");
    }

    private void updateVocListCache(LocalDate from, LocalDate to) {
        // 기존 캐시 데이터 삭제 (해당 날짜 범위)
        List<VocListCache> existingData = vocListRepo.findByConsultingDateBetweenOrderByConsultingDateDesc(from, to);
        if (!existingData.isEmpty()) {
            vocListRepo.deleteAll(existingData);
            System.out.println("기존 VoC 리스트 캐시 삭제: " + existingData.size() + "개");
        }
        
        // 새로운 데이터 조회 및 저장
        // TODO: API 기반으로 변경 후 수정 필요
        // List<Map<String, Object>> vocListData = vocDataService.getVocListForCache(from, to, 1000);
        List<Map<String, Object>> vocListData = List.of(); // 임시 비어있는 리스트
        
        for (Map<String, Object> row : vocListData) {
            VocListCache cache = new VocListCache();
            cache.setVocId((String) row.get("voc_id"));
            cache.setConsultingDate((LocalDate) row.get("consulting_date"));
            cache.setConsultingCategory((String) row.get("consulting_category"));
            cache.setClientAge((String) row.get("client_age"));
            cache.setClientGender((String) row.get("client_gender"));
            cache.setSourceSystem((String) row.get("source_system"));
            cache.setSummaryText((String) row.get("summary_text"));
            cache.setCreatedAt((Instant) row.get("created_at"));
            cache.setUpdatedAt((Instant) row.get("updated_at"));
            
            vocListRepo.save(cache);
        }
        
        System.out.println("VoC 리스트 캐시 저장: " + vocListData.size() + "개 레코드");
    }
}
