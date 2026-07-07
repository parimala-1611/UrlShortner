package com.urlshortener.web.dto;

import java.util.List;

public record AnalyticsResponse(
        String shortCode,
        long totalClicks,
        List<DailyClickCount> dailyClickCounts,
        List<ReferrerCount> topReferrers) {
}
