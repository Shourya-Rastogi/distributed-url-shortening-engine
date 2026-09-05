package com.distributed.urlshortener.domain.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB Document storing raw time-series click events for granular analytics and audit trails.
 */
@Document(collection = "click_events")
@CompoundIndex(name = "idx_clicks_code_time", def = "{'shortCode': 1, 'timestamp': -1}")
public class ClickEventDocument {

    @Id
    private String id;

    @Indexed
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

    public ClickEventDocument() {
    }

    public ClickEventDocument(String shortCode, String originalUrl, Instant timestamp, String ipAddress,
                              String userAgent, String referer, String country, String city,
                              String deviceType, String browser, String os) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
        this.country = country;
        this.city = city;
        this.deviceType = deviceType;
        this.browser = browser;
        this.os = os;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
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
