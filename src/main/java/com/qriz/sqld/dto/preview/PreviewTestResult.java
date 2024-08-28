package com.qriz.sqld.dto.preview;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PreviewTestResult {
    private List<QuestionDto> questions;
    private int totalTimeLimit;
}
