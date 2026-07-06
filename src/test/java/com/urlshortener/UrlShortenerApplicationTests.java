package com.urlshortener;

import com.urlshortener.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class UrlShortenerApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Verifies the Spring context boots, Flyway migrations apply, and the
        // app connects to a real Testcontainers-managed Postgres instance.
    }
}
