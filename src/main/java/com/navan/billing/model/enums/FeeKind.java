package com.navan.billing.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FeeKind {
    @JsonProperty("trip_fee") TRIP_FEE,
    @JsonProperty("agent_call_fee") AGENT_CALL_FEE
}
