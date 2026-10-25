package com.navan.billing.model.transaction.metadata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Value;

@Value
public class RailMetadata implements TransactionMetadata {

    @NotEmpty
    List<@NotNull @Valid RailLeg> legs;
}
