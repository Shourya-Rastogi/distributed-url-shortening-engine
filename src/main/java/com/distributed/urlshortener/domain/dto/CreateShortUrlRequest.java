package com.distributed.urlshortener.domain.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

/**
 * Request payload for creating a shortened URL.
 */
public class CreateShortUrlRequest {

    @NotBlank(message = "Original URL must not be blank")
    @URL(message = "Invalid URL format. Must start with http:// or https://")
    private String originalUrl;

    private String customAlias;

    private Long ttlSeconds;

    private Instant expiresAt;

    public CreateShortUrlRequest() {
    }

    public CreateShortUrlRequest(String originalUrl, String customAlias, Long ttlSeconds, Instant expiresAt) {
        this.originalUrl = originalUrl;
        this.customAlias = customAlias;
        this.ttlSeconds = ttlSeconds;
        this.expiresAt = expiresAt;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
