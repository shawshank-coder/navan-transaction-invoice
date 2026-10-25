package com.navan.billing.model.transaction.metadata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Value;

@Value
public class Coupon {

    @NotBlank
    String flightNumber;

    String carrier;

    @NotBlank
    String from;

    @NotBlank
    String to;

    @NotNull
    Instant departure;

    @NotNull
    Instant arrival;
}
