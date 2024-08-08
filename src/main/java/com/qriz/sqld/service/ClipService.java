package com.qriz.sqld.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.clip.ClipRepository;
import com.qriz.sqld.domain.clip.Clipped;
import com.qriz.sqld.domain.question.Question;
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

    private final Logger log = LoggerFactory.getLogger(ClipService.class);

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
        log.info("Fetching clipped question detail for userId: {} and clipId: {}", userId, clipId);

        Clipped clipped = clipRepository.findById(clipId)
                .orElseThrow(() -> {
                    log.error("Clip not found for id: {}", clipId);
                    return new CustomApiException("해당 오답노트 기록을 찾을 수 없습니다.");
                });

        log.info("Clipped entity found: {}", clipped);

        if (!clipped.getUserActivity().getUser().getId().equals(userId)) {
            log.error("User {} attempted to access clip {} which belongs to user {}", userId, clipId,
                    clipped.getUserActivity().getUser().getId());
            throw new CustomApiException("자신의 오답노트 기록만 조회할 수 있습니다.");
        }

        UserActivity userActivity = clipped.getUserActivity();
        if (userActivity == null) {
            log.error("UserActivity is null for clip: {}", clipId);
            throw new CustomApiException("해당 문제의 풀이 기록을 찾을 수 없습니다.");
        }

        log.info("UserActivity found: {}", userActivity);

        Question question = userActivity.getQuestion();
        if (question == null) {
            log.error("Question is null for UserActivity: {}", userActivity.getId());
            throw new CustomApiException("해당 문제를 찾을 수 없습니다.");
        }

        log.info("Question found: {}", question);

        String testInfo = userActivity.getTestInfo();
        String dayNumber = testInfo.replace("데일리 테스트 - ", "");
        log.info("Extracted day number: {}", dayNumber);

        try {
            return dailyService.getDailyResultDetail(userId, dayNumber, question.getId());
        } catch (Exception e) {
            log.error("Error getting daily result detail for user: {}, day: {}, question: {}", userId, dayNumber,
                    question.getId(), e);
            throw new CustomApiException("해당 문제의 풀이 결과를 찾을 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<ClipRespDto> getFilteredClippedQuestions(Long userId, List<String> keyConcepts, boolean onlyIncorrect,
            Integer category) {
        List<Clipped> clippedList;
        if (keyConcepts == null || keyConcepts.isEmpty()) {
            if (onlyIncorrect) {
                if (category == null) {
                    clippedList = clipRepository.findIncorrectByUserId(userId);
                } else {
                    clippedList = clipRepository.findIncorrectByUserIdAndCategory(userId, category);
                }
            } else {
                if (category == null) {
                    clippedList = clipRepository.findByUserActivity_User_IdOrderByDateDesc(userId);
                } else {
                    clippedList = clipRepository.findByUserIdAndCategory(userId, category);
                }
            }
        } else {
            if (onlyIncorrect) {
                if (category == null) {
                    clippedList = clipRepository.findIncorrectByUserIdAndKeyConcepts(userId, keyConcepts);
                } else {
                    clippedList = clipRepository.findIncorrectByUserIdAndKeyConceptsAndCategory(userId, keyConcepts,
                            category);
                }
            } else {
                if (category == null) {
                    clippedList = clipRepository.findByUserIdAndKeyConcepts(userId, keyConcepts);
                } else {
                    clippedList = clipRepository.findByUserIdAndKeyConceptsAndCategory(userId, keyConcepts, category);
                }
            }
        }
        return clippedList.stream()
                .map(ClipRespDto::new)
                .collect(Collectors.toList());
    }
}
