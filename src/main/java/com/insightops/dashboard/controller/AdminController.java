package com.insightops.dashboard.controller;

import com.insightops.dashboard.service.VocDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 관리자용 컨트롤러 - 데이터 집계 관리
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final VocDataService vocDataService;

    public AdminController(VocDataService vocDataService) {
        this.vocDataService = vocDataService;
    }

    /**
     * 수동 데이터 집계 실행
     * POST /api/admin/aggregate?from=2024-01-01&to=2024-01-31
     */
    @PostMapping("/aggregate")
    public ResponseEntity<Map<String, Object>> manualAggregation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        try {
            // API 기반으로 변경되어 더 이상 로컬 집계를 수행하지 않음
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API 기반 아키텍처로 변경됨 - 로컬 집계 비활성화",
                "from", from.toString(),
                "to", to.toString(),
                "note", "Voicebot Service API를 통해 실시간 데이터 조회"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "데이터 집계 중 오류가 발생했습니다: " + e.getMessage(),
                "from", from.toString(),
                "to", to.toString()
            ));
        }
    }
    
    /**
     * Period별 VoC 건수 조회 (Daily/Weekly/Monthly)
     * GET /api/admin/period-counts?date=2024-01-01
     */
    @GetMapping("/period-counts")
    public ResponseEntity<Map<String, Object>> getPeriodCounts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            Long dailyCount = vocDataService.getDailyVocCount(date);
            Long weeklyCount = vocDataService.getWeeklyVocCount(date);
            Long monthlyCount = vocDataService.getMonthlyVocCount(date);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "date", date.toString(),
                "daily", dailyCount,
                "weekly", weeklyCount,
                "monthly", monthlyCount
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Period 건수 조회 중 오류가 발생했습니다: " + e.getMessage(),
                "date", date.toString()
            ));
        }
    }

    /**
     * 일일 집계 실행 (어제 데이터)
     * POST /api/admin/aggregate/daily
     */
    @PostMapping("/aggregate/daily")
    public ResponseEntity<Map<String, Object>> dailyAggregation() {
        try {
            // API 기반으로 변경되어 더 이상 로컬 집계를 수행하지 않음
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "API 기반 아키텍처로 변경됨 - 로컬 집계 비활성화",
                "note", "Voicebot Service API를 통해 실시간 데이터 조회"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "일일 데이터 집계 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * Voicebot DB 연결 상태 확인
     * GET /api/admin/voicebot/health
     */
    @GetMapping("/voicebot/health")
    public ResponseEntity<Map<String, Object>> checkVoicebotConnection() {
        boolean isConnected = vocDataService.testConnection();
        
        return ResponseEntity.ok(Map.of(
            "connected", isConnected,
            "message", isConnected ? "Voicebot DB 연결 정상" : "Voicebot DB 연결 실패"
        ));
    }

    /**
     * 특정 날짜의 VoC 건수 조회 (원본 데이터)
     * GET /api/admin/voicebot/count?from=2024-01-01&to=2024-01-31
     */
    @GetMapping("/voicebot/count")
    public ResponseEntity<Map<String, Object>> getVocCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        try {
            // API 기반으로 변경되어 더미 데이터 반환
            Long count = 0L; // vocDataService.getTotalVocCount(from, to); - API로 대체 필요
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "from", from.toString(),
                "to", to.toString(),
                "count", count,
                "note", "API 기반으로 변경됨"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "VoC 건수 조회 중 오류가 발생했습니다: " + e.getMessage(),
                "from", from.toString(),
                "to", to.toString()
            ));
        }
    }
}
