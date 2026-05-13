package com.green.mmg.auth.user.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/**
 * user.birth: DB 컬럼 DATE ↔ Java 필드 String("yyyy-MM-dd").
 *
 * <p>응답 스펙 동결을 위해 birth는 String 유지 (UserGetRes.birth: String).
 * MyBatis 시절 자동 변환을 JPA에선 명시 컨버터로 동일 동작.</p>
 *
 * <p>autoApply=false — 다른 String 필드에 잘못 적용되지 않도록 @Convert 명시 사용.</p>
 */
@Converter(autoApply = false)
public class StringDateConverter implements AttributeConverter<String, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(String birth) {
        if (birth == null || birth.isBlank()) return null;
        return LocalDate.parse(birth);
    }

    @Override
    public String convertToEntityAttribute(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
