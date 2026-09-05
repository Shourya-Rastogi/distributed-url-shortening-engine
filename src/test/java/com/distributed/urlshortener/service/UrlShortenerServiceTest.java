package com.distributed.urlshortener.service;

import com.distributed.urlshortener.FakeUrlMappingRepository;
import com.distributed.urlshortener.core.DistributedIdGenerator;
import com.distributed.urlshortener.domain.dto.CreateShortUrlRequest;
import com.distributed.urlshortener.domain.dto.ShortUrlResponse;
import com.distributed.urlshortener.domain.entity.UrlMapping;
import com.distributed.urlshortener.exception.CustomAliasConflictException;
import com.distributed.urlshortener.exception.InvalidUrlException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerServiceTest {

    private FakeUrlMappingRepository urlMappingRepository;
    private DistributedIdGenerator idGenerator;
    private CacheService cacheService;
    private UrlShortenerService urlShortenerService;

    @BeforeEach
    void setUp() {
        urlMappingRepository = new FakeUrlMappingRepository();
        idGenerator = new DistributedIdGenerator(null, null, 1000);
        cacheService = new CacheService(null, new ObjectMapper(), Duration.ofDays(1), Duration.ofSeconds(30));
        urlShortenerService = new UrlShortenerService(urlMappingRepository, idGenerator, cacheService, "https://sho.rt");
    }

    @Test
    @DisplayName("Shorten valid URL should generate Base62 short code and persist mapping")
    void testShortenValidUrl() {
        CreateShortUrlRequest request = new CreateShortUrlRequest("https://www.google.com/search?q=distributed+systems", null, 3600L, null);
        ShortUrlResponse response = urlShortenerService.shortenUrl(request, "192.168.1.10");

        assertNotNull(response);
        assertNotNull(response.getShortCode());
        assertFalse(response.getShortCode().isEmpty());
        assertTrue(response.getShortUrl().startsWith("https://sho.rt/"));
        assertEquals("https://www.google.com/search?q=distributed+systems", response.getOriginalUrl());
        assertFalse(response.isCustomAlias());
        assertNotNull(response.getExpiresAt());

        // Verify stored in repository
        assertTrue(urlMappingRepository.existsByShortCode(response.getShortCode()));
    }

    @Test
    @DisplayName("Shorten with custom alias should validate and reserve custom short code")
    void testShortenWithCustomAlias() {
        CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com/promo", "custom-deal-2026", null, null);
        ShortUrlResponse response = urlShortenerService.shortenUrl(request, "127.0.0.1");

        assertNotNull(response);
        assertEquals("custom-deal-2026", response.getShortCode());
        assertEquals("https://sho.rt/custom-deal-2026", response.getShortUrl());
        assertTrue(response.isCustomAlias());
        assertTrue(urlMappingRepository.existsByShortCode("custom-deal-2026"));
    }

    @Test
    @DisplayName("Shorten with conflicting custom alias should throw CustomAliasConflictException")
    void testShortenWithDuplicateCustomAlias() {
        urlMappingRepository.save(new UrlMapping("taken-alias", "https://existing.com", Instant.now(), null, true, "127.0.0.1"));

        CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com/item", "taken-alias", null, null);
        assertThrows(CustomAliasConflictException.class, () -> urlShortenerService.shortenUrl(request, "127.0.0.1"));
    }

    @Test
    @DisplayName("Shorten with invalid URL format should throw InvalidUrlException")
    void testShortenWithInvalidUrl() {
        CreateShortUrlRequest request = new CreateShortUrlRequest("htp://not-a-valid-url", null, null, null);
        assertThrows(InvalidUrlException.class, () -> urlShortenerService.shortenUrl(request, "127.0.0.1"));
    }
}
