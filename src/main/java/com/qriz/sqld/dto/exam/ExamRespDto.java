package com.qriz.sqld.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class ExamRespDto {
    
    @Getter
    @AllArgsConstructor
    public static class SessionList {
        private boolean completed;
        private String session;
        private String totalScore;
    }
}
