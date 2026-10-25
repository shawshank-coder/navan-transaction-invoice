package com.navan.billing.model.transaction.metadata;

import com.navan.billing.model.enums.FeeKind;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class NavanFeeMetadata implements TransactionMetadata {

    @NotNull
    FeeKind feeKind;

    String description;

    String relatedExternalId;
}
