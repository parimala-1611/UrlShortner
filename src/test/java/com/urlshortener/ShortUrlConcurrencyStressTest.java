package com.urlshortener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires many concurrent requests at the running app to prove dedup and
 * short-code generation stay correct under contention, not just sequentially.
 */
class ShortUrlConcurrencyStressTest extends AbstractIntegrationTest {

    private static final int THREAD_COUNT = 50;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void concurrentRequestsForSameUrlAllReturnTheSameShortCode() throws Exception {
        String url = "https://example.com/stress-dedup-" + System.nanoTime();

        List<String> codes = runConcurrently(THREAD_COUNT, i -> shorten(url));

        assertThat(Set.copyOf(codes)).hasSize(1);
    }

    @Test
    void concurrentRequestsForDistinctUrlsAllSucceedWithUniqueCodes() throws Exception {
        long nonce = System.nanoTime();

        List<String> codes = runConcurrently(THREAD_COUNT,
                i -> shorten("https://example.com/stress-unique-" + nonce + "-" + i));

        assertThat(codes).hasSize(THREAD_COUNT);
        assertThat(Set.copyOf(codes)).hasSize(THREAD_COUNT);
    }

    private List<String> runConcurrently(int threadCount, java.util.function.IntFunction<String> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Callable<String>> callables = IntStream.range(0, threadCount)
                    .<Callable<String>>mapToObj(i -> () -> {
                        ready.countDown();
                        start.await();
                        return task.apply(i);
                    })
                    .collect(Collectors.toList());

            List<Future<String>> futures = callables.stream().map(pool::submit).collect(Collectors.toList());

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            return futures.stream().map(f -> {
                try {
                    return f.get(20, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        } finally {
            pool.shutdown();
        }
    }

    private String shorten(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"url\":\"" + url + "\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/urls", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("shortCode").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
