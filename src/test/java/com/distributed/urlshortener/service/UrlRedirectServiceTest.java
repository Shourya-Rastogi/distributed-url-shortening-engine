package com.distributed.urlshortener.service;

import com.distributed.urlshortener.FakeUrlMappingRepository;
import com.distributed.urlshortener.domain.entity.UrlMapping;
import com.distributed.urlshortener.exception.UrlExpiredException;
import com.distributed.urlshortener.exception.UrlNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UrlRedirectServiceTest {

    private CacheService cacheService;
    private FakeUrlMappingRepository urlMappingRepository;
    private AnalyticsConsumerService analyticsConsumerService;
    private AnalyticsProducerService analyticsProducerService;
    private UrlRedirectService urlRedirectService;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService(null, new ObjectMapper(), Duration.ofDays(1), Duration.ofSeconds(30));
        urlMappingRepository = new FakeUrlMappingRepository();
        analyticsConsumerService = new AnalyticsConsumerService(null, null, null);
        analyticsProducerService = new AnalyticsProducerService(null, analyticsConsumerService, "url-clicks");
        urlRedirectService = new UrlRedirectService(cacheService, urlMappingRepository, analyticsProducerService);
    }

    @Test
    @DisplayName("Cache hit should return target URL immediately")
    void testCacheHitResolution() {
        cacheService.put("fastCode", "https://fast.example.com", Instant.now().plusSeconds(3600), true);

        String target = urlRedirectService.resolveAndRedirect("fastCode", null);
        assertEquals("https://fast.example.com", target);
    }

    @Test
    @DisplayName("Cache miss should fallback to database and backfill cache")
    void testCacheMissFallbackToDb() {
        UrlMapping mapping = new UrlMapping("dbCode", "https://db.example.com", Instant.now(), Instant.now().plusSeconds(3600), false, "127.0.0.1");
        urlMappingRepository.save(mapping);

        String target = urlRedirectService.resolveAndRedirect("dbCode", null);
        assertEquals("https://db.example.com", target);

        // Verify cache was backfilled
        assertNotNull(cacheService.get("dbCode"));
        assertEquals("https://db.example.com", cacheService.get("dbCode").getOriginalUrl());
    }

    @Test
    @DisplayName("Expired URL should throw UrlExpiredException (HTTP 410)")
    void testExpiredUrlThrowsException() {
        UrlMapping expiredMapping = new UrlMapping("oldCode", "https://expired.example.com", Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600), false, "127.0.0.1");
        urlMappingRepository.save(expiredMapping);

        assertThrows(UrlExpiredException.class, () -> urlRedirectService.resolveAndRedirect("oldCode", null));
    }

    @Test
    @DisplayName("Non-existent URL should throw UrlNotFoundException and activate negative caching")
    void testNonExistentUrlNegativeCaching() {
        assertThrows(UrlNotFoundException.class, () -> urlRedirectService.resolveAndRedirect("unknownCode", null));

        // Subsequent lookup should be caught by negative cache immediately
        assertTrue(cacheService.isNegativeCached("unknownCode"));
    }
}
