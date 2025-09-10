package com.insightops.dashboard.dto;

/**
 * 메일 초안 생성 요청 DTO
 */
public class MailGenerationRequestDto {
    private String vocId;
    
    public MailGenerationRequestDto() {}
    
    public MailGenerationRequestDto(String vocId) {
        this.vocId = vocId;
    }
    
    public String getVocId() { return vocId; }
    public void setVocId(String vocId) { this.vocId = vocId; }
}
