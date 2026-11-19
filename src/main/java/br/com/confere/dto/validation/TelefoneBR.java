package br.com.confere.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TelefoneBrValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TelefoneBR {

    String message() default "Telefone brasileiro inválido (use DDD + número com 10 ou 11 dígitos)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
