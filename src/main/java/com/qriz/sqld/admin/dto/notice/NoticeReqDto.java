package com.qriz.sqld.admin.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class NoticeReqDto {
    
    @Getter
    public static class GetNoticeReqDto {
        private Long noticeId;
    }

    @Setter
    @AllArgsConstructor
    public static class PostNoticeReqDto {
        private String title;
        private String content;
    }
}
