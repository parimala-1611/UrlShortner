package com.urlshortener.web.dto;

import java.time.LocalDate;

public record DailyClickCount(LocalDate date, long count) {
}
