package com.urlshortener.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public record ShortenRequest(
        @NotBlank(message = "url must not be blank") String url,
        OffsetDateTime expiresAt) {
}
