package com.urlshortener.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.QrCodeService;
import com.urlshortener.service.ShortUrlService;
import com.urlshortener.service.exception.ShortUrlExpiredException;
import com.urlshortener.service.exception.ShortUrlNotFoundException;
import com.urlshortener.web.dto.AnalyticsResponse;
import com.urlshortener.web.dto.DailyClickCount;
import com.urlshortener.web.dto.ReferrerCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShortUrlController.class)
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShortUrlService shortUrlService;

    @MockBean
    private QrCodeService qrCodeService;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void shortenReturnsCreatedWithShortenedUrlDetails() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com/page", "abc12345", null);
        when(shortUrlService.shorten(eq("https://example.com/page"), any(), any())).thenReturn(shortUrl);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequestJson("https://example.com/page", null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc12345"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/page"))
                .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.endsWith("/abc12345")));
    }

    @Test
    void shortenRejectsBlankUrlWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequestJson("", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenReturnsBadRequestWhenServiceRejectsInvalidUrl() throws Exception {
        when(shortUrlService.shorten(eq("not a valid url"), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid URL: not a valid url"));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequestJson("not a valid url", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenRejectsCustomAliasShorterThanSixCharacters() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ShortenRequestJson("https://example.com/page", null, "abc"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenUsesProvidedCustomAlias() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com/page", "myalias123", null);
        when(shortUrlService.shorten(eq("https://example.com/page"), any(), eq("myalias123"))).thenReturn(shortUrl);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ShortenRequestJson("https://example.com/page", null, "myalias123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("myalias123"));
    }

    @Test
    void redirectReturnsFoundWithLocationHeader() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com/page", "abc12345", null);
        when(shortUrlService.resolve(eq("abc12345"), any())).thenReturn(shortUrl);

        mockMvc.perform(get("/abc12345"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/page"));
    }

    @Test
    void redirectPassesRefererHeaderThrough() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com/page", "abc12345", null);
        when(shortUrlService.resolve("abc12345", "https://twitter.com")).thenReturn(shortUrl);

        mockMvc.perform(get("/abc12345").header("Referer", "https://twitter.com"))
                .andExpect(status().isFound());
    }

    @Test
    void redirectReturnsNotFoundForUnknownCode() throws Exception {
        when(shortUrlService.resolve(eq("missing"), any())).thenThrow(new ShortUrlNotFoundException("missing"));

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectReturnsGoneForExpiredCode() throws Exception {
        when(shortUrlService.resolve(eq("expired"), any())).thenThrow(new ShortUrlExpiredException("expired"));

        mockMvc.perform(get("/expired"))
                .andExpect(status().isGone());
    }

    @Test
    void statsReturnsOkWithUrlDetails() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com/page", "abc12345", OffsetDateTime.now().plusDays(1));
        shortUrl.incrementClickCount();
        when(shortUrlService.getStats("abc12345")).thenReturn(shortUrl);

        mockMvc.perform(get("/api/urls/abc12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc12345"))
                .andExpect(jsonPath("$.clickCount").value(1));
    }

    @Test
    void statsReturnsNotFoundForUnknownCode() throws Exception {
        when(shortUrlService.getStats("missing")).thenThrow(new ShortUrlNotFoundException("missing"));

        mockMvc.perform(get("/api/urls/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void qrReturnsPngImageForExistingCode() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com/page", "abc12345", null);
        byte[] fakePng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        when(shortUrlService.getStats("abc12345")).thenReturn(shortUrl);
        when(qrCodeService.generatePng(org.mockito.ArgumentMatchers.anyString())).thenReturn(fakePng);

        mockMvc.perform(get("/api/urls/abc12345/qr"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(fakePng));
    }

    @Test
    void qrReturnsNotFoundForUnknownCode() throws Exception {
        when(shortUrlService.getStats("missing")).thenThrow(new ShortUrlNotFoundException("missing"));

        mockMvc.perform(get("/api/urls/missing/qr"))
                .andExpect(status().isNotFound());
    }

    @Test
    void qrWorksForExpiredCode() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com/page", "expired1",
                OffsetDateTime.now().minusDays(1));
        byte[] fakePng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        when(shortUrlService.getStats("expired1")).thenReturn(shortUrl);
        when(qrCodeService.generatePng(org.mockito.ArgumentMatchers.anyString())).thenReturn(fakePng);

        mockMvc.perform(get("/api/urls/expired1/qr"))
                .andExpect(status().isOk());
    }

    @Test
    void analyticsReturnsOkWithBreakdown() throws Exception {
        AnalyticsResponse response = new AnalyticsResponse(
                "abc12345", 3L,
                List.of(new DailyClickCount(LocalDate.of(2026, 1, 1), 3L)),
                List.of(new ReferrerCount("https://twitter.com", 2L), new ReferrerCount("(direct)", 1L)));
        when(analyticsService.getAnalytics("abc12345")).thenReturn(response);

        mockMvc.perform(get("/api/urls/abc12345/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc12345"))
                .andExpect(jsonPath("$.totalClicks").value(3))
                .andExpect(jsonPath("$.dailyClickCounts[0].count").value(3))
                .andExpect(jsonPath("$.topReferrers[0].referrer").value("https://twitter.com"));
    }

    @Test
    void analyticsReturnsNotFoundForUnknownCode() throws Exception {
        when(analyticsService.getAnalytics("missing")).thenThrow(new ShortUrlNotFoundException("missing"));

        mockMvc.perform(get("/api/urls/missing/analytics"))
                .andExpect(status().isNotFound());
    }

    private record ShortenRequestJson(String url, OffsetDateTime expiresAt, String customAlias) {
    }
}
