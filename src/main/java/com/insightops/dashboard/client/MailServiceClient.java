package com.insightops.dashboard.client;

import com.insightops.dashboard.domain.MessagePreviewCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 메일 시스템과 통신하는 HTTP 클라이언트
 */
@Component
public class MailServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${external.mail-service.base-url:http://localhost:8003}")
    private String mailServiceUrl;
    
    public MailServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * 메일 템플릿 미리보기를 외부 메일 서비스에서 가져옴
     */
    public String generateMailPreview(Long smallCategoryId, String assigneeEmail) {
        try {
            String url = mailServiceUrl + "/api/mail/preview" +
                "?smallCategoryId=" + smallCategoryId +
                "&assigneeEmail=" + assigneeEmail;
            
            var response = restTemplate.getForObject(url, Map.class);
            return response != null ? (String) response.get("preview") : "";
            
        } catch (RestClientException e) {
            return "메일 미리보기를 가져올 수 없습니다.";
        }
    }
    
    /**
     * 메일 발송 로그를 외부 서비스에서 가져옴
     */
    public List<Map<String, Object>> getRecentMailLogs() {
        try {
            String url = mailServiceUrl + "/api/mail/logs/recent?limit=50";
            
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
     * 메일 발송 요청
     */
    public boolean sendMail(Long smallCategoryId, String assigneeEmail, String subject, String body) {
        try {
            Map<String, Object> mailRequest = Map.of(
                "smallCategoryId", smallCategoryId,
                "assigneeEmail", assigneeEmail,
                "subject", subject,
                "body", body
            );
            
            String url = mailServiceUrl + "/api/mail/send";
            var response = restTemplate.postForObject(url, mailRequest, Map.class);
            
            return response != null && Boolean.TRUE.equals(response.get("success"));
            
        } catch (RestClientException e) {
            return false;
        }
    }
}
