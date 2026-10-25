package com.navan.billing.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Money is written as a JSON string so no client or JSON column ever round-trips it through a double.
    @Bean
    Jackson2ObjectMapperBuilderCustomizer bigDecimalAsString() {
        return builder -> builder.postConfigurer(mapper -> mapper.configOverride(BigDecimal.class)
                .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING)));
    }
}
