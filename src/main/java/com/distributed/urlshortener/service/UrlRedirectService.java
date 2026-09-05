package com.distributed.urlshortener.service;

import com.distributed.urlshortener.domain.entity.UrlMapping;
import com.distributed.urlshortener.exception.UrlExpiredException;
import com.distributed.urlshortener.exception.UrlNotFoundException;
import com.distributed.urlshortener.repository.UrlMappingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Ultra-Fast Stateless Redirect Engine.
 * Path:
 * 1. Redis Cache Lookup (Sub-millisecond P95).
 * 2. Fallback to PostgreSQL indexed lookup on cache miss.
 * 3. Expiration validation (HTTP 410 Gone).
 * 4. Negative caching for nonexistent keys (prevents cache penetration).
 * 5. Asynchronous Kafka event dispatch for decoupled analytics.
 */
@Service
public class UrlRedirectService {

    private static final Logger log = LoggerFactory.getLogger(UrlRedirectService.class);

    private final CacheService cacheService;
    private final UrlMappingRepository urlMappingRepository;
    private final AnalyticsProducerService analyticsProducerService;

    public UrlRedirectService(
            CacheService cacheService,
            UrlMappingRepository urlMappingRepository,
            AnalyticsProducerService analyticsProducerService) {
        this.cacheService = cacheService;
        this.urlMappingRepository = urlMappingRepository;
        this.analyticsProducerService = analyticsProducerService;
    }

    /**
     * Resolves shortCode to destination original URL and triggers decoupled click analytics.
     *
     * @param shortCode the short code token
     * @param request   HTTP servlet request for client metadata extraction
     * @return destination original URL
     */
    public String resolveAndRedirect(String shortCode, HttpServletRequest request) {
        String originalUrl;

        // 1. Check Multi-Tier Redis Cache (Fast Path)
        CacheService.CachedUrlEntry cached = cacheService.get(shortCode);
        if (cached != null) {
            if (cached.isExpired() || !cached.isActive()) {
                cacheService.evict(shortCode);
                throw new UrlExpiredException(shortCode);
            }
            originalUrl = cached.getOriginalUrl();
        } else {
            // 2. Cache Miss: Fallback to PostgreSQL Indexed Lookup
            if (cacheService.isNegativeCached(shortCode)) {
                throw new UrlNotFoundException(shortCode);
            }

            UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode).orElse(null);
            if (mapping == null) {
                // Negative Caching to protect against cache penetration attacks
                cacheService.putNegative(shortCode);
                throw new UrlNotFoundException(shortCode);
            }

            // Expiration validation
            if (mapping.isExpired() || !mapping.isActive()) {
                throw new UrlExpiredException(shortCode);
            }

            originalUrl = mapping.getOriginalUrl();

            // Populate Redis Cache for subsequent hot requests
            cacheService.put(shortCode, originalUrl, mapping.getExpiresAt(), mapping.isActive());
        }

        // 3. Asynchronously record click event via Kafka (Zero Impact on Redirect Response Time)
        analyticsProducerService.recordClickAsync(shortCode, originalUrl, request);

        return originalUrl;
    }
}
