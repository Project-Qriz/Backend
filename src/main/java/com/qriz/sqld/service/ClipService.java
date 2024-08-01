package com.qriz.sqld.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.clip.ClipRepository;
import com.qriz.sqld.domain.clip.Clipped;
import com.qriz.sqld.domain.userActivity.UserActivity;
import com.qriz.sqld.domain.userActivity.UserActivityRepository;
import com.qriz.sqld.dto.clip.ClipReqDto;
import com.qriz.sqld.dto.clip.ClipRespDto;
import com.qriz.sqld.dto.daily.DailyResultDetailDto;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClipService {
    
    private final ClipRepository clipRepository;
    private final UserActivityRepository userActivityRepository;
    private final DailyService dailyService;

    @Transactional
    public void clipQuestion(Long userId, ClipReqDto clipReqDto) {
        UserActivity userActivity = userActivityRepository.findById(clipReqDto.getActivityId())
                .orElseThrow(() -> new CustomApiException("해당 문제 풀이 기록을 찾을 수 없습니다."));

        if (!userActivity.getUser().getId().equals(userId)) {
            throw new CustomApiException("자신의 문제 풀이만 오답노트에 등록할 수 있습니다.");
        }

        if (clipRepository.existsByUserActivity_Id(clipReqDto.getActivityId())) {
            throw new CustomApiException("이미 오답노트에 등록된 문제입니다.");
        }

        Clipped clipped = new Clipped();
        clipped.setUserActivity(userActivity);
        clipped.setDate(LocalDateTime.now());

        clipRepository.save(clipped);
    }

    @Transactional(readOnly = true)
    public List<ClipRespDto> getClippedQuestions(Long userId) {
        List<Clipped> clippedList = clipRepository.findByUserActivity_User_Id(userId);

        return clippedList.stream()
                .map(ClipRespDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void unclipQuestion(Long userId, Long clipId) {
        Clipped clipped = clipRepository.findById(clipId)
                .orElseThrow(() -> new CustomApiException("해당 오답노트 기록을 찾을 수 없습니다."));

        if (!clipped.getUserActivity().getUser().getId().equals(userId)) {
            throw new CustomApiException("자신의 오답노트 기록만 삭제할 수 있습니다.");
        }

        clipRepository.delete(clipped);
    }

    @Transactional(readOnly = true)
    public DailyResultDetailDto getClippedQuestionDetail(Long userId, Long clipId) {
        Clipped clipped = clipRepository.findById(clipId)
                .orElseThrow(() -> new CustomApiException("해당 오답노트 기록을 찾을 수 없습니다."));

        if (!clipped.getUserActivity().getUser().getId().equals(userId)) {
            throw new CustomApiException("자신의 오답노트 기록만 조회할 수 있습니다.");
        }

        UserActivity userActivity = clipped.getUserActivity();
        String dayNumber = userActivity.getTestInfo().replace("데일리 테스트 - ", "");
        
        return dailyService.getDailyResultDetail(userId, dayNumber, userActivity.getQuestion().getId());
    }
}
