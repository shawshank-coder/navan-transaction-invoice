package com.navan.billing.entity;

import com.navan.billing.model.enums.TransactionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the lowercase API code ("navan_fee"), matching the CHECK constraint in V1__init.sql. */
@Converter(autoApply = true)
public class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {

    @Override
    public String convertToDatabaseColumn(TransactionType type) {
        return type == null ? null : type.code();
    }

    @Override
    public TransactionType convertToEntityAttribute(String code) {
        return code == null ? null : TransactionType.fromCode(code);
    }
}
