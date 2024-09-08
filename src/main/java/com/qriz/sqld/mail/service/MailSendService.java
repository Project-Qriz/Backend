package com.qriz.sqld.mail.service;

import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.qriz.sqld.util.RedisUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MailSendService {

    private final EmailService emailService;
    private final RedisUtil redisUtil;
    private int authNumber;

    private static final String LOGO_PATH = "src/main/resources/static/images/logo.png";

    // 인증번호 확인
    public boolean CheckAuthNum(String email, String authNum) {
        if (redisUtil.getData(authNum) == null) {
            return false;
        } else if (redisUtil.getData(authNum).equals(email)) {
            return true;
        } else {
            return false;
        }
    }

    // 임의의 6자리 양수를 반환
    public void makeRandomNumber() {
        Random r = new Random();
        authNumber = r.nextInt(900000) + 100000;
    }

    // 이메일 전송
    public String joinEmail(String email) {
        makeRandomNumber();
        String setFrom = "ori178205@gmail.com";
        String toMail = email;
        String title = "인증번호를 확인해 주세요!";
        String content = generateHtmlContent(Integer.toString(authNumber));

        try {
            emailService.sendEmailWithInlineImage(setFrom, toMail, title, content, LOGO_PATH, "logo");
        } catch (Exception e) {
            // 로그 처리 또는 예외 처리
            e.printStackTrace();
        }

        redisUtil.setDataExpire(Integer.toString(authNumber), toMail, 60 * 3L);
        return Integer.toString(authNumber);
    }

    private String generateHtmlContent(String authNumber) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ko\">\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "    <title>인증번호 확인</title>\n" +
                "  </head>\n" +
                "  <body style=\"font-family: 'Noto Sans KR', Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;\">\n" +
                "    <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff;\">\n" +
                "      <tr>\n" +
                "        <td style=\"padding: 40px 20px;\">\n" +
                "          <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">\n" +
                "            <tr>\n" +
                "              <td style=\"padding-bottom: 30px;\">\n" +
                "                <img src=\"cid:logo\" alt=\"Qriz Logo\" style=\"width: auto; height: 32px;\" />\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-size: 24px; color: #24282D; padding-bottom: 20px;\">\n" +
                "                인증번호를 <span style=\"color: #007AFF;\">확인해</span><br><span style=\"color: #007AFF;\">주세요!</span>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-size: 16px; color: #666666; padding-bottom: 30px;\">\n" +
                "                아래 인증번호를 인증번호 입력 창에<br>입력해주세요.\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-weight: bold; color: #333333; padding: 10px 0; border-top: 2px solid #24282D;\">\n" +
                "                인증번호\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"background-color: #F0F4F7; padding: 20px; font-size: 32px; font-weight: bold; text-align: center; color: #24282D;\">\n" +
                "                " + authNumber + "\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td style=\"font-size: 14px; color: #999999; padding-top: 30px;\">\n" +
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

    public String generatePasswordReseUrl(String email) {
        String token = UUID.randomUUID().toString();
        redisUtil.setDataExpire(token, email, 60 * 3L); // 토큰을 Redis 에 저장, 3 분간 유효
        return "http://localhost:8081/api/v1/reset-password?token=" + token;
    }
}