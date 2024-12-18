package com.qriz.sqld.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.admin.domain.notice.Notice;
import com.qriz.sqld.admin.domain.notice.NoticeRepository;
import com.qriz.sqld.admin.dto.notice.NoticeRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeService {
    
    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public NoticeRespDto.getNoticeRespDto getNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
            .orElseThrow(() -> new CustomApiException("해당 공지사항을 찾을 수 없습니다."));

        return new NoticeRespDto.getNoticeRespDto(
            notice.getTitle(),
            notice.getContent(),
            notice.getCreateAt()
        );
    }
}
