package com.distributed.urlshortener.domain.event;

import java.io.Serializable;
import java.time.Instant;

/**
 * Click Event emitted asynchronously to Kafka topic 'url-clicks' upon redirect.
 * Fully decoupled from the user's redirect critical path.
 */
public class ClickEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String shortCode;
    private String originalUrl;
    private Instant timestamp;
    private String ipAddress;
    private String userAgent;
    private String referer;
    private String country;
    private String city;
    private String deviceType;
    private String browser;
    private String os;

    public ClickEvent() {
    }

    public ClickEvent(String eventId, String shortCode, String originalUrl, Instant timestamp,
                      String ipAddress, String userAgent, String referer, String country,
                      String city, String deviceType, String browser, String os) {
        this.eventId = eventId;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer != null && !referer.isBlank() ? referer : "Direct";
        this.country = country != null ? country : "Unknown";
        this.city = city != null ? city : "Unknown";
        this.deviceType = deviceType != null ? deviceType : "Desktop";
        this.browser = browser != null ? browser : "Other";
        this.os = os != null ? os : "Other";
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
