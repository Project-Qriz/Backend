package com.qriz.sqld.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.survey.Survey;
import com.qriz.sqld.domain.survey.SurveyRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.survey.SurveyRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;

    private final UserRepository userRepository;

    private final SkillRepository skillRepository;

    public List<SurveyRespDto> submitSurvey(Long userId, List<String> keyConcepts) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomApiException("해당 사용자가 존재하지 않습니다."));

        List<Skill> skills = skillRepository.findByKeyConceptsIn(keyConcepts);

        List<Survey> surveys = skills.stream()
                .map(skill -> new Survey(user, skill, true))
                .collect(Collectors.toList());

        surveyRepository.saveAll(surveys);

        return surveys.stream()
                .map(survey -> new SurveyRespDto(survey.getUser().getId(), survey.getSkill().getId(),
                        survey.isChecked()))
                .collect(Collectors.toList());
    }
}
