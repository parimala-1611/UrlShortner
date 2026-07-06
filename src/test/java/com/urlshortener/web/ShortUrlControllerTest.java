package com.urlshortener.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.service.ShortUrlService;
import com.urlshortener.service.exception.ShortUrlExpiredException;
import com.urlshortener.service.exception.ShortUrlNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

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

    @Test
    void shortenReturnsCreatedWithShortenedUrlDetails() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com/page", "abc12345", null);
        when(shortUrlService.shorten(eq("https://example.com/page"), any())).thenReturn(shortUrl);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequestJson("https://example.com/page", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc12345"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/page"))
                .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.endsWith("/abc12345")));
    }

    @Test
    void shortenRejectsBlankUrlWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequestJson("", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortenReturnsBadRequestWhenServiceRejectsInvalidUrl() throws Exception {
        when(shortUrlService.shorten(eq("not a valid url"), any()))
                .thenThrow(new IllegalArgumentException("Invalid URL: not a valid url"));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShortenRequestJson("not a valid url", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redirectReturnsFoundWithLocationHeader() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com/page", "abc12345", null);
        when(shortUrlService.resolve("abc12345")).thenReturn(shortUrl);

        mockMvc.perform(get("/abc12345"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/page"));
    }

    @Test
    void redirectReturnsNotFoundForUnknownCode() throws Exception {
        when(shortUrlService.resolve("missing")).thenThrow(new ShortUrlNotFoundException("missing"));

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectReturnsGoneForExpiredCode() throws Exception {
        when(shortUrlService.resolve("expired")).thenThrow(new ShortUrlExpiredException("expired"));

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

    private record ShortenRequestJson(String url, OffsetDateTime expiresAt) {
    }
}
