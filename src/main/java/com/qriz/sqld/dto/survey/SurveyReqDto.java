package com.qriz.sqld.dto.survey;

import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyReqDto {
    @NotNull(message = "Key concepts must not be null")
    private List<String> keyConcepts;

    public void setKeyConcepts(List<String> keyConcepts) {
        this.keyConcepts = keyConcepts;
    }
}
