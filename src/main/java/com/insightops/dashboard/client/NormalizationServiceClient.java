package com.insightops.dashboard.client;

import com.insightops.dashboard.dto.CaseItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 정규화 서비스와 통신하는 HTTP 클라이언트
 */
@Component
public class NormalizationServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${external.normalization-service.base-url:http://localhost:8001}")
    private String normalizationServiceUrl;
    
    public NormalizationServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * 정규화된 VoC 케이스 목록을 외부 서비스에서 가져옴
     */
    public List<CaseItem> getVocCases(Instant from, Instant to, Long smallCategoryId, int page, int size) {
        try {
            String url = normalizationServiceUrl + "/api/voc-events/normalized" +
                "?from=" + from.toString() +
                "&to=" + to.toString() +
                "&page=" + page +
                "&size=" + size;
            
            if (smallCategoryId != null) {
                url += "&smallCategoryId=" + smallCategoryId;
            }
            
            var response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CaseItem>>() {}
            );
            
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
            
        } catch (RestClientException e) {
            // 외부 서비스 장애 시 빈 목록 반환 (Circuit Breaker 패턴)
            return Collections.emptyList();
        }
    }
    
    /**
     * 특정 VoC 이벤트의 요약 정보를 가져옴
     */
    public String getVocSummary(Long vocEventId) {
        try {
            String url = normalizationServiceUrl + "/api/voc-events/" + vocEventId + "/summary";
            return restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            return "요약 정보를 가져올 수 없습니다.";
        }
    }
    
    /**
     * 정규화 서비스에서 집계 데이터를 가져와 로컬 캐시 업데이트
     */
    public Map<String, Object> getAggregationData(String granularity, String startDate, String endDate) {
        try {
            String url = normalizationServiceUrl + "/api/aggregations" +
                "?granularity=" + granularity +
                "&startDate=" + startDate +
                "&endDate=" + endDate;
            
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            return Collections.emptyMap();
        }
    }
}
