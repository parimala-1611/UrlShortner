package com.urlshortener.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

public record ShortenRequest(
        @NotBlank(message = "url must not be blank") String url,
        OffsetDateTime expiresAt,
        @Pattern(regexp = "^$|^[0-9a-zA-Z]{6,12}$", message = "customAlias must be 6-12 alphanumeric characters")
        String customAlias) {
}
