package com.qriz.sqld.dto.clip;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClipReqDto {
    private Long activityId;

    @Getter
    @Setter
    public static class ClipFilterReqDto {
        private List<String> keyConcepts;
    }
}
