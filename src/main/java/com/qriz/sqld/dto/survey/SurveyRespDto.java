package com.qriz.sqld.dto.survey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyRespDto {
    
    private Long userId;
    private Long skillId;
    private boolean checked;
    private boolean knowsNothing;

    public SurveyRespDto(Long userId, Long skillId, boolean checked, boolean knowsNothing) {
        this.userId = userId;
        this.skillId = skillId;
        this.checked = checked;
        this.knowsNothing = knowsNothing;
    }

    // "아무것도 모른다" 옵션을 위한 생성자
    public static SurveyRespDto createKnowsNothingResponse(Long userId) {
        return new SurveyRespDto(userId, null, false, true);
    }
}