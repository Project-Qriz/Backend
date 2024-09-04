package com.qriz.sqld.mail.service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.user.UserReqDto;
import com.qriz.sqld.dto.user.UserRespDto;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Async
    public void sendEmailAsync(String setForm, String toMail, String title, String content) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom(setForm);
            helper.setTo(toMail);
            helper.setSubject(title);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Async
    public void sendEmailWithInlineImage(String setFrom, String toMail, String title, String content, String imagePath,
            String imageContentId) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom(setFrom);
            helper.setTo(toMail);
            helper.setSubject(title);
            helper.setText(content, true);

            FileSystemResource file = new FileSystemResource(new File(imagePath));
            helper.addInline(imageContentId, file);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // 이메일 중복확인
    @Transactional(readOnly = true)
    public UserRespDto.EmailDuplicateRespDto emailDuplicate(UserReqDto.EmailDuplicateReqDto emailDuplicateReqDto) {
        // 1. 이메일이 존재하는지 찾기
        Optional<User> userOP = userRepository.findByEmail(emailDuplicateReqDto.getEmail());

        // 2. 이메일 존재 여부에 따라 응답 생성
        if (userOP.isPresent()) {
            // 이메일 이미 사용 중인 경우
            return new UserRespDto.EmailDuplicateRespDto(false);
        } else {
            // 사용 가능한 이메일인 경우
            return new UserRespDto.EmailDuplicateRespDto(true);
        }
    }
}