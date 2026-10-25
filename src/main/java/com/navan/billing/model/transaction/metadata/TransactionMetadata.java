package com.navan.billing.model.transaction.metadata;

public sealed interface TransactionMetadata
        permits FlightMetadata, HotelMetadata, RailMetadata, NavanFeeMetadata {
}
