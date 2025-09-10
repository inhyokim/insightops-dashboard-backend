package com.insightops.dashboard.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Voicebot 서비스와 통신하는 HTTP 클라이언트
 * voicebot.voc_raw 테이블에서 집계 데이터를 가져옴
 */
@Component
public class VoicebotServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${external.voicebot-service.base-url:http://localhost:8002}")
    private String voicebotServiceUrl;
    
    public VoicebotServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * 전체 VoC 집계 데이터 조회
     */
    public Map<String, Object> getTotalAggregation(String granularity, LocalDate from, LocalDate to) {
        try {
            String url = voicebotServiceUrl + "/api/aggregations/total" +
                "?granularity=" + granularity +
                "&from=" + from.toString() +
                "&to=" + to.toString();
            
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            return Collections.emptyMap();
        }
    }
    
    /**
     * 카테고리별 집계 데이터 조회
     */
    public List<Map<String, Object>> getCategoryAggregation(String granularity, LocalDate from, LocalDate to) {
        try {
            String url = voicebotServiceUrl + "/api/aggregations/category" +
                "?granularity=" + granularity +
                "&from=" + from.toString() +
                "&to=" + to.toString();
            
            var response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            return Collections.emptyList();
        }
    }
    
    /**
     * 카테고리+연령+성별별 집계 데이터 조회
     */
    public List<Map<String, Object>> getCategoryAgeGenderAggregation(String granularity, LocalDate from, LocalDate to) {
        try {
            String url = voicebotServiceUrl + "/api/aggregations/category-age-gender" +
                "?granularity=" + granularity +
                "&from=" + from.toString() +
                "&to=" + to.toString();
            
            var response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            return Collections.emptyList();
        }
    }
    
    /**
     * VoC 리스트 메타데이터 조회 (페이징)
     */
    public List<Map<String, Object>> getVocListMetadata(LocalDate from, LocalDate to, int page, int size) {
        try {
            String url = voicebotServiceUrl + "/api/voc-list" +
                "?from=" + from.toString() +
                "&to=" + to.toString() +
                "&page=" + page +
                "&size=" + size;
            
            var response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            return Collections.emptyList();
        }
    }
    
    /**
     * VoC 건수 요약 조회 (새로운 API)
     */
    public Map<String, Object> getVocCountSummary(Map<String, Object> request) {
        try {
            String url = voicebotServiceUrl + "/api/voc/count-summary";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            return restTemplate.postForObject(url, entity, Map.class);
        } catch (RestClientException e) {
            System.err.println("VoC count summary API 호출 실패: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * Voicebot 서비스 Health Check
     */
    public Map<String, Object> healthCheck() {
        try {
            String url = voicebotServiceUrl + "/health";
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            System.err.println("Voicebot 서비스 health check 실패: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
