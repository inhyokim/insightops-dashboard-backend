package com.insightops.dashboard.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 담당자 정보 DTO (외부 admin DB의 assignee 테이블에서 API 호출로 가져옴)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssigneeDto {

    private Long assigneeId;
    private String consultingCategory; // Small 카테고리명 (25개 중 하나)
    private String assigneeEmail;
    private String assigneeName;
    private String assigneeTeam;
    private String assigneePhone;
}
