package com.qriz.sqld.dto.exam;

import java.util.List;
import com.qriz.sqld.dto.test.TestRespDto;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class ExamTestResult {
    private List<TestRespDto.ExamRespDto> questions;
    private int totalTimeLimit;
}