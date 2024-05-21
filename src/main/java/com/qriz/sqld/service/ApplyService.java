package com.qriz.sqld.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        boolean exists = userApplyRepository.existsByUserIdAndApplicationId(loginUser.getUser().getId(), applyReqDto.getApplyId());
        if (exists) {
            throw new IllegalArgumentException("이미 해당 시험에 접수하였습니다.");
        }
        
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String testTime = applyReqDto.getStartTime().format(timeFormatter) + " ~ " + applyReqDto.getEndTime().format(timeFormatter);

        // 2. 시험이 존재하는지 확인
        Application application = applicationRepository.findById(applyReqDto.getApplyId())
                .orElseThrow(() -> new CustomApiException("존재하지 않는 시험입니다."));

        UserApply userApply = new UserApply(loginUser.getUser(), application);
        userApplyRepository.save(userApply);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        logger.info(loginUser.getUser().getNickname());

        return new ApplicationRespDto.ApplyRespDto(
                application.getId(),
                applyReqDto.getDate().format(dateFormatter),
                testTime,
                applyReqDto.getLocation()
        );
    }
}
