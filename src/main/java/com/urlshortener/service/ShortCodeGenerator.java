package com.urlshortener.service;

import com.urlshortener.util.Base62Encoder;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class ShortCodeGenerator {

    public static final int CODE_LENGTH = 8;
    private static final BigInteger MODULUS = BigInteger.valueOf(62).pow(CODE_LENGTH);

    private final Base62Encoder base62Encoder;

    public ShortCodeGenerator(Base62Encoder base62Encoder) {
        this.base62Encoder = base62Encoder;
    }

    public String generate(String normalizedUrl, int salt) {
        String input = salt == 0 ? normalizedUrl : normalizedUrl + "#" + salt;
        byte[] hash = sha256(input);
        BigInteger value = new BigInteger(1, hash).mod(MODULUS);
        return base62Encoder.encode(value, CODE_LENGTH);
    }

    private byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
