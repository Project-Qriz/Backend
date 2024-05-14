package com.qriz.sqld.controller;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.dto.email.EmailCheckDto;
import com.qriz.sqld.dto.email.EmailReqDto;
import com.qriz.sqld.service.MailSendService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class EmailController {
    
    private final MailSendService mailService;

    @PostMapping(value = {"/v1/email-send", "/email-send"})
    public String mailSend(@RequestBody @Valid EmailReqDto emailDto){
        System.out.println("이메일 인증 이메일 :"+emailDto.getEmail());
        return mailService.joinEmail(emailDto.getEmail());
    }

    @PostMapping(value = {"/v1/email-authentication", "/email-authentication"})
    public String AuthCheck(@RequestBody @Valid EmailCheckDto emailCheckDto){
        Boolean Checked=mailService.CheckAuthNum(emailCheckDto.getEmail(),emailCheckDto.getAuthNum());
        if(Checked){
            return "ok";
        }
        else{
            throw new NullPointerException("뭔가 잘못됨!");
        }
    }
}
