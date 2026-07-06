package com.urlshortener.service;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.service.exception.ShortCodeGenerationException;
import com.urlshortener.service.exception.ShortUrlExpiredException;
import com.urlshortener.service.exception.ShortUrlNotFoundException;
import com.urlshortener.util.UrlNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class ShortUrlService {

    public static final int MAX_COLLISION_ATTEMPTS = 5;

    private final ShortUrlRepository repository;
    private final UrlNormalizer urlNormalizer;
    private final ShortCodeGenerator codeGenerator;

    public ShortUrlService(ShortUrlRepository repository, UrlNormalizer urlNormalizer, ShortCodeGenerator codeGenerator) {
        this.repository = repository;
        this.urlNormalizer = urlNormalizer;
        this.codeGenerator = codeGenerator;
    }

    @Transactional
    public ShortUrl shorten(String rawUrl, OffsetDateTime expiresAt) {
        String normalizedUrl = urlNormalizer.normalize(rawUrl);

        return repository.findByOriginalUrl(normalizedUrl)
                .orElseGet(() -> createShortUrl(normalizedUrl, expiresAt));
    }

    private ShortUrl createShortUrl(String normalizedUrl, OffsetDateTime expiresAt) {
        for (int attempt = 0; attempt <= MAX_COLLISION_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate(normalizedUrl, attempt);
            Optional<ShortUrl> existing = repository.findByShortCode(code);

            if (existing.isEmpty()) {
                return repository.save(new ShortUrl(normalizedUrl, code, expiresAt));
            }
            if (existing.get().getOriginalUrl().equals(normalizedUrl)) {
                return existing.get();
            }
        }

        throw new ShortCodeGenerationException(
                "Could not generate a unique short code after " + (MAX_COLLISION_ATTEMPTS + 1) + " attempts");
    }

    @Transactional
    public ShortUrl resolve(String shortCode) {
        ShortUrl shortUrl = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (shortUrl.isExpired()) {
            throw new ShortUrlExpiredException(shortCode);
        }

        shortUrl.incrementClickCount();
        return shortUrl;
    }

    @Transactional(readOnly = true)
    public ShortUrl getStats(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
    }
}
