package com.qriz.sqld.dto.email;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

import lombok.Getter;

@Getter
public class EmailCheckDto {
    @Email
    @NotEmpty(message = "이메일을 입력해 주세요")
    private String email;

    @NotEmpty(message = "인증 번호를 입력해 주세요")
    private String authNum;
}
