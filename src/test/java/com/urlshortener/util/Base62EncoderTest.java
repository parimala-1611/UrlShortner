package com.urlshortener.util;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encodesZeroAsPaddedZeros() {
        assertThat(encoder.encode(BigInteger.ZERO, 4)).isEqualTo("0000");
    }

    @Test
    void encodesSingleDigitValues() {
        assertThat(encoder.encode(BigInteger.valueOf(1), 1)).isEqualTo("1");
        assertThat(encoder.encode(BigInteger.valueOf(61), 1)).isEqualTo("Z");
    }

    @Test
    void encodesMultiDigitValues() {
        // 62 = 1*62 + 0 -> "10"
        assertThat(encoder.encode(BigInteger.valueOf(62), 2)).isEqualTo("10");
    }

    @Test
    void padsShorterValuesToRequestedLength() {
        assertThat(encoder.encode(BigInteger.valueOf(1), 4)).isEqualTo("0001");
    }

    @Test
    void rejectsNegativeValues() {
        assertThatThrownBy(() -> encoder.encode(BigInteger.valueOf(-1), 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsValuesThatDoNotFitInRequestedLength() {
        // 62^2 = 3844 requires 3 digits, doesn't fit in length 2
        assertThatThrownBy(() -> encoder.encode(BigInteger.valueOf(3844), 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
