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
                "    <style>\n" +
                "      @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700&display=swap');\n"
                +
                "      body { font-family: 'Noto Sans KR', sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }\n"
                +
                "      .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 40px 20px; }\n"
                +
                "      .logo { margin-bottom: 30px; }\n" +
                "      .logo img { width: auto; height: 32px; }\n" +
                "      h1 { font-size: 24px; color: #24282D; margin-bottom: 20px; }\n" +
                "      h1 .highlight { color: #007AFF; }\n" +
                "      p { font-size: 16px; color: #666666; margin-bottom: 30px; }\n" +
                "      .auth-number-container { border-top: 2px solid #24282D; width: 100%; }\n" +
                "      .auth-number-header { text-align: left; font-weight: bold; color: #333333; padding: 10px 0; }\n"
                +
                "      .auth-number { background-color: #F0F4F7; padding: 20px; font-size: 32px; font-weight: bold; text-align: center; color: #24282D; margin-bottom: 30px; }\n"
                +
                "      .warning { font-size: 14px; color: #999999; }\n" +
                "      @media only screen and (max-width: 480px) {\n" +
                "        .container { width: 100%; padding: 20px; box-sizing: border-box; }\n" +
                "        .auth-number-container { width: 100%; }\n" +
                "        .auth-number { font-size: 28px; padding: 15px; }\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <div class=\"container\">\n" +
                "      <div class=\"logo\">\n" +
                "        <img src=\"cid:logo\" alt=\"Qriz Logo\">\n" +
                "      </div>\n" +
                "      <h1>인증번호를 <span class=\"highlight\">확인해</span><br><span class=\"highlight\">주세요!</span></h1>\n" +
                "      <p>아래 인증번호를 인증번호 입력 창에<br>입력해주세요.</p>\n" +
                "      <div class=\"auth-number-header\">인증번호</div>\n" +
                "      <div class=\"auth-number-container\">\n" +
                "        <div class=\"auth-number\">" + authNumber + "</div>\n" +
                "      </div>\n" +
                "      <p class=\"warning\">이 코드를 요청하지 않은 경우, 즉시 암호를 변경하시기 바랍니다.</p>\n" +
                "    </div>\n" +
                "  </body>\n" +
                "</html>";
    }

    public String generatePasswordReseUrl(String email) {
        String token = UUID.randomUUID().toString();
        redisUtil.setDataExpire(token, email, 60 * 3L); // 토큰을 Redis 에 저장, 3 분간 유효
        return "http://localhost:8081/api/v1/reset-password?token=" + token;
    }
}