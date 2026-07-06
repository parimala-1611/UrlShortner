package com.urlshortener.web.dto;

import java.time.OffsetDateTime;

public record ShortUrlStatsResponse(
        String shortCode,
        String originalUrl,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        long clickCount) {
}
