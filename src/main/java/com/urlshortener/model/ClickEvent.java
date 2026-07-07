package com.urlshortener.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_url_id", nullable = false)
    private Long shortUrlId;

    @Column(name = "clicked_at", nullable = false)
    private OffsetDateTime clickedAt;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    protected ClickEvent() {
        // required by JPA
    }

    public ClickEvent(Long shortUrlId, String referrer) {
        this.shortUrlId = Objects.requireNonNull(shortUrlId, "shortUrlId must not be null");
        this.referrer = referrer;
    }

    @PrePersist
    void onCreate() {
        if (clickedAt == null) {
            clickedAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getShortUrlId() {
        return shortUrlId;
    }

    public OffsetDateTime getClickedAt() {
        return clickedAt;
    }

    public String getReferrer() {
        return referrer;
    }
}
