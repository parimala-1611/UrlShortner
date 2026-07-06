package com.urlshortener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShortUrlEndToEndTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shortenThenRedirectIncrementsClickCountAndStatsReflectIt() throws Exception {
        String originalUrl = "https://example.com/e2e-test-page";

        String shortCode = shorten(originalUrl, null);

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound());

        mockMvc.perform(get("/api/urls/" + shortCode))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.clickCount").value(2));
    }

    @Test
    void expiredLinkReturnsGone() throws Exception {
        String originalUrl = "https://example.com/e2e-expired-page";
        String shortCode = shorten(originalUrl, OffsetDateTime.now().minusMinutes(1));

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone());
    }

    @Test
    void shorteningTheSameUrlTwiceReturnsTheSameCode() throws Exception {
        String originalUrl = "https://example.com/e2e-dedup-page";

        String firstCode = shorten(originalUrl, null);
        String secondCode = shorten(originalUrl, null);

        assertThat(secondCode).isEqualTo(firstCode);
    }

    private String shorten(String url, OffsetDateTime expiresAt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new ShortenRequestJson(url, expiresAt));

        String responseBody = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        return json.get("shortCode").asText();
    }

    private record ShortenRequestJson(String url, OffsetDateTime expiresAt) {
    }
}
