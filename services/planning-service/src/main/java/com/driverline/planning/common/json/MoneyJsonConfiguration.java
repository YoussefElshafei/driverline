package com.driverline.planning.common.json;

import java.math.BigDecimal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Writes every BigDecimal as a JSON string ("1234.50") instead of a JSON number (1234.50).
 * JavaScript parses JSON numbers into binary floating point, which can't hold most cent values exactly,
 * so the frontend receives strings and does its arithmetic in whole cents.
 */
@Configuration
public class MoneyJsonConfiguration {

    @Bean
    JacksonModule bigDecimalAsStringModule() {
        return new SimpleModule("BigDecimalAsString").addSerializer(BigDecimal.class, new PlainBigDecimalSerializer());
    }

    /** Uses toPlainString so values are never written in scientific notation such as "1E+3". */
    static class PlainBigDecimalSerializer extends StdSerializer<BigDecimal> {

        PlainBigDecimalSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal value, JsonGenerator generator, SerializationContext context) {
            generator.writeString(value.toPlainString());
        }
    }
}
