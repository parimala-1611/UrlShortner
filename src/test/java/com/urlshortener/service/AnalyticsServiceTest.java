package com.urlshortener.service;

import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.service.exception.ShortUrlNotFoundException;
import com.urlshortener.web.dto.AnalyticsResponse;
import com.urlshortener.web.dto.DailyClickCount;
import com.urlshortener.web.dto.ReferrerCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    private AnalyticsService realService() {
        return new AnalyticsService(shortUrlRepository, clickEventRepository);
    }

    @Test
    void throwsNotFoundForUnknownCode() {
        when(shortUrlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> realService().getAnalytics("missing"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }

    @Test
    void returnsTotalClicksDailyCountsAndTopReferrers() throws Exception {
        ShortUrl shortUrl = new ShortUrl("https://example.com", "abc12345", null);
        setId(shortUrl, 1L);
        shortUrl.incrementClickCount();
        shortUrl.incrementClickCount();
        shortUrl.incrementClickCount();

        OffsetDateTime day1 = OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime day2 = OffsetDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC);

        List<ClickEvent> events = List.of(
                clickEventAt(1L, "https://twitter.com", day1),
                clickEventAt(1L, "https://twitter.com", day1),
                clickEventAt(1L, null, day2)
        );

        when(shortUrlRepository.findByShortCode("abc12345")).thenReturn(Optional.of(shortUrl));
        when(clickEventRepository.findByShortUrlId(1L)).thenReturn(events);

        AnalyticsResponse response = realService().getAnalytics("abc12345");

        assertThat(response.shortCode()).isEqualTo("abc12345");
        assertThat(response.totalClicks()).isEqualTo(3);
        assertThat(response.dailyClickCounts()).containsExactly(
                new DailyClickCount(LocalDate.of(2026, 1, 1), 2),
                new DailyClickCount(LocalDate.of(2026, 1, 2), 1));
        assertThat(response.topReferrers()).containsExactly(
                new ReferrerCount("https://twitter.com", 2),
                new ReferrerCount("(direct)", 1));
    }

    @Test
    void aggregateByDaySortsChronologically() {
        OffsetDateTime day2 = OffsetDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime day1 = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        List<ClickEvent> events = List.of(
                clickEventAt(1L, null, day2),
                clickEventAt(1L, null, day1));

        List<DailyClickCount> result = AnalyticsService.aggregateByDay(events);

        assertThat(result).containsExactly(
                new DailyClickCount(LocalDate.of(2026, 1, 1), 1),
                new DailyClickCount(LocalDate.of(2026, 1, 2), 1));
    }

    @Test
    void aggregateByReferrerLimitsToTopN() {
        OffsetDateTime now = OffsetDateTime.now();
        List<ClickEvent> events = List.of(
                clickEventAt(1L, "a", now),
                clickEventAt(1L, "a", now),
                clickEventAt(1L, "b", now),
                clickEventAt(1L, "c", now));

        List<ReferrerCount> result = AnalyticsService.aggregateByReferrer(events, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(new ReferrerCount("a", 2));
    }

    private static ClickEvent clickEventAt(Long shortUrlId, String referrer, OffsetDateTime clickedAt) {
        ClickEvent event = new ClickEvent(shortUrlId, referrer);
        try {
            Field field = ClickEvent.class.getDeclaredField("clickedAt");
            field.setAccessible(true);
            field.set(event, clickedAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return event;
    }

    private static void setId(ShortUrl shortUrl, Long id) throws Exception {
        Field field = ShortUrl.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(shortUrl, id);
    }
}
