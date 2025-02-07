package com.qriz.sqld.mail.dto;

import lombok.Getter;
import lombok.Setter;

public class EmailRespDto {

    @Getter
    @Setter
    public static class MailVerificationResponse {
        private String message;
        private String resetToken; // 클라이언트에게는 전송하지 않음
    }

    @Getter
    @Setter
    public static class VerificationResponse {
        private String resetToken;

        public VerificationResponse(String resetToken) {
            this.resetToken = resetToken;
        }
    }

    @Getter
    @Setter
    public static class VerificationResult {
        private boolean verified;
        private String resetToken;

        public VerificationResult(boolean verified, String resetToken) {
            this.verified = verified;
            this.resetToken = resetToken;
        }
    }

}
