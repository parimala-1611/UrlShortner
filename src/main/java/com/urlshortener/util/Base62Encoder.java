package com.urlshortener.util;

import java.math.BigInteger;

public class Base62Encoder {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length());

    public String encode(BigInteger value, int length) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("value must not be negative: " + value);
        }

        StringBuilder result = new StringBuilder();
        BigInteger remaining = value;
        while (remaining.signum() > 0) {
            BigInteger[] quotientAndRemainder = remaining.divideAndRemainder(BASE);
            result.append(ALPHABET.charAt(quotientAndRemainder[1].intValue()));
            remaining = quotientAndRemainder[0];
        }
        result.reverse();

        if (result.length() > length) {
            throw new IllegalArgumentException(
                    "value " + value + " does not fit in " + length + " base62 characters");
        }

        while (result.length() < length) {
            result.insert(0, ALPHABET.charAt(0));
        }

        return result.toString();
    }
}
