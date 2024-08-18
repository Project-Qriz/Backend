package com.qriz.sqld.dto.preview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultDto {
    private List<ConceptResult> topMissedConcepts;
    private int totalQuestions;
    private int correctAnswers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConceptResult {
        private String keyConcepts;
        private int occurrences;
        private int incorrectAnswers;
    }
}