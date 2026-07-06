package com.urlshortener.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortUrlTest {

    @Test
    void constructorPopulatesFieldsAndDefaultsClickCountToZero() {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(1);

        ShortUrl shortUrl = new ShortUrl("https://example.com", "abc123", expiresAt);

        assertThat(shortUrl.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(shortUrl.getShortCode()).isEqualTo("abc123");
        assertThat(shortUrl.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(shortUrl.getClickCount()).isZero();
    }

    @Test
    void constructorRejectsNullOriginalUrl() {
        assertThatThrownBy(() -> new ShortUrl(null, "abc123", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorRejectsNullShortCode() {
        assertThatThrownBy(() -> new ShortUrl("https://example.com", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void incrementClickCountIncreasesCountByOne() {
        ShortUrl shortUrl = new ShortUrl("https://example.com", "abc123", null);

        shortUrl.incrementClickCount();
        shortUrl.incrementClickCount();

        assertThat(shortUrl.getClickCount()).isEqualTo(2L);
    }

    @Test
    void onCreateSetsCreatedAtWhenAbsent() {
        ShortUrl shortUrl = new ShortUrl("https://example.com", "abc123", null);

        shortUrl.onCreate();

        assertThat(shortUrl.getCreatedAt()).isNotNull();
    }
}
