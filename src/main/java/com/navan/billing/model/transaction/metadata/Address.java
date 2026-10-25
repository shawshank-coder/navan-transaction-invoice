package com.navan.billing.model.transaction.metadata;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class Address {

    @NotBlank
    String street;

    @NotBlank
    String city;

    @NotBlank
    String country;
}
