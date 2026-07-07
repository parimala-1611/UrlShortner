package com.urlshortener.service;

import com.urlshortener.model.ClickEvent;
import com.urlshortener.model.ShortUrl;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.ShortUrlRepository;
import com.urlshortener.service.exception.ShortUrlNotFoundException;
import com.urlshortener.web.dto.AnalyticsResponse;
import com.urlshortener.web.dto.DailyClickCount;
import com.urlshortener.web.dto.ReferrerCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    static final int TOP_REFERRERS_LIMIT = 10;
    private static final String DIRECT_REFERRER_LABEL = "(direct)";

    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(ShortUrlRepository shortUrlRepository, ClickEventRepository clickEventRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        List<ClickEvent> events = clickEventRepository.findByShortUrlId(shortUrl.getId());

        return new AnalyticsResponse(
                shortCode,
                shortUrl.getClickCount(),
                aggregateByDay(events),
                aggregateByReferrer(events, TOP_REFERRERS_LIMIT));
    }

    static List<DailyClickCount> aggregateByDay(List<ClickEvent> events) {
        Map<java.time.LocalDate, Long> counts = events.stream()
                .collect(Collectors.groupingBy(e -> e.getClickedAt().toLocalDate(), Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DailyClickCount(e.getKey(), e.getValue()))
                .toList();
    }

    static List<ReferrerCount> aggregateByReferrer(List<ClickEvent> events, int limit) {
        Map<String, Long> counts = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getReferrer() == null || e.getReferrer().isBlank()
                                ? DIRECT_REFERRER_LABEL : e.getReferrer(),
                        Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new ReferrerCount(e.getKey(), e.getValue()))
                .toList();
    }
}
