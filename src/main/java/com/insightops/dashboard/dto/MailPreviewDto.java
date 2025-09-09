package com.insightops.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메일 미리보기 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailPreviewDto {
    
    private Long vocEventId;
    private String toEmail;
    private String subject;
    private String bodyHtml;
    private String bodyMarkdown;
}
