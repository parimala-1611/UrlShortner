package com.urlshortener.repository;

import com.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    Optional<ShortUrl> findByOriginalUrl(String originalUrl);

    long deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
