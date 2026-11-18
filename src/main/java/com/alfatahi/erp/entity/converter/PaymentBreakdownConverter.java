package com.alfatahi.erp.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Converter
public class PaymentBreakdownConverter implements AttributeConverter<Map<String, BigDecimal>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, BigDecimal>> TYPE = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(Map<String, BigDecimal> breakdown) {
        if (breakdown == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(breakdown);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Não foi possível salvar os valores por forma de pagamento.", e);
        }
    }

    @Override
    public Map<String, BigDecimal> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return new LinkedHashMap<>();
        try {
            return OBJECT_MAPPER.readValue(value, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Não foi possível ler os valores por forma de pagamento.", e);
        }
    }
}
