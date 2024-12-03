package com.qriz.sqld;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qriz.sqld.dto.user.UserReqDto;
import com.qriz.sqld.service.user.UserService;
import com.qriz.sqld.mail.domain.EmailVerification.EmailVerification;
import com.qriz.sqld.mail.domain.EmailVerification.EmailVerificationRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class ResetEmailTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailVerificationRepository verificationRepository;

    @MockBean
    private UserService userService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private UserReqDto.ResetPasswordReqDto resetRequest;
    private EmailVerification validVerification;

    @BeforeEach
    void setUp() {
        // 테스트 요청 데이터 초기화
        resetRequest = new UserReqDto.ResetPasswordReqDto();
        resetRequest.setEmail("test@example.com");
        resetRequest.setToken("valid-token");
        resetRequest.setNewPassword("newPassword123!");

        // 유효한 검증 데이터 초기화
        validVerification = new EmailVerification();
        validVerification.setEmail(resetRequest.getEmail());
        validVerification.setAuthNumber(resetRequest.getToken());
        validVerification.setExpiryDate(LocalDateTime.now().plusHours(1));
        validVerification.setVerified(false);
        validVerification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("비밀번호 재설정 - 성공 케이스")
    public void testPasswordReset_Success() throws Exception {
        // given
        when(verificationRepository.findByEmailAndAuthNumberAndVerifiedFalse(
                resetRequest.getEmail(),
                resetRequest.getToken()))
                .thenReturn(Optional.of(validVerification));

        // when
        ResultActions result = mockMvc.perform(post("/api/pwd-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)));

        // then
        result.andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("비밀번호가 성공적으로 변경되었습니다. 다시 로그인해 주세요."));

        verify(userService).resetPassword(resetRequest.getEmail(), resetRequest.getNewPassword());
    }

    @Test
    @DisplayName("비밀번호 재설정 - 유효하지 않은 토큰")
    public void testPasswordReset_InvalidToken() throws Exception {
        // given
        when(verificationRepository.findByEmailAndAuthNumberAndVerifiedFalse(
                resetRequest.getEmail(),
                resetRequest.getToken()))
                .thenReturn(Optional.empty());

        // when
        ResultActions result = mockMvc.perform(post("/api/pwd-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)));

        // then
        result.andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.msg").value("유효하지 않거나 만료된 링크입니다."));
    }

    @Test
    @DisplayName("비밀번호 재설정 - 만료된 토큰")
    public void testPasswordReset_ExpiredToken() throws Exception {
        // given
        EmailVerification expiredVerification = new EmailVerification();
        expiredVerification.setEmail(resetRequest.getEmail());
        expiredVerification.setAuthNumber(resetRequest.getToken());
        expiredVerification.setExpiryDate(LocalDateTime.now().minusHours(1));
        expiredVerification.setVerified(false);

        when(verificationRepository.findByEmailAndAuthNumberAndVerifiedFalse(
                resetRequest.getEmail(),
                resetRequest.getToken()))
                .thenReturn(Optional.of(expiredVerification));

        // when
        ResultActions result = mockMvc.perform(post("/api/pwd-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)));

        // then
        result.andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.msg").value("링크가 만료되었습니다. 비밀번호 찾기를 다시 시도해주세요."));
    }

    @Test
    @DisplayName("비밀번호 재설정 - 잘못된 비밀번호 형식")
    public void testPasswordReset_InvalidPasswordFormat() throws Exception {
        // given
        resetRequest.setNewPassword("weak"); // 약한 비밀번호

        // when
        ResultActions result = mockMvc.perform(post("/api/pwd-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)));

        // then
        result.andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 재설정 - 이미 사용된 토큰")
    public void testPasswordReset_AlreadyUsedToken() throws Exception {
        // given
        EmailVerification usedVerification = new EmailVerification();
        usedVerification.setEmail(resetRequest.getEmail());
        usedVerification.setAuthNumber(resetRequest.getToken());
        usedVerification.setExpiryDate(LocalDateTime.now().plusHours(1));
        usedVerification.setVerified(true);

        when(verificationRepository.findByEmailAndAuthNumberAndVerifiedFalse(
                resetRequest.getEmail(),
                resetRequest.getToken()))
                .thenReturn(Optional.empty());

        // when
        ResultActions result = mockMvc.perform(post("/api/pwd-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)));

        // then
        result.andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.msg").value("유효하지 않거나 만료된 링크입니다."));
    }
}