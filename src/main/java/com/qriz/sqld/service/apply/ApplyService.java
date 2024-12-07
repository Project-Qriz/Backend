package com.qriz.sqld.service.apply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.domain.application.Application;
import com.qriz.sqld.domain.application.ApplicationRepository;
import com.qriz.sqld.domain.apply.UserApply;
import com.qriz.sqld.domain.apply.UserApplyRepository;
import com.qriz.sqld.dto.application.ApplicationReqDto;
import com.qriz.sqld.dto.application.ApplicationRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ApplyService {

        private final ApplicationRepository applicationRepository;
        private final UserApplyRepository userApplyRepository;

        private final Logger logger = LoggerFactory.getLogger(ApplyService.class);

        // 시험 접수 목록 조회
        public ApplicationRespDto.ApplyListRespDto applyList() {
                List<Application> applications = applicationRepository.findAll();
                List<ApplicationRespDto.ApplyListRespDto.ApplicationDetail> applicationDetails = applications.stream()
                                .map(ApplicationRespDto.ApplyListRespDto.ApplicationDetail::new)
                                .collect(Collectors.toList());
                return new ApplicationRespDto.ApplyListRespDto(applicationDetails);
        }

        // 시험 접수
        public ApplicationRespDto.ApplyRespDto apply(ApplicationReqDto.ApplyReqDto applyReqDto, LoginUser loginUser) {
                // 1. 해당 사용자가 해당 시험에 접수 중인지 확인
                boolean exists = userApplyRepository.existsByUserIdAndApplicationId(
                                loginUser.getUser().getId(),
                                applyReqDto.getApplyId());

                if (exists) {
                        throw new CustomApiException("이미 해당 시험에 접수하였습니다.");
                }

                // 2. 시험이 존재하는지 확인
                Application application = applicationRepository.findById(applyReqDto.getApplyId())
                                .orElseThrow(() -> new CustomApiException("존재하지 않는 시험입니다."));

                // 3. 사용자 접수 정보 생성
                UserApply userApply = new UserApply(loginUser.getUser(), application);
                userApplyRepository.save(userApply);

                // 4. 응답 데이터 생성
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String period = formatPeriod(application.getStartDate(), application.getEndDate());

                return new ApplicationRespDto.ApplyRespDto(
                                application.getId(),
                                period,
                                application.getExamDate().format(dateFormatter),
                                application.getTestTime());
        }

        // 등확한 시험 접수 정보 조회
        public ApplicationRespDto.ApplyRespDto getApplied(Long userId) {
                // 1. 사용자 조회
                UserApply userApply = userApplyRepository.findUserApplyByUserId(userId)
                                .orElseThrow(() -> new CustomApiException("등록된 일정이 없어요"));

                Application application = userApply.getApplication();

                String testTime = application.getTestTime();

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                String period = formatPeriod(application.getStartDate(), application.getEndDate());

                return new ApplicationRespDto.ApplyRespDto(
                                application.getId(),
                                period,
                                application.getExamDate().format(dateFormatter),
                                testTime);
        }

        // 등록한 시험에 대한 D-Day
        public ApplicationRespDto.ExamDDayRespDto getDDay(Long userId) {
                UserApply userApply = userApplyRepository.findUserApplyByUserId(userId)
                                .orElseThrow(() -> new CustomApiException("등록된 일정이 없어요"));

                LocalDate examDate = userApply.getApplication().getExamDate();
                LocalDate currentDate = LocalDate.now();

                long daysBetween = ChronoUnit.DAYS.between(currentDate, examDate);

                return new ApplicationRespDto.ExamDDayRespDto((int) daysBetween);
        }

        private String formatPeriod(LocalDate startDate, LocalDate endDate) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM.dd(E)");
                return startDate.format(formatter) + " ~ " + endDate.format(formatter);
        }

        // 시험 일정 수정
        public ApplicationRespDto.ApplyRespDto modifyApplication(ApplicationReqDto.ModifyReqDto modifyReqDto,
                        LoginUser loginUser) {
                // 1. 현재 사용자의 접수 정보 조회
                UserApply currentUserApply = userApplyRepository.findUserApplyByUserId(loginUser.getUser().getId())
                                .orElseThrow(() -> new CustomApiException("현재 접수된 시험을 찾을 수 없습니다."));

                Long currentApplyId = currentUserApply.getApplication().getId();

                // 2. 동일한 시험 선택 시 예외 처리
                if (currentApplyId.equals(modifyReqDto.getNewApplyId())) {
                        throw new CustomApiException("현재 접수된 시험과 동일한 시험입니다.");
                }

                // 3. 새로운 시험 정보 확인
                Application newApplication = applicationRepository.findById(modifyReqDto.getNewApplyId())
                                .orElseThrow(() -> new CustomApiException("변경하려는 시험 정보를 찾을 수 없습니다."));

                // 4. 기존 접수 삭제 및 새로운 접수 생성
                userApplyRepository.delete(currentUserApply);
                UserApply newUserApply = new UserApply(loginUser.getUser(), newApplication);
                userApplyRepository.save(newUserApply);

                // 5. 응답 데이터 생성
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String period = formatPeriod(newApplication.getStartDate(), newApplication.getEndDate());

                return new ApplicationRespDto.ApplyRespDto(
                                newApplication.getId(),
                                period,
                                newApplication.getExamDate().format(dateFormatter),
                                newApplication.getTestTime());
        }
}
