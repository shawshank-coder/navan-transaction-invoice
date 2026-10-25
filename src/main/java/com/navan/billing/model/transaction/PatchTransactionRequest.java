package com.navan.billing.model.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.navan.billing.model.MoneyDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Value;

/** Absent fields are left unchanged; type, external_id and currency are immutable, so sending them is a 400. */
@Value
public class PatchTransactionRequest {

    // JsonNode so that even an explicit JSON null counts as "sent" (it arrives as NullNode).
    @Schema(hidden = true)
    @Null(message = "cannot be changed")
    JsonNode type;

    @Schema(hidden = true)
    @Null(message = "cannot be changed")
    JsonNode externalId;

    @Schema(hidden = true)
    @Null(message = "cannot be changed")
    JsonNode currency;

    @Digits(integer = 17, fraction = 2)
    @PositiveOrZero
    @JsonDeserialize(using = MoneyDeserializer.class)
    BigDecimal total;

    Instant occurredAt;

    List<@NotNull @Valid TaxLine> taxLines;

    List<@NotNull @Valid FeeLine> feeLines;

    JsonNode metadata;
}
