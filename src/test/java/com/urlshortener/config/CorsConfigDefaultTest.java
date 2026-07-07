package com.urlshortener.config;

import com.urlshortener.service.AnalyticsService;
import com.urlshortener.service.QrCodeService;
import com.urlshortener.service.ShortUrlService;
import com.urlshortener.web.ShortUrlController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShortUrlController.class)
@Import(CorsConfig.class)
class CorsConfigDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShortUrlService shortUrlService;

    @MockBean
    private QrCodeService qrCodeService;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void rejectsAnyOriginWhenNoneConfigured() throws Exception {
        mockMvc.perform(options("/api/urls")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
