package com.qriz.sqld.handler.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        boolean valid = true;
        // 기존 메시지 비활성화
        context.disableDefaultConstraintViolation();

        // 공백을 제거한 길이 검사
        String trimmedPassword = password.replaceAll("\\s+", "");
        if (trimmedPassword.length() < 8 || trimmedPassword.length() > 16) {
            context.buildConstraintViolationWithTemplate("8자 이상 16자 이하 입력 (공백 제외)")
                    .addConstraintViolation();
            valid = false;
        }

        // 대문자, 소문자, 숫자, 특수문자 포함 및 공백 미포함 검사
        if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=*!])(?=\\S+$).+$")) {
            context.buildConstraintViolationWithTemplate("대문자/소문자/숫자/특수문자 포함")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
