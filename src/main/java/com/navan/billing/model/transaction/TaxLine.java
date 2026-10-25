package com.navan.billing.model.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.navan.billing.model.MoneyDeserializer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaxLine {

    @NotBlank
    @Size(max = 128)
    String name;

    @DecimalMin("0")
    @Digits(integer = 3, fraction = 6)
    BigDecimal rate;

    @NotNull
    @Digits(integer = 17, fraction = 2)
    @PositiveOrZero
    @JsonDeserialize(using = MoneyDeserializer.class)
    BigDecimal amount;
}
