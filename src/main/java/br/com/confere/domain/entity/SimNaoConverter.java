package br.com.confere.domain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SimNaoConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute ? "S" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return "S".equalsIgnoreCase(dbData.trim());
    }
}
