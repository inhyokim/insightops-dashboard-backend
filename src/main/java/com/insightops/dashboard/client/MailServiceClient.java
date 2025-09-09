package com.insightops.dashboard.client;

import com.insightops.dashboard.domain.MessagePreviewCache;
import com.insightops.dashboard.dto.MailPreviewDto;
import com.insightops.dashboard.dto.MailSendRequestDto;
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
    public MailPreviewDto generateMailPreview(Long vocEventId) {
        try {
            String url = mailServiceUrl + "/api/mail/preview?vocEventId=" + vocEventId;
            
            var response = restTemplate.getForObject(url, MailPreviewDto.class);
            return response != null ? response : new MailPreviewDto();
            
        } catch (RestClientException e) {
            return new MailPreviewDto();
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
    public void sendMail(MailSendRequestDto request) {
        try {
            String url = mailServiceUrl + "/api/mail/send";
            restTemplate.postForEntity(url, request, Void.class);
            
        } catch (RestClientException e) {
            // 로깅 및 예외 처리
            throw new RuntimeException("메일 발송 실패", e);
        }
    }
    
    /**
     * 최근 메일 미리보기 캐시 조회
     */
    public List<MessagePreviewCache> getRecentMailPreviews() {
        try {
            String url = mailServiceUrl + "/api/mail/recent-previews";
            
            var response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MessagePreviewCache>>() {}
            );
            
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
            
        } catch (RestClientException e) {
            return Collections.emptyList();
        }
    }
}
