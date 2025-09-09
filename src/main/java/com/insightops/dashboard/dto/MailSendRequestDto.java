package com.insightops.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메일 발송 요청 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailSendRequestDto {
    
    private Long vocEventId;
    private String toEmail;
    private String subject;
    private String bodyHtml;
    private String bodyMarkdown;
    private Long smallCategoryId;
}
