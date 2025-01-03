package com.qriz.sqld.dto.daily;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultDetailDto {
    private String skillName;
    private String question;
    private int qustionNum;
    private String description;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String answer;
    private String solution;
    private String checked;
    private boolean correction;
    private String testInfo;
    private String title;
    private String keyConcepts;
}
