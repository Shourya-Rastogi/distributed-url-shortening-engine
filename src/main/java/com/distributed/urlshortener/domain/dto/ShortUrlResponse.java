package com.distributed.urlshortener.domain.dto;

import java.time.Instant;

/**
 * Response DTO containing short URL details.
 */
public class ShortUrlResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean customAlias;
    private long totalClicks;

    public ShortUrlResponse() {
    }

    public ShortUrlResponse(String shortCode, String shortUrl, String originalUrl,
                            Instant createdAt, Instant expiresAt, boolean customAlias, long totalClicks) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.customAlias = customAlias;
        this.totalClicks = totalClicks;
    }

    // Getters and Setters
    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(boolean customAlias) {
        this.customAlias = customAlias;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }
}
