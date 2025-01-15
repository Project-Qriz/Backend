package com.qriz.sqld.dto.clip;

import com.qriz.sqld.domain.clip.Clipped;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ClipRespDto {
    private Long id;
    private int questionNum;
    private String question;
    private boolean correction;
    private String keyConcepts;
    private LocalDateTime date;

    public ClipRespDto(Clipped clipped) {
        this.id = clipped.getId();
        this.questionNum = clipped.getUserActivity().getQuestionNum();
        this.question = clipped.getUserActivity().getQuestion().getQuestion();
        this.correction = clipped.getUserActivity().isCorrection();
        this.keyConcepts = clipped.getUserActivity().getQuestion().getSkill().getKeyConcepts();
        this.date = clipped.getDate();
    }

    @Getter
    @Setter
    @Builder
    public static class ClippedDaysDto {
        private List<String> days; 
    }

    @Getter
    @Setter
    @Builder
    public static class ClippedSessionsDto {
        private List<String> sessions;    
        private String latestSession;
    }
}