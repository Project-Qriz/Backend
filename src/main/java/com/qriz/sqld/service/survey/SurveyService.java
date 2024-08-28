package com.qriz.sqld.service.survey;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.survey.Survey;
import com.qriz.sqld.domain.survey.SurveyRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.survey.SurveyRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.service.daily.DailyPlanService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SurveyService {

        private final SurveyRepository surveyRepository;
        private final UserRepository userRepository;
        private final SkillRepository skillRepository;
        private final DailyPlanService dailyPlanService;

        @Transactional
        public List<SurveyRespDto> submitSurvey(Long userId, List<String> keyConcepts) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomApiException("해당 사용자가 존재하지 않습니다."));

                if (isSurveyCompleted(userId)) {
                        throw new CustomApiException("이미 설문 조사를 완료하셨습니다.");
                }

                if (keyConcepts.isEmpty() || keyConcepts.contains("KNOWS_NOTHING")) {
                        // "아무것도 모른다" 옵션 처리
                        Survey survey = Survey.createKnowsNothingSurvey(user);
                        surveyRepository.save(survey);

                        // 플랜 즉시 생성
                        dailyPlanService.generateDailyPlan(userId);

                        return Collections.singletonList(SurveyRespDto.createKnowsNothingResponse(user.getId()));
                }

                // 기존 로직 (특정 개념 선택 시)
                List<Skill> skills = skillRepository.findByKeyConceptsIn(keyConcepts);
                List<Survey> surveys = skills.stream()
                                .map(skill -> new Survey(user, skill, true))
                                .collect(Collectors.toList());
                surveyRepository.saveAll(surveys);

                return surveys.stream()
                                .map(survey -> new SurveyRespDto(survey.getUser().getId(), survey.getSkill().getId(),
                                                survey.isChecked(), false))
                                .collect(Collectors.toList());
        }

        public boolean isSurveyCompleted(Long userId) {
                return surveyRepository.existsByUserId(userId);
        }

        public List<SurveyRespDto> getSurveyResults(Long userId) {
                List<Survey> surveys = surveyRepository.findByUserId(userId);
                if (surveys.isEmpty()) {
                        return Collections.emptyList();
                }

                if (surveys.get(0).isKnowsNothing()) {
                        return Collections.singletonList(SurveyRespDto.createKnowsNothingResponse(userId));
                }

                return surveys.stream()
                                .map(survey -> new SurveyRespDto(survey.getUser().getId(), survey.getSkill().getId(),
                                                survey.isChecked(), false))
                                .collect(Collectors.toList());
        }

        public boolean isKnowsNothingUser(Long userId) {
                return surveyRepository.findByUserId(userId).stream()
                                .anyMatch(Survey::isKnowsNothing);
        }
}