package com.qriz.sqld.admin.dto.notice;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class NoticeRespDto {
    
    @Getter
    @AllArgsConstructor
    public static class getNoticeRespDto {
        private String title;
        private String content;
        private LocalDateTime createAt;
    }
}
