package com.navan.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TaxLineValue {

    @Column(nullable = false)
    private String name;

    private BigDecimal rate;

    @Column(nullable = false)
    private BigDecimal amount;
}
