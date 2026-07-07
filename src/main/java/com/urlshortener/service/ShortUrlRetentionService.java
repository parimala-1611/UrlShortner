package com.urlshortener.service;

import com.urlshortener.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
public class ShortUrlRetentionService {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlRetentionService.class);

    private final ShortUrlRepository repository;
    private final long purgeAfterDays;

    public ShortUrlRetentionService(ShortUrlRepository repository,
            @Value("${app.retention.purge-after-days:90}") long purgeAfterDays) {
        this.repository = repository;
        this.purgeAfterDays = purgeAfterDays;
    }

    @Scheduled(cron = "${app.retention.purge-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredLinks() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(purgeAfterDays);
        long deleted = repository.deleteByExpiresAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} short URLs that expired before {}", deleted, cutoff);
        }
    }
}
