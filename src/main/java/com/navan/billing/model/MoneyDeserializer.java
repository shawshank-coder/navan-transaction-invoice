package com.navan.billing.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.math.BigDecimal;

/** Pads money to scale 2 (180 -> 180.00) without rounding; values with more decimals are left for @Digits to reject. */
public class MoneyDeserializer extends StdDeserializer<BigDecimal> {

    public MoneyDeserializer() {
        super(BigDecimal.class);
    }

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        BigDecimal value = context.readValue(parser, BigDecimal.class);
        if (value == null || value.stripTrailingZeros().scale() > 2) {
            return value;
        }
        return value.setScale(2);
    }
}
