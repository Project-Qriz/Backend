package com.qriz.sqld.dto.daily;

import lombok.Getter;

public class DailyContentDto {
    
    @Getter
    public static class DailyStudyConceptDto {
        private String type;
        private String keyConcepts;
        private String description;
    }
}
