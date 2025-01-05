package com.qriz.sqld.dto.survey;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyReqDto {
    @NotNull(message = "Key concepts must not be null")
    private List<String> keyConcepts;

    public void setKeyConcepts(List<String> keyConcepts) {
        this.keyConcepts = keyConcepts.stream()
                .map(String::trim) // 앞뒤 공백 제거
                .map(str -> str.replaceAll("\\s+", " ")) // 연속된 공백을 하나로
                .collect(Collectors.toList());
    }
}