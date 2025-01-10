package com.qriz.sqld.domain.preview;

public enum PreviewTestStatus {
    NOT_STARTED,            // 초기 상태
    SURVEY_COMPLETED,       // 설문조사 완료
    PREVIEW_SKIPPED,        // "아무것도 모른다" 선택으로 스킵
    PREVIEW_COMPLETED       // 프리뷰 테스트 완료
}
