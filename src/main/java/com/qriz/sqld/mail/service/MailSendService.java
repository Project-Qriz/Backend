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

    // 인증번호 확인
    public boolean CheckAuthNum(String email,String authNum){
        if(redisUtil.getData(authNum)==null){
            return false;
        }
        else if(redisUtil.getData(authNum).equals(email)){
             return true;
         }
         else{
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
        String title = "회원 가입 인증 이메일 입니다.";
        String reseUrl = generatePasswordReseUrl(email);
        String content = "나의 APP을 방문해주셔서 감사합니다." + "<br><br>" + "인증 번호는 " + authNumber + "입니다." + "<br>" + "인증번호를 제대로 입력해주세요" +
        "<br><br>" + "비밀번호를 재설정하려면 아래 버튼을 클릭해주세요." + "<br><br>" + "<a href=\"" + reseUrl + "\">비밀번호 재설정하기</a>";
        
        emailService.sendEmailAsync(setFrom, toMail, title, content);
        redisUtil.setDataExpire(Integer.toString(authNumber), toMail, 60 * 3L);
        return Integer.toString(authNumber);
    }

    public String generatePasswordReseUrl(String email) {
        String token = UUID.randomUUID().toString();
        redisUtil.setDataExpire(token, email, 60 * 3L); // 토큰을 Redis 에 저장, 3 분간 유효
        return "http://localhost:8081/api/v1/reset-password?token=" + token;
    }
}