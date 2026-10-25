package com.navan.billing.model.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.navan.billing.model.MoneyDeserializer;
import com.navan.billing.model.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Value;

/** Metadata stays raw here; the service binds it to the class chosen by {@code type}. */
@Value
public class CreateTransactionRequest {

    @NotNull
    TransactionType type;

    @Schema(example = "flight-TXL-LHR-2026-03-11")
    @NotBlank
    @Size(max = 128)
    String externalId;

    @Schema(example = "EUR")
    @NotNull
    @Pattern(regexp = "[A-Z]{3}")
    String currency;

    @Schema(example = "2026-03-11T06:40:00Z")
    @NotNull
    Instant occurredAt;

    @Schema(type = "string", example = "220.00")
    @NotNull
    @Digits(integer = 17, fraction = 2)
    @PositiveOrZero
    @JsonDeserialize(using = MoneyDeserializer.class)
    BigDecimal total;

    @NotNull
    List<@NotNull @Valid TaxLine> taxLines;

    @NotNull
    List<@NotNull @Valid FeeLine> feeLines;

    @Schema(implementation = Object.class, description = """
            Shape depends on type. flight: origin, destination, fare, coupons[]. \
            hotel: name, address{street,city,country}, check_in, check_out, nights, room_rate_per_night, extras[]. \
            rail: legs[]. navan_fee: fee_kind (trip_fee | agent_call_fee), description, related_external_id.""",
            example = """
            {"origin":"TXL","destination":"LHR","fare":180.0,"coupons":[{"carrier":"BA","flight_number":"BA982",\
            "from":"TXL","to":"LHR","departure":"2026-03-11T07:00:00Z","arrival":"2026-03-11T08:20:00Z"}]}""")
    @NotNull
    JsonNode metadata;
}
