package com.qriz.sqld.mail.service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.mail.domain.EmailVerification.EmailVerification;
import com.qriz.sqld.mail.domain.EmailVerification.EmailVerificationRepository;
import com.qriz.sqld.mail.domain.PasswordResetToken.PasswordResetToken;
import com.qriz.sqld.mail.domain.PasswordResetToken.PasswordResetTokenRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Service
public class MailSendService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final EmailService emailService;
    private final EmailVerificationRepository verificationRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserRepository userRepository;
    private static final String LOGO_PATH = "src/main/resources/static/images/logo.png";
    private static final String SENDER_EMAIL = "ori178205@gmail.com";

    // 인증번호 생성
    private String makeRandomNumber() {
        Random r = new Random();
        int number = r.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    // 이메일 전송
    @Transactional
    public String joinEmail(String email) {
        // 이전 인증 정보 삭제
        verificationRepository.deleteByEmail(email);

        // 새 인증번호 생성
        String authNumber = makeRandomNumber();

        // 이메일 내용 생성 및 전송
        String title = "인증번호를 확인해 주세요!";
        String content = generateHtmlContent(authNumber);

        try {
            emailService.sendEmailWithInlineImage(
                    SENDER_EMAIL,
                    email,
                    title,
                    content,
                    LOGO_PATH,
                    "logo");

            // DB에 인증 정보 저장 (3분 유효)
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .authNumber(authNumber)
                    .expiryDate(LocalDateTime.now().plusMinutes(3))
                    .verified(false)
                    .build();

            verificationRepository.save(verification);
            log.debug("인증 이메일 발송 성공. email: {}", email);

            return authNumber;
        } catch (Exception e) {
            log.error("이메일 발송 실패. email: {}", email, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    // 인증번호 확인
    @Transactional
    public boolean CheckAuthNum(String email, String authNum) {
        return verificationRepository
                .findByEmailAndAuthNumberAndVerifiedFalse(email, authNum)
                .map(verification -> {
                    // 만료 시간 체크
                    if (verification.isExpired()) {
                        verificationRepository.delete(verification);
                        log.debug("인증번호가 만료되었습니다. email: {}", email);
                        return false;
                    }

                    // 인증 성공 처리
                    verification.verify();
                    verificationRepository.save(verification);
                    log.debug("인증 성공. email: {}", email);
                    return true;
                })
                .orElseGet(() -> {
                    log.debug("유효하지 않은 인증번호. email: {}, authNum: {}", email, authNum);
                    return false;
                });
    }

    // 인증번호 검증 (회원가입용)
    @Transactional
    public boolean verifyEmailCode(String email, String authNumber) {
        return verificationRepository
                .findByEmailAndAuthNumberAndVerifiedFalse(email, authNumber)
                .map(verification -> {
                    if (verification.isExpired()) {
                        verificationRepository.delete(verification);
                        return false;
                    }
                    verification.verify();
                    return true;
                })
                .orElse(false);
    }

    private String generateHtmlContent(String authNumber) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ko\">\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "    <title>인증번호 확인</title>\n" +
                "  </head>\n" +
                "  <body style=\"font-family: 'Noto Sans KR', Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;\">\n"
                +
                "    <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff;\">\n"
                +
                "      <tr>\n" +
                "        <td style=\"padding: 40px 20px;\">\n" +
                "          <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">\n" +
                "            <tr>\n" +
                "              <td style=\"padding-bottom: 60px;\">\n" +
                "                <img src=\"cid:logo\" alt=\"Qriz Logo\" style=\"width: auto; height: 32px;\" />\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-weight: bold; font-size: 24px; color: #24282D; padding: 0 30px 40px 30px;\">\n"
                +
                "                인증번호를 <span style=\"font-weight: bold; color: #007AFF;\">확인해</span><br><span style=\"font-weight: bold; color: #007AFF;\">주세요!</span>\n"
                +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-weight: semibold; font-size: 16px; color: #666666; padding: 0 30px 40px 30px;\">\n"
                +
                "                아래 인증번호를 인증번호 입력 창에<br>입력해주세요.\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-weight: bold; color: #333333; padding: 0 30px 10px 30px;\">\n" +
                "                인증번호\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"padding: 0 30px;\">\n" +
                "                <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">\n" +
                "                  <tr>\n" +
                "                    <td style=\"background-color: #F0F4F7; padding: 20px; font-size: 32px; font-weight: bold; text-align: center; color: #24282D; border-top: 2px solid #24282D;\">\n"
                +
                "                      " + authNumber + "\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </table>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-size: 14px; color: #999999; padding: 30px 30px 0 30px;\">\n" +
                "                이 코드를 요청하지 않은 경우, 즉시 암호를 변경하시기 바랍니다.\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";
    }

    @Transactional
    public void sendPasswordResetEmail(String email) {
        // 이전 토큰 삭제
        resetTokenRepository.deleteByEmail(email);

        // 새 토큰 생성
        String resetToken = UUID.randomUUID().toString();

        // 토큰 정보 저장
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .email(email)
                .token(resetToken)
                .expiryDate(LocalDateTime.now().plusMinutes(PasswordResetToken.EXPIRATION_MINUTES))
                .used(false)
                .build();

        resetTokenRepository.save(passwordResetToken);

        // 딥링크 생성
        String resetLink = String.format("qriz://password-reset?token=%s&email=%s", resetToken, email);

        // 개발 환경에서만 토큰 값 로그 출력
        log.debug("Generated reset token for testing: {}", resetToken);

        try {
            // 이메일 발송
            emailService.sendEmailWithInlineImage(
                    SENDER_EMAIL,
                    email,
                    "비밀번호 재설정",
                    generatePasswordResetEmailContent(resetLink),
                    LOGO_PATH,
                    "logo");
            log.debug("비밀번호 재설정 이메일 발송 성공. email: {}", email);
        } catch (Exception e) {
            log.error("비밀번호 재설정 이메일 발송 실패. email: {}", email, e);
            resetTokenRepository.delete(passwordResetToken); // 실패시 토큰 삭제
            throw new CustomApiException("이메일 발송에 실패했습니다. 다시 시도해 주세요.");
        }
    }

    // 비밀번호 재설정 토큰 검증
    @Transactional
    public boolean verifyPasswordResetToken(String email, String token) {
        return resetTokenRepository
                .findByEmailAndTokenAndUsedFalse(email, token)
                .map(resetToken -> {
                    if (resetToken.isExpired()) {
                        resetTokenRepository.delete(resetToken);
                        return false;
                    }
                    resetToken.setUsed(true);
                    return true;
                })
                .orElse(false);
    }

    private String generatePasswordResetEmailContent(String resetLink) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ko\">\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "    <title>비밀번호 재설정</title>\n" +
                "  </head>\n" +
                "  <body style=\"font-family: 'Noto Sans KR', Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;\">\n"
                +
                "    <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff;\">\n"
                +
                "      <tr>\n" +
                "        <td style=\"padding: 40px 20px;\">\n" +
                "          <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">\n" +
                "            <tr>\n" +
                "              <td style=\"padding-bottom: 60px;\">\n" +
                "                <img src=\"cid:logo\" alt=\"Qriz Logo\" style=\"width: auto; height: 32px;\" />\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-weight: bold; font-size: 24px; color: #24282D; padding: 0 30px 40px 30px;\">\n"
                +
                "                안녕하세요, <span style=\"font-weight: bold; color: #3A6EFE;\">Qriz</span>입니다.\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-weight: regular; font-size: 16px; color: #666666; padding: 0 30px 40px 30px;\">\n"
                +
                "                비밀번호 재설정 링크를 보내드립니다.<br>\n" +
                "                아래 버튼을 클릭하여 새로운 비밀번호를 설정해주세요.\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"padding: 0 30px;\">\n" +
                "                <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">\n" +
                "                  <tr>\n" +
                "                    <td style=\"text-align: center;\">\n" +
                "                      <a href=\"" + resetLink
                + "\" style=\"display: inline-block; min-width: 180px; background-color: #3A6EFE; color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 16px; font-weight: bold;\">비밀번호 재설정하기</a>\n"
                +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </table>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "              <td style=\"font-size: 14px; color: #999999; padding: 30px 30px 0 30px;\">\n" +
                "                본 링크는 30분 동안만 유효합니다.<br>\n" +
                "                비밀번호 재설정을 요청하지 않으셨다면, 이 메일을 무시하셔도 됩니다.\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";
    }

    @Transactional
    public void sendUsernameEmail(String email) {
        // 사용자 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomApiException("해당 이메일로 등록된 계정이 없습니다."));

        String title = "Qriz 아이디 안내";
        String content = generateUsernameEmailContent(user.getUsername());

        try {
            emailService.sendEmailWithInlineImage(
                    SENDER_EMAIL,
                    email,
                    title,
                    content,
                    LOGO_PATH,
                    "logo");
            log.debug("아이디 안내 이메일 발송 성공. email: {}", email);
        } catch (Exception e) {
            log.error("이메일 발송 실패. email: {}", email, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    private String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return username;
        }

        if (username.contains("@")) {
            String[] parts = username.split("@");
            String name = parts[0];
            String domain = parts[1];

            // 이메일 길이에 따른 동적 마스킹
            int nameLength = name.length();
            String maskedName;

            if (nameLength <= 3) {
                maskedName = name.charAt(0) + "*".repeat(nameLength - 1);
            } else if (nameLength <= 6) {
                maskedName = name.charAt(0) +
                        "*".repeat(nameLength - 2) +
                        name.charAt(nameLength - 1);
            } else {
                maskedName = name.substring(0, 2) +
                        "*".repeat(nameLength - 4) +
                        name.substring(nameLength - 2);
            }

            return maskedName + "@" + domain;
        }

        // 일반 username
        if (username.length() <= 4) {
            return username.charAt(0) + "*".repeat(username.length() - 1);
        } else {
            return username.substring(0, 2) +
                    "*".repeat(username.length() - 4) +
                    username.substring(username.length() - 2);
        }
    }

    private String generateUsernameEmailContent(String username) {
        String maskedUsername = maskUsername(username);

        return "<!DOCTYPE html>\n" +
                "<html lang=\"ko\">\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "    <title>아이디 안내</title>\n" +
                "  </head>\n" +
                "  <body style=\"font-family: 'Noto Sans KR', Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;\">\n"
                +
                "    <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff;\">\n"
                +
                "      <tr>\n" +
                "        <td style=\"padding: 40px 20px;\">\n" +
                "          <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">\n" +
                "            <tr>\n" +
                "              <td style=\"padding-bottom: 60px;\">\n" +
                "                <img src=\"cid:logo\" alt=\"Qriz Logo\" style=\"width: auto; height: 32px;\" />\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-weight: bold; font-size: 24px; color: #24282D; padding: 0 30px 40px 30px;\">\n"
                +
                "                안녕하세요, <span style=\"font-weight: bold; color: #3A6EFE;\">Qriz</span>입니다.\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-weight: semibold; font-size: 16px; color: #666666; padding: 0 30px 40px 30px;\">\n"
                +
                "                <span style=\"font-weight: bold; color: #3A6EFE;\">아이디</span>를 확인해 주세요.\n" +
                "              </td>\n" +
                "            <tr>\n" +
                "              <td style=\"font-weight: semibold; font-size: 16px; color: #666666; padding: 0 30px 40px 30px;\">\n"
                +
                "                조회하신 아이디는 다음과 같습니다.\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"padding: 0 30px;\">\n" +
                "                <div style=\"font-family: 'Gothic Neo', sans-serif; font-weight: 800; font-size: 14px; color: #24282D; margin-bottom: 8px;\">아이디</div>\n"
                +
                "                <div style=\"border-bottom: 2px solid #24282D;\"></div>\n" +
                "                <div style=\"background-color: #F7F8FA; padding: 16px; text-align: center;\">\n" +
                "                  <span style=\"font-size: 26px; color: #24282D;\">" + maskedUsername + "</span>\n" +
                "                </div>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "              <td style=\"font-size: 14px; color: #999999; padding: 30px 30px 0 30px;\">\n" +
                "                본 이메일은 발신전용이며, 아이디 찾기를 요청하지 않으셨다면<br>이 이메일을 무시하셔도 됩니다.\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";
    }
}