package com.distributed.urlshortener.service;

import com.distributed.urlshortener.domain.dto.UrlAnalyticsResponse;
import com.distributed.urlshortener.domain.event.ClickEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KafkaAnalyticsIntegrationTest {

    @Test
    @DisplayName("Asynchronous Click Events stream processing should aggregate facets and timeseries accurately")
    void testClickEventStreamAggregation() {
        AnalyticsConsumerService consumerService = new AnalyticsConsumerService(null, null, null);
        AnalyticsQueryService queryService = new AnalyticsQueryService(consumerService, null);

        String shortCode = "eventStreamTest";
        String originalUrl = "https://example.org/stream";

        // Simulate 3 distinct click events from different IPs and devices
        ClickEvent event1 = new ClickEvent(
                UUID.randomUUID().toString(),
                shortCode,
                originalUrl,
                Instant.now(),
                "10.0.0.1",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0",
                "https://google.com",
                "US",
                "New York",
                "Desktop",
                "Chrome",
                "Windows"
        );

        ClickEvent event2 = new ClickEvent(
                UUID.randomUUID().toString(),
                shortCode,
                originalUrl,
                Instant.now(),
                "10.0.0.2",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Safari/604.1",
                "https://twitter.com",
                "US",
                "San Francisco",
                "Mobile",
                "Safari",
                "iOS"
        );

        ClickEvent event3 = new ClickEvent(
                UUID.randomUUID().toString(),
                shortCode,
                originalUrl,
                Instant.now(),
                "10.0.0.1", // Repeat IP to test unique visitor deduplication
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0",
                "Direct",
                "IN",
                "Bengaluru",
                "Desktop",
                "Chrome",
                "Windows"
        );

        // Ingest into consumer
        consumerService.processClickEvent(event1);
        consumerService.processClickEvent(event2);
        consumerService.processClickEvent(event3);

        // Query aggregated view
        UrlAnalyticsResponse analytics = queryService.getAnalytics(shortCode);

        assertNotNull(analytics);
        assertEquals(3, analytics.getTotalClicks(), "Total clicks should be 3");
        assertEquals(2, analytics.getUniqueVisitors(), "Unique visitors should be 2 (IPs 10.0.0.1 and 10.0.0.2)");
        assertEquals(2L, analytics.getClicksByDevice().get("Desktop"));
        assertEquals(1L, analytics.getClicksByDevice().get("Mobile"));
        assertEquals(2L, analytics.getClicksByBrowser().get("Chrome"));
        assertEquals(1L, analytics.getClicksByBrowser().get("Safari"));
        assertEquals(2L, analytics.getClicksByCountry().get("US"));
        assertEquals(1L, analytics.getClicksByCountry().get("IN"));
    }
}
