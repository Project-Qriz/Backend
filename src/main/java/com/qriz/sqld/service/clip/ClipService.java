package com.qriz.sqld.service.clip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.UserActivity.UserActivity;
import com.qriz.sqld.domain.UserActivity.UserActivityRepository;
import com.qriz.sqld.domain.clip.ClipRepository;
import com.qriz.sqld.domain.clip.Clipped;
import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.dto.clip.ClipReqDto;
import com.qriz.sqld.dto.clip.ClipRespDto;
import com.qriz.sqld.dto.daily.ResultDetailDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.service.daily.DailyService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

        log.info("Getting clipped questions - userId: {}, category: {}, testInfo: {}",
                userId, category, testInfo);

        // testInfo가 없으면 해당 카테고리의 최신 testInfo 찾기
        if (testInfo == null) {
            if (category == 2) { // 데일리
                // Day1, Day2, ... 중 가장 큰 번호 찾기
                Integer latestDay = clipRepository.findLatestDayNumberByUserId(userId);
                if (latestDay != null) {
                    testInfo = "Day" + latestDay;
                }
            } else if (category == 3) { // 모의고사
                List<String> sessions = clipRepository.findCompletedSessionsByUserId(userId);
                if (!sessions.isEmpty()) {
                    // 이미 내림차순 정렬되어 있으므로 첫 번째 값이 최신
                    testInfo = sessions.get(0);
                }
            }
        }

        List<ClipRespDto> result = new ArrayList<>();

        // testInfo가 있는 경우 해당하는 문제들 조회
        if (testInfo != null) {
            List<Clipped> clippedList = clipRepository.findByUserIdAndTestInfoOrderByQuestionNum(userId, testInfo);

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

            result = clippedList.stream()
                    .map(ClipRespDto::new)
                    .collect(Collectors.toList());
        }

        log.info("Found {} questions for testInfo: {}", result.size(), testInfo);
        return result;
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
    public List<ClipRespDto> getFilteredClippedQuestions(Long userId, List<String> keyConcepts, boolean onlyIncorrect,
            Integer category, String testInfo) {
        List<Clipped> clippedList;

        log.info(
                "Filtering clips with params - userId: {}, keyConcepts: {}, onlyIncorrect: {}, category: {}, testInfo: {}",
                userId, keyConcepts, onlyIncorrect, category, testInfo);

        if (testInfo != null) {
            clippedList = clipRepository.findByUserIdAndTestInfoOrderByQuestionNum(userId, testInfo);
        } else {
            if (keyConcepts == null || keyConcepts.isEmpty()) {
                if (onlyIncorrect) {
                    if (category == null) {
                        clippedList = clipRepository.findIncorrectByUserId(userId);
                    } else {
                        clippedList = clipRepository.findIncorrectByUserIdAndCategory(userId, category);
                        log.info("Found {} incorrect clips for category {}", clippedList.size(), category);
                    }
                } else {
                    if (category == null) {
                        clippedList = clipRepository.findByUserActivity_User_IdOrderByDateDesc(userId);
                    } else {
                        clippedList = clipRepository.findByUserIdAndCategory(userId, category);
                        log.info("Found {} clips for category {}", clippedList.size(), category);
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
                        clippedList = clipRepository.findByUserIdAndKeyConceptsAndCategory(userId, keyConcepts,
                                category);
                    }
                }
            }
        }

        log.info("Raw clipped list size: {}", clippedList.size());

        List<ClipRespDto> result = clippedList.stream()
                .filter(clipped -> (keyConcepts == null || keyConcepts.isEmpty()
                        || keyConcepts.contains(clipped.getUserActivity().getQuestion().getSkill().getKeyConcepts())))
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

    @Transactional(readOnly = true)
    public ClipRespDto.ClippedDaysDto getClippedDaysDtos(Long userId) {
        List<String> completedDays = clipRepository.findCompletedDayNumbersByUserId(userId);

        return ClipRespDto.ClippedDaysDto.builder()
                .days(completedDays)
                .build();
    }

    @Transactional(readOnly = true)
    public ClipRespDto.ClippedSessionsDto getClippedSessionsDtos(Long userId) {
        List<String> completedSessions = clipRepository.findCompletedSessionsByUserId(userId);

        return ClipRespDto.ClippedSessionsDto.builder()
                .sessions(completedSessions)
                .build();
    }
}
