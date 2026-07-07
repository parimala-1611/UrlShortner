package com.urlshortener.web;

import com.urlshortener.model.ShortUrl;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.QrCodeService;
import com.urlshortener.service.ShortUrlService;
import com.urlshortener.web.dto.AnalyticsResponse;
import com.urlshortener.web.dto.ShortUrlStatsResponse;
import com.urlshortener.web.dto.ShortenRequest;
import com.urlshortener.web.dto.ShortenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ShortUrlController {

    private final ShortUrlService shortUrlService;
    private final QrCodeService qrCodeService;
    private final AnalyticsService analyticsService;

    public ShortUrlController(ShortUrlService shortUrlService, QrCodeService qrCodeService,
            AnalyticsService analyticsService) {
        this.shortUrlService = shortUrlService;
        this.qrCodeService = qrCodeService;
        this.analyticsService = analyticsService;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortUrl shortUrl = shortUrlService.shorten(request.url(), request.expiresAt(), request.customAlias());

        String shortUrlLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{shortCode}")
                .buildAndExpand(shortUrl.getShortCode())
                .toUriString();

        ShortenResponse response = new ShortenResponse(
                shortUrl.getShortCode(),
                shortUrlLink,
                shortUrl.getOriginalUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode,
            @RequestHeader(value = "Referer", required = false) String referrer) {
        ShortUrl shortUrl = shortUrlService.resolve(shortCode, referrer);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, shortUrl.getOriginalUrl())
                .build();
    }

    @GetMapping("/api/urls/{shortCode}")
    public ResponseEntity<ShortUrlStatsResponse> stats(@PathVariable String shortCode) {
        ShortUrl shortUrl = shortUrlService.getStats(shortCode);

        ShortUrlStatsResponse response = new ShortUrlStatsResponse(
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.getClickCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/urls/{shortCode}/qr")
    public ResponseEntity<byte[]> qr(@PathVariable String shortCode) {
        ShortUrl shortUrl = shortUrlService.getStats(shortCode);

        String shortUrlLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{shortCode}")
                .buildAndExpand(shortUrl.getShortCode())
                .toUriString();

        byte[] png = qrCodeService.generatePng(shortUrlLink);

        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/api/urls/{shortCode}/analytics")
    public ResponseEntity<AnalyticsResponse> analytics(@PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsService.getAnalytics(shortCode));
    }
}
