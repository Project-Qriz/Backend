package com.qriz.sqld.dto.test;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TestReqDto {
    private List<TestSubmitReqDto> activities;

    @Getter
    @Setter
    public static class TestSubmitReqDto {
        private Long activityId;
        private Long userId;
        private QuestionReqDto question;
        private int questionNum;
        private String checked;
        private Integer timeSpent;

        @Getter
        @Setter
        public static class QuestionReqDto {
            private Long questionId;
            private int category;    
        }
    }
}
