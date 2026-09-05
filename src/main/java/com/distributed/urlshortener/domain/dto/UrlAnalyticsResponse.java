package com.distributed.urlshortener.domain.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Aggregated analytics response for a shortCode.
 */
public class UrlAnalyticsResponse {

    private String shortCode;
    private String originalUrl;
    private long totalClicks;
    private int uniqueVisitors;
    private Map<String, Long> clicksByDate;
    private Map<String, Long> clicksByHour;
    private Map<String, Long> clicksByCountry;
    private Map<String, Long> clicksByDevice;
    private Map<String, Long> clicksByBrowser;
    private Map<String, Long> clicksByReferer;
    private Instant firstClickedAt;
    private Instant lastClickedAt;

    public UrlAnalyticsResponse() {
    }

    // Getters and Setters
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

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public int getUniqueVisitors() {
        return uniqueVisitors;
    }

    public void setUniqueVisitors(int uniqueVisitors) {
        this.uniqueVisitors = uniqueVisitors;
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
