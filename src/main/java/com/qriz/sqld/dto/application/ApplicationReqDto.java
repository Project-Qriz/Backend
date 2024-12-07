package com.qriz.sqld.dto.application;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class ApplicationReqDto {

    @Getter
    @Setter
    @ToString
    public static class ApplyReqDto {
        @NotNull(message = "시험 ID는 필수입니다.")
        private Long applyId;
    }

    @Getter
    @Setter
    @ToString
    public static class ModifyReqDto {
        @NotNull(message = "변경할 시험 ID는 필수입니다.")
        private Long newApplyId;
    }
}
