package com.urlshortener.service.exception;

public class ShortUrlExpiredException extends RuntimeException {

    public ShortUrlExpiredException(String shortCode) {
        super("Short URL has expired for code: " + shortCode);
    }
}
