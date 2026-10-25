package com.navan.billing.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.billing.model.transaction.metadata.FlightMetadata;
import com.navan.billing.model.transaction.metadata.HotelMetadata;
import com.navan.billing.model.transaction.metadata.NavanFeeMetadata;
import com.navan.billing.model.transaction.metadata.RailMetadata;
import com.navan.billing.model.transaction.metadata.TransactionMetadata;

public enum TransactionType {
    @JsonProperty("flight") FLIGHT("flight", FlightMetadata.class),
    @JsonProperty("hotel") HOTEL("hotel", HotelMetadata.class),
    @JsonProperty("rail") RAIL("rail", RailMetadata.class),
    @JsonProperty("navan_fee") NAVAN_FEE("navan_fee", NavanFeeMetadata.class);

    private final String code;
    private final Class<? extends TransactionMetadata> metadataClass;

    TransactionType(String code, Class<? extends TransactionMetadata> metadataClass) {
        this.code = code;
        this.metadataClass = metadataClass;
    }

    public String code() {
        return code;
    }

    public Class<? extends TransactionMetadata> metadataClass() {
        return metadataClass;
    }

    public static TransactionType fromCode(String code) {
        for (TransactionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown transaction type: " + code);
    }
}
