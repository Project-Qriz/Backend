package com.qriz.sqld.handler.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target(( FIELD ))
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "유효하지 않은 비밀번호";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
