package com.abo2.recode.dto.user;

import java.time.LocalDateTime;

import com.abo2.recode.domain.user.User;
import com.abo2.recode.util.CustomDateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class UserRespDto {

    @Setter
    @Getter
    public static class LoginRespDto {
        private Long id;
        private String username;
        private String createdAt;

        public LoginRespDto(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.createdAt = CustomDateUtil.toStringFormat(user.getCreatedAt());
        }
    }

    @ToString
    @Setter
    @Getter
    public static class JoinRespDto {
        private Long id;
        private String username;
        private String nickname;

        public JoinRespDto(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
        }
    }

    @Getter
    @Setter
    public static class FindUsernameRespDto {
        private Long id;
        private String username;
        private String createdAt;

        public FindUsernameRespDto(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.createdAt = CustomDateUtil.toStringFormat(user.getCreatedAt());
        }
    }

    @Getter
    @Setter
    public static class ChangePwdRespDto {
        private String username;
        private String message;
        
        public ChangePwdRespDto(User user) {
            this.username = user.getUsername();
            this.message = "비밀번호가 변경되었습니다.";
        }

        public ChangePwdRespDto(String username, String message) {
            this.username = username;
            this.message = message;
        }
    }

    @Getter
    @Setter
    public static class UsernameDuplicateRespDto {
        private boolean isAvailable;

        public UsernameDuplicateRespDto(boolean isAvailable) {
            this.isAvailable = isAvailable;
        }
    }

    @Getter
    @Setter
    public static class EmailDuplicateRespDto {
        private boolean isAvailable;

        public EmailDuplicateRespDto(boolean isAvailable) {
            this.isAvailable = isAvailable;
        }
    }
}
