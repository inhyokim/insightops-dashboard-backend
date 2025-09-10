package com.insightops.dashboard.client;

import com.insightops.dashboard.dto.AssigneeDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Admin 서비스와 통신하는 HTTP 클라이언트
 * admin DB의 assignee 테이블에서 담당자 정보를 가져옴
 */
@Component
public class AdminServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${external.admin-service.base-url:http://localhost:8004}")
    private String adminServiceUrl;
    
    public AdminServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * 모든 담당자 정보 조회
     */
    public List<AssigneeDto> getAllAssignees() {
        try {
            String url = adminServiceUrl + "/api/assignees";
            
            var response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<AssigneeDto>>() {}
            );
            
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
            
        } catch (RestClientException e) {
            // 로그 기록 후 빈 리스트 반환
            System.err.println("Failed to fetch assignees from admin service: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * 특정 카테고리의 담당자 정보 조회
     */
    public Optional<AssigneeDto> getAssigneeByCategory(String consultingCategory) {
        try {
            String url = adminServiceUrl + "/api/assignees/category/" + consultingCategory;
            
            var response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                AssigneeDto.class
            );
            
            return Optional.ofNullable(response.getBody());
            
        } catch (RestClientException e) {
            // 로그 기록 후 빈 Optional 반환
            System.err.println("Failed to fetch assignee for category " + consultingCategory + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
