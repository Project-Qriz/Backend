package com.qriz.sqld.dto.application;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

public class ApplicationReqDto {
    
    @Getter
    @Setter
    public static class ApplyReqDto {
        private Long applyId;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private String location;
    }
}
