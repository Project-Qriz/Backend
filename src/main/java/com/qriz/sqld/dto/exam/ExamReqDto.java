package com.qriz.sqld.dto.exam;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExamReqDto {
    private List<ExamSubmitReqDto> activities;

    @Getter
    @Setter
    public static class ExamSubmitReqDto {
        private Long userId;
        private QuestionReqDto question;
        private int questionNum;
        private String checked;

        @Getter
        @Setter
        public static class QuestionReqDto {
            private Long questionId;
            private int category;    
        }
    }
}
