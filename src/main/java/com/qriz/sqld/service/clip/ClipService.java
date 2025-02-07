package com.qriz.sqld.service.clip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.UserActivity.UserActivity;
import com.qriz.sqld.domain.UserActivity.UserActivityRepository;
import com.qriz.sqld.domain.clip.ClipRepository;
import com.qriz.sqld.domain.clip.Clipped;
import com.qriz.sqld.domain.daily.UserDaily;
import com.qriz.sqld.domain.daily.UserDailyRepository;
import com.qriz.sqld.domain.exam.UserExamSession;
import com.qriz.sqld.domain.exam.UserExamSessionRepository;
import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.dto.clip.ClipReqDto;
import com.qriz.sqld.dto.clip.ClipRespDto;
import com.qriz.sqld.dto.daily.ResultDetailDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.service.daily.DailyService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClipService {

    private final ClipRepository clipRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserExamSessionRepository userExamSessionRepository;
    private final QuestionRepository questionRepository;
    private final DailyService dailyService;
    private final UserDailyRepository userDailyRepository;

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

    /**
     * category에 따른 최신 testInfo의 문제들 조회
     */
    @Transactional(readOnly = true)
    public List<ClipRespDto> getClippedQuestions(
            Long userId,
            List<String> keyConcepts,
            boolean onlyIncorrect,
            Integer category,
            String testInfo) {

        // 현재 활성화된 플랜의 버전 조회
        List<UserDaily> currentPlan = userDailyRepository.findByUserIdAndIsArchivedFalse(userId);
        if (currentPlan.isEmpty()) {
            return Collections.emptyList();
        }
        int currentVersion = currentPlan.get(0).getPlanVersion();

        List<Clipped> clippedList;

        // testInfo 존재하는 경우
        if (testInfo != null) {
            clippedList = clipRepository.findByUserIdAndPlanVersion(userId, currentVersion)
                    .stream()
                    .filter(clip -> clip.getUserActivity().getTestInfo().equals(testInfo))
                    .collect(Collectors.toList());
        } else {
            clippedList = clipRepository.findByUserIdAndPlanVersion(userId, currentVersion);
        }

        // 기존 필터링 로직 유지
        if (onlyIncorrect) {
            clippedList = clippedList.stream()
                    .filter(clip -> !clip.getUserActivity().isCorrection())
                    .collect(Collectors.toList());
        }

        if (keyConcepts != null && !keyConcepts.isEmpty()) {
            clippedList = clippedList.stream()
                    .filter(clip -> keyConcepts.contains(
                            clip.getUserActivity().getQuestion().getSkill().getKeyConcepts()))
                    .collect(Collectors.toList());
        }

        return clippedList.stream()
                .map(ClipRespDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClipRespDto> getClippedQuestions(Long userId, List<String> keyConcepts, boolean onlyIncorrect,
            Integer category) {
        return getClippedQuestions(userId, keyConcepts, onlyIncorrect, category, null);
    }

    @Transactional(readOnly = true)
    public List<String> findAllTestInfosByUserId(Long userId) {
        return clipRepository.findDistinctTestInfosByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ClipRespDto> getFilteredClippedQuestions(
            Long userId,
            List<String> keyConcepts,
            boolean onlyIncorrect,
            Integer category,
            String testInfo) {

        // 현재 활성화된 플랜의 버전 조회
        List<UserDaily> currentPlan = userDailyRepository.findByUserIdAndIsArchivedFalse(userId);
        if (currentPlan.isEmpty()) {
            return Collections.emptyList();
        }
        int currentVersion = currentPlan.get(0).getPlanVersion();

        List<Clipped> clippedList = clipRepository.findByUserIdAndPlanVersion(userId, currentVersion);

        log.info(
                "Filtering clips with params - userId: {}, keyConcepts: {}, onlyIncorrect: {}, category: {}, testInfo: {}",
                userId, keyConcepts, onlyIncorrect, category, testInfo);

        // testInfo 필터링
        if (testInfo != null) {
            clippedList = clippedList.stream()
                    .filter(clip -> clip.getUserActivity().getTestInfo().equals(testInfo))
                    .collect(Collectors.toList());
        }

        // 카테고리 필터링
        if (category != null) {
            clippedList = clippedList.stream()
                    .filter(clip -> clip.getUserActivity().getQuestion().getCategory() == category)
                    .collect(Collectors.toList());
        }

        // 오답만 필터링
        if (onlyIncorrect) {
            clippedList = clippedList.stream()
                    .filter(clip -> !clip.getUserActivity().isCorrection())
                    .collect(Collectors.toList());
        }

        // 키 컨셉 필터링
        if (keyConcepts != null && !keyConcepts.isEmpty()) {
            clippedList = clippedList.stream()
                    .filter(clip -> keyConcepts.contains(
                            clip.getUserActivity().getQuestion().getSkill().getKeyConcepts()))
                    .collect(Collectors.toList());
        }

        log.info("Raw clipped list size: {}", clippedList.size());

        List<ClipRespDto> result = clippedList.stream()
                .map(ClipRespDto::new)
                .collect(Collectors.toList());

        log.info("Final result size after filtering: {}", result.size());

        return result;
    }

    @Transactional(readOnly = true)
    public ResultDetailDto getClippedQuestionDetail(Long userId, Long clipId) {
        log.info("Fetching clipped question detail for userId: {} and clipId: {}", userId, clipId);

        Clipped clipped = clipRepository.findById(clipId)
                .orElseThrow(() -> new CustomApiException("해당 오답노트 기록을 찾을 수 없습니다."));

        if (!clipped.getUserActivity().getUser().getId().equals(userId)) {
            throw new CustomApiException("자신의 오답노트 기록만 조회할 수 있습니다.");
        }

        UserActivity userActivity = clipped.getUserActivity();
        Question question = userActivity.getQuestion();

        // ResultDetailDto 직접 생성
        return ResultDetailDto.builder()
                .skillName(question.getSkill().getKeyConcepts())
                .question(question.getQuestion())
                .qustionNum(userActivity.getQuestionNum())
                .description(question.getDescription())
                .option1(question.getOption1())
                .option2(question.getOption2())
                .option3(question.getOption3())
                .option4(question.getOption4())
                .answer(question.getAnswer())
                .solution(question.getSolution())
                .checked(userActivity.getChecked())
                .correction(userActivity.isCorrection())
                .testInfo(userActivity.getTestInfo())
                .title(question.getSkill().getTitle())
                .keyConcepts(question.getSkill().getKeyConcepts())
                .build();
    }

    @Transactional(readOnly = true)
    public ClipRespDto.ClippedDaysDto getClippedDaysDtos(Long userId) {
        List<String> completedDays = clipRepository.findCompletedDayNumbersByUserId(userId);

        return ClipRespDto.ClippedDaysDto.builder()
                .days(completedDays)
                .build();
    }

    @Transactional(readOnly = true)
    public ClipRespDto.ClippedSessionsDto getClippedSessionsDtos(Long userId) {
        // 1. 모든 모의고사 회차 정보 조회
        List<String> allSessions = questionRepository.findDistinctExamSessionByCategory(3);

        // 2. 사용자가 완료한 가장 최근 세션 조회
        UserExamSession latestSession = userExamSessionRepository
                .findFirstByUserIdOrderByCompletionDateDesc(userId)
                .orElse(null);

        // 3. 각 세션에 대해 포맷팅된 문자열 생성
        List<String> formattedSessions = allSessions.stream()
                .map(session -> {
                    if (latestSession != null && session.equals(latestSession.getSession())) {
                        return session + " (제일 최신 회차)";
                    }
                    return session;
                })
                .collect(Collectors.toList());

        // 4. 최신 세션 정보 포함하여 반환
        return ClipRespDto.ClippedSessionsDto.builder()
                .sessions(formattedSessions)
                .latestSession(latestSession != null ? latestSession.getSession() : null)
                .build();
    }
}
