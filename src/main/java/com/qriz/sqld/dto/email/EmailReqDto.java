package com.qriz.sqld.dto.email;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailReqDto {
    /*  1)@기호를 포함해야 한다.
        2)@기호를 기준으로 이메일 주소를 이루는 로컬호스트와 도메인 파트가 존재해야 한다.
        3)'도메인 파트는 최소 하나의 점과 그 뒤에 최소한 2개의 알파벳을 가진다' 를 검증
    */
    @Email(message = "이메일 형식으로 입력해 주세요.")
    @Pattern(regexp = "^[^@]+@[^@]+\\.[a-zA-Z]{2,}$", message = "기호를 기준으로 이메일 주소를 이루는 로컬호스트와 도메인 파트가 존재해야 합니다.")
    @NotEmpty(message = "이메일을 입력해 주세요")
    private String email;
}