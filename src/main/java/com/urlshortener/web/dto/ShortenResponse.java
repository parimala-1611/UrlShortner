package com.urlshortener.web.dto;

import java.time.OffsetDateTime;

public record ShortenResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt) {
}
