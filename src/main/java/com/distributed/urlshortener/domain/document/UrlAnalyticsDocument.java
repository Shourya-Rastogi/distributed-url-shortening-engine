package com.distributed.urlshortener.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * MongoDB Document maintaining real-time aggregated metrics per shortCode.
 * Updated atomically using MongoDB $inc and $addToSet operations in the Kafka consumer.
 */
@Document(collection = "url_analytics")
public class UrlAnalyticsDocument {

    @Id
    private String id; // shortCode

    private String originalUrl;
    private long totalClicks = 0;
    private Set<String> uniqueIps = new HashSet<>();
    private Map<String, Long> clicksByDate = new HashMap<>();       // e.g. "2026-08-30" -> 1520
    private Map<String, Long> clicksByHour = new HashMap<>();       // e.g. "2026-08-30T18" -> 140
    private Map<String, Long> clicksByCountry = new HashMap<>();    // e.g. "US" -> 500, "IN" -> 400
    private Map<String, Long> clicksByDevice = new HashMap<>();     // e.g. "Mobile" -> 800, "Desktop" -> 720
    private Map<String, Long> clicksByBrowser = new HashMap<>();    // e.g. "Chrome" -> 900, "Safari" -> 400
    private Map<String, Long> clicksByReferer = new HashMap<>();    // e.g. "Google" -> 600, "Twitter" -> 300
    private Instant firstClickedAt;
    private Instant lastClickedAt;

    public UrlAnalyticsDocument() {
    }

    public UrlAnalyticsDocument(String shortCode, String originalUrl) {
        this.id = shortCode;
        this.originalUrl = originalUrl;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public Set<String> getUniqueIps() {
        return uniqueIps;
    }

    public void setUniqueIps(Set<String> uniqueIps) {
        this.uniqueIps = uniqueIps;
    }

    public Map<String, Long> getClicksByDate() {
        return clicksByDate;
    }

    public void setClicksByDate(Map<String, Long> clicksByDate) {
        this.clicksByDate = clicksByDate;
    }

    public Map<String, Long> getClicksByHour() {
        return clicksByHour;
    }

    public void setClicksByHour(Map<String, Long> clicksByHour) {
        this.clicksByHour = clicksByHour;
    }

    public Map<String, Long> getClicksByCountry() {
        return clicksByCountry;
    }

    public void setClicksByCountry(Map<String, Long> clicksByCountry) {
        this.clicksByCountry = clicksByCountry;
    }

    public Map<String, Long> getClicksByDevice() {
        return clicksByDevice;
    }

    public void setClicksByDevice(Map<String, Long> clicksByDevice) {
        this.clicksByDevice = clicksByDevice;
    }

    public Map<String, Long> getClicksByBrowser() {
        return clicksByBrowser;
    }

    public void setClicksByBrowser(Map<String, Long> clicksByBrowser) {
        this.clicksByBrowser = clicksByBrowser;
    }

    public Map<String, Long> getClicksByReferer() {
        return clicksByReferer;
    }

    public void setClicksByReferer(Map<String, Long> clicksByReferer) {
        this.clicksByReferer = clicksByReferer;
    }

    public Instant getFirstClickedAt() {
        return firstClickedAt;
    }

    public void setFirstClickedAt(Instant firstClickedAt) {
        this.firstClickedAt = firstClickedAt;
    }

    public Instant getLastClickedAt() {
        return lastClickedAt;
    }

    public void setLastClickedAt(Instant lastClickedAt) {
        this.lastClickedAt = lastClickedAt;
    }
}
