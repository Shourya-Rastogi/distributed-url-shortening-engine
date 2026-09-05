package com.distributed.urlshortener.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * PostgreSQL JPA Entity representing the indexed, persisted URL mapping.
 * Optimized with B-Tree indexes on short_code, expires_at, and created_at.
 */
@Entity
@Table(
        name = "url_mappings",
        indexes = {
                @Index(name = "idx_urls_short_code", columnList = "short_code", unique = true),
                @Index(name = "idx_urls_expires_at", columnList = "expires_at"),
                @Index(name = "idx_urls_created_at", columnList = "created_at")
        }
)
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 64, unique = true)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_custom", nullable = false)
    private boolean customAlias;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "total_clicks", nullable = false)
    private long totalClicks = 0;

    @Column(name = "created_by_ip", length = 64)
    private String createdByIp;

    public UrlMapping() {
    }

    public UrlMapping(String shortCode, String originalUrl, Instant createdAt, Instant expiresAt, boolean customAlias, String createdByIp) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.expiresAt = expiresAt;
        this.customAlias = customAlias;
        this.active = true;
        this.createdByIp = createdByIp;
        this.totalClicks = 0;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public String getCreatedByIp() {
        return createdByIp;
    }

    public void setCreatedByIp(String createdByIp) {
        this.createdByIp = createdByIp;
    }
}
