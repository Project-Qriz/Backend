package com.qriz.sqld.dto.survey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyRespDto {
    
    private Long userId;
    private Long skillId;
    private boolean checked;

    public SurveyRespDto(Long userId, Long skillId, boolean checked) {
        this.userId = userId;
        this.skillId = skillId;
        this.checked = checked;
    }
}
