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

    @Transactional(readOnly = true)
    public List<ClipRespDto> getClippedQuestions(Long userId, List<String> keyConcepts, boolean onlyIncorrect,
            Integer category, String testInfo) {
        
        log.info(
                "Filtering clips with params - userId: {}, keyConcepts: {}, onlyIncorrect: {}, category: {}, testInfo: {}",
                userId, keyConcepts, onlyIncorrect, category, testInfo);

        List<Clipped> clippedList;
        
        // 조건별로 적절한 Repository 메서드 호출
        if (testInfo != null) {
            clippedList = clipRepository.findByUserIdAndTestInfoOrderByQuestionNum(userId, testInfo);
        } else if (category != null) {
            if (keyConcepts != null && !keyConcepts.isEmpty()) {
                clippedList = clipRepository.findByUserIdAndKeyConceptsAndCategory(userId, keyConcepts, category);
            } else {
                clippedList = clipRepository.findByUserIdAndCategory(userId, category);
            }
        } else if (keyConcepts != null && !keyConcepts.isEmpty()) {
            clippedList = clipRepository.findByUserIdAndKeyConcepts(userId, keyConcepts);
        } else {
            clippedList = clipRepository.findByUserActivity_User_IdOrderByDateDesc(userId);
        }

        // 오답만 필터링
        if (onlyIncorrect) {
            clippedList = clippedList.stream()
                    .filter(clip -> !clip.getUserActivity().isCorrection())
                    .collect(Collectors.toList());
        }

        log.info("Found {} clips after filtering", clippedList.size());

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
        log.info("Extracted day number: {}", testInfo);

        try {
            ResultDetailDto detailDto = dailyService.getDailyResultDetail(userId, testInfo, question.getId());

            // 추가된 정보 설정
            detailDto.setTestInfo(testInfo);
            detailDto.setTitle(question.getSkill().getTitle());
            detailDto.setKeyConcepts(question.getSkill().getKeyConcepts());

            return detailDto;
        } catch (Exception e) {
            log.error("Error getting daily result detail for user: {}, day: {}, question: {}", userId, testInfo,
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

    @Transactional(readOnly = true)
    public ClipRespDto.ClippedDaysDto getClippedDaysDtos(Long userId) {
        List<String> completedDays = clipRepository.findCompletedDayNumbersByUserId(userId);

        return ClipRespDto.ClippedDaysDto.builder()
                .days(completedDays)
                .build();
    }
}
