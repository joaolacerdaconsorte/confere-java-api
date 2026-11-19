package br.com.confere.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TelefoneBrValidator implements ConstraintValidator<TelefoneBR, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
            digits = digits.substring(2);
        }
        return digits.length() == 10 || digits.length() == 11;
    }
}
