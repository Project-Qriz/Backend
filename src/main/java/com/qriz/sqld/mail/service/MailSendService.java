package com.qriz.sqld.mail.service;

import java.time.LocalDateTime;
import java.util.Random;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.mail.domain.EmailVerification;
import com.qriz.sqld.mail.domain.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Service
public class MailSendService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final EmailService emailService;
    private final EmailVerificationRepository verificationRepository;
    private static final String LOGO_PATH = "src/main/resources/static/images/logo.png";
    private static final String SENDER_EMAIL = "ori178205@gmail.com";

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

    // 비밀번호 재설정 링크 생성
    @Transactional
    public String generatePasswordResetUrl(String email) {
        String authNumber = makeRandomNumber();

        // 이전 인증 정보 삭제
        verificationRepository.deleteByEmail(email);

        // 새 인증 정보 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .authNumber(authNumber)
                .expiryDate(LocalDateTime.now().plusMinutes(3))
                .verified(false)
                .build();

        verificationRepository.save(verification);

        return "http://localhost:8081/api/v1/reset-password?auth=" + authNumber + "&email=" + email;
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
}