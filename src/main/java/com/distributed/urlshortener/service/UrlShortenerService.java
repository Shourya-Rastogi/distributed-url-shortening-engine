package com.distributed.urlshortener.service;

import com.distributed.urlshortener.core.Base62Encoder;
import com.distributed.urlshortener.core.DistributedIdGenerator;
import com.distributed.urlshortener.domain.dto.CreateShortUrlRequest;
import com.distributed.urlshortener.domain.dto.ShortUrlResponse;
import com.distributed.urlshortener.domain.entity.UrlMapping;
import com.distributed.urlshortener.exception.CustomAliasConflictException;
import com.distributed.urlshortener.exception.InvalidUrlException;
import com.distributed.urlshortener.exception.UrlNotFoundException;
import com.distributed.urlshortener.repository.UrlMappingRepository;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Service orchestrating URL Shortening, Distributed Base62 Code Generation,
 * Custom Alias Validation, PostgreSQL Persistence, and Redis Cache Warmup.
 */
@Service
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"}, UrlValidator.ALLOW_LOCAL_URLS);

    private final UrlMappingRepository urlMappingRepository;
    private final DistributedIdGenerator idGenerator;
    private final CacheService cacheService;
    private final String baseUrl;

    public UrlShortenerService(
            UrlMappingRepository urlMappingRepository,
            DistributedIdGenerator idGenerator,
            CacheService cacheService,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.urlMappingRepository = urlMappingRepository;
        this.idGenerator = idGenerator;
        this.cacheService = cacheService;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Shortens a long URL into a compact Base62 or custom short code.
     */
    @Transactional
    public ShortUrlResponse shortenUrl(CreateShortUrlRequest request, String clientIp) {
        String originalUrl = request.getOriginalUrl().trim();
        if (!URL_VALIDATOR.isValid(originalUrl)) {
            throw new InvalidUrlException("The provided URL '" + originalUrl + "' is invalid or malformed.");
        }

        String shortCode;
        boolean isCustom = false;

        // 1. Handle Custom Alias
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            String alias = request.getCustomAlias().trim();
            if (!Base62Encoder.isValidCustomAlias(alias)) {
                throw new InvalidUrlException("Custom alias '" + alias + "' is invalid. Must be alphanumeric with '-' or '_', length 3-32 characters.");
            }
            if (urlMappingRepository.existsByShortCode(alias) || cacheService.get(alias) != null) {
                throw new CustomAliasConflictException(alias);
            }
            shortCode = alias;
            isCustom = true;
        } else {
            // 2. Distributed Base62 Short Code Generation
            shortCode = idGenerator.nextShortCode();
        }

        // 3. Compute Expiration
        Instant expiresAt = null;
        if (request.getTtlSeconds() != null && request.getTtlSeconds() > 0) {
            expiresAt = Instant.now().plus(Duration.ofSeconds(request.getTtlSeconds()));
        } else if (request.getExpiresAt() != null) {
            expiresAt = request.getExpiresAt();
        }

        // 4. Persist to PostgreSQL with indexing
        UrlMapping mapping = new UrlMapping(
                shortCode,
                originalUrl,
                Instant.now(),
                expiresAt,
                isCustom,
                clientIp
        );
        UrlMapping saved = urlMappingRepository.save(mapping);
        log.info("Persisted URL mapping: shortCode='{}' -> '{}' (isCustom={})", shortCode, originalUrl, isCustom);

        // 5. Warm up Redis Cache
        cacheService.put(shortCode, originalUrl, expiresAt, true);

        return toResponse(saved);
    }

    /**
     * Retrieves URL Mapping metadata.
     */
    @Transactional(readOnly = true)
    public ShortUrlResponse getUrlDetails(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return toResponse(mapping);
    }

    private ShortUrlResponse toResponse(UrlMapping mapping) {
        String fullShortUrl = baseUrl + "/" + mapping.getShortCode();
        return new ShortUrlResponse(
                mapping.getShortCode(),
                fullShortUrl,
                mapping.getOriginalUrl(),
                mapping.getCreatedAt(),
                mapping.getExpiresAt(),
                mapping.isCustomAlias(),
                mapping.getTotalClicks()
        );
    }
}
