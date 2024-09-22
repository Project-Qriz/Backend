package com.qriz.sqld.service.exam;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.exam.UserExamSession;
import com.qriz.sqld.domain.exam.UserExamSessionRepository;
import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExamService {
    
    private final UserExamSessionRepository userExamSessionRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<TestRespDto.ExamRespDto> getExamQuestionsBySession(Long userId, String session) {
        UserExamSession userExamSession = userExamSessionRepository.findByUserIdAndSession(userId, session)
            .orElseThrow(() -> new CustomApiException("해당 회차의 모의고사 세션을 찾을 수 없습니다."));

        if (userExamSession.isCompleted()) {
            throw new CustomApiException("이미 완료된 모의고사입니다.");
        }

        List<Question> examQuestions = questionRepository.findByCategoryAndExamSessionOrderById(3, session);
        
        if (examQuestions.isEmpty()) {
            throw new CustomApiException("해당 회차의 모의고사 문제를 찾을 수 없습니다.");
        }

        return examQuestions.stream()
            .map(this::convertToExamRespDto)
            .collect(Collectors.toList());
    }

    private TestRespDto.ExamRespDto convertToExamRespDto(Question question) {
        return new TestRespDto.ExamRespDto(
            question.getId(),
            question.getSkill().getId(),
            question.getCategory(),
            question.getQuestion(),
            question.getOption1(),
            question.getOption2(),
            question.getOption3(),
            question.getOption4(),
            question.getTimeLimit()
        );
    }
}
