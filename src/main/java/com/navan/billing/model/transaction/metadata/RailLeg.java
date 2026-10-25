package com.navan.billing.model.transaction.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Value;

@Value
public class RailLeg {

    @NotBlank
    String origin;

    @NotBlank
    String destination;

    @NotNull
    Instant departure;

    @NotNull
    Instant arrival;

    String trainNumber;

    @JsonProperty("class")
    String travelClass;
}
