package com.navan.billing.model.transaction.metadata;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.navan.billing.model.MoneyDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Value;

@Value
public class HotelMetadata implements TransactionMetadata {

    @NotBlank
    String name;

    @NotNull
    @Valid
    Address address;

    @NotNull
    LocalDate checkIn;

    @NotNull
    LocalDate checkOut;

    @NotNull
    @Positive
    Integer nights;

    @NotNull
    @Digits(integer = 17, fraction = 2)
    @PositiveOrZero
    @JsonDeserialize(using = MoneyDeserializer.class)
    BigDecimal roomRatePerNight;

    List<@NotNull @Valid HotelExtra> extras;
}
