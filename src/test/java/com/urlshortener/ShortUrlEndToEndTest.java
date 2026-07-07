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

    @Test
    void shorteningTheSameUrlThreeTimesAlwaysReturnsTheSameCode() throws Exception {
        String originalUrl = "https://example.com/e2e-dedup-triple";

        String firstCode = shorten(originalUrl, null);
        String secondCode = shorten(originalUrl, null);
        String thirdCode = shorten(originalUrl, null);

        assertThat(secondCode).isEqualTo(firstCode);
        assertThat(thirdCode).isEqualTo(firstCode);
    }

    @Test
    void rejectsJumbledNonUrlText() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new ShortenRequestJson("asdkjhasdkjh", null, null));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFileLikeHost() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new ShortenRequestJson("malware.exe", null, null));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsProperUrl() throws Exception {
        String shortCode = shorten("https://example.com/e2e-proper-url", null);

        assertThat(shortCode).isNotBlank();
    }

    @Test
    void usesAvailableCustomAlias() throws Exception {
        String shortCode = shortenWithAlias("https://example.com/e2e-alias-page", "mylink12");

        assertThat(shortCode).isEqualTo("mylink12");
    }

    @Test
    void fallsBackToGeneratedCodeWhenAliasAlreadyTaken() throws Exception {
        shortenWithAlias("https://example.com/e2e-alias-first", "taken1234");

        String secondCode = shortenWithAlias("https://example.com/e2e-alias-second", "taken1234");

        assertThat(secondCode).isNotEqualTo("taken1234");
        assertThat(secondCode).isNotBlank();
    }

    @Test
    void defaultExpiryIsAppliedWhenNotProvided() throws Exception {
        String shortCode = shorten("https://example.com/e2e-default-expiry", null);

        String responseBody = mockMvc.perform(get("/api/urls/" + shortCode))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        assertThat(json.get("expiresAt").isNull()).isFalse();
    }

    private String shorten(String url, OffsetDateTime expiresAt) throws Exception {
        return shorten(url, expiresAt, null);
    }

    private String shortenWithAlias(String url, String customAlias) throws Exception {
        return shorten(url, null, customAlias);
    }

    private String shorten(String url, OffsetDateTime expiresAt, String customAlias) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new ShortenRequestJson(url, expiresAt, customAlias));

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

    private record ShortenRequestJson(String url, OffsetDateTime expiresAt, String customAlias) {
    }
}
