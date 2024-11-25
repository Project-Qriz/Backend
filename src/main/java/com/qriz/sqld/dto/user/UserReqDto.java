package com.qriz.sqld.dto.user;

import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

public class UserReqDto {

    @Setter
    @Getter
    public static class LoginReqDto {
        private String username;
        private String password;
    }

    @Getter
    @Setter
    public static class JoinReqDto {
        @Pattern(regexp = "^[a-zA-Z0-9]{2,20}$", message = "영문/숫자 2~20자 이내로 작성해주세요")
        @NotEmpty
        private String username;

        /*
          1. 길이 : 최소 8 ~ 16 자
          2. 대문자 포함 : 최소 한 개의 대문자를 포함
          3. 소문자 포함 : 최소 한 개의 소문자를 포함
          4. 숫자 포함 : 하나 이상의 숫자 포함
          5. 특수 문자 포함 : 하나 이상의 특수 문자 포함
         */
        @NotEmpty
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=*!])(?=\\S+$).{8,16}$")
        private String password;

        @NotEmpty
        @Pattern(regexp = "^[a-zA-Z0-9]{2,10}@[a-zA-Z0-9]{2,6}\\.[a-zA-Z]{2,3}$", message = "이메일 형식으로 작성해주세요")
        private String email;

        @NotEmpty
        @Pattern(regexp = "^[a-zA-Z가-힣]{1,20}$", message = "한글/영문 1~20자 이내로 작성해주세요")
        private String nickname;

        public User toEntity(BCryptPasswordEncoder passwordEncoder) {
            return User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .email(email)
                    .nickname(nickname)
                    .role(UserEnum.CUSTOMER)
                    .build();
        }
    }

    @Getter
    @Setter
    public static class FindUsernameReqDto {
        @NotEmpty
        @Pattern(regexp = "^[a-zA-Z가-힣]{1,20}$", message = "한글/영문 1~20자 이내로 작성해주세요")
        private String nickname;
        
        @NotEmpty
        @Pattern(regexp = "^[a-zA-Z0-9]{2,10}@[a-zA-Z0-9]{2,6}\\.[a-zA-Z]{2,3}$", message = "이메일 형식으로 작성해주세요")
        private String email;
    }

    @Getter
    @Setter
    public static class FindPwdReqDto {
        @NotEmpty
        private String email;
    }

    @Getter
    @Setter
    public static class ChangePwdReqDto {
        @NotEmpty
        private String password;
    }

    @Getter
    @Setter
    public static class UsernameDuplicateReqDto {
        private String username;        
    }

    @Getter
    @Setter
    public static class EmailDuplicateReqDto {
        private String email;        
    }
}
