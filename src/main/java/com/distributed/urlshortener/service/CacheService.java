package com.distributed.urlshortener.service;

import com.distributed.urlshortener.domain.dto.CacheMetricsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Multi-Tier High-Performance Cache Service.
 * Implements Cache-Aside pattern, Negative Caching (preventing Cache Penetration),
 * dynamic TTL calculation, and real-time Cache Hit Ratio metric instrumentation.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final String CACHE_PREFIX = "url:cache:";
    private static final String NOT_FOUND_PREFIX = "url:cache:notfound:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration defaultTtl;
    private final Duration negativeTtl;

    // Real-time metric counters
    private final AtomicLong totalLookups = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong negativeCacheHits = new AtomicLong(0);

    // In-memory L1 cache fallback if Redis is in standalone/isolated mode
    private final Map<String, CachedUrlEntry> localCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> localNegativeCache = new ConcurrentHashMap<>();

    public CacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.cache.default-ttl:86400s}") Duration defaultTtl,
            @Value("${app.cache.negative-ttl:30s}") Duration negativeTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.defaultTtl = defaultTtl;
        this.negativeTtl = negativeTtl;
    }

    /**
     * Cached URL Metadata payload.
     */
    public static class CachedUrlEntry implements Serializable {
        private String originalUrl;
        private Instant expiresAt;
        private boolean active;

        public CachedUrlEntry() {
        }

        public CachedUrlEntry(String originalUrl, Instant expiresAt, boolean active) {
            this.originalUrl = originalUrl;
            this.expiresAt = expiresAt;
            this.active = active;
        }

        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Lookup URL in Cache (Redis -> Local fallback).
     *
     * @param shortCode short code key
     * @return CachedUrlEntry or null if miss
     */
    public CachedUrlEntry get(String shortCode) {
        totalLookups.incrementAndGet();

        // 1. Check negative cache first (to prevent Cache Penetration)
        if (isNegativeCached(shortCode)) {
            negativeCacheHits.incrementAndGet();
            cacheHits.incrementAndGet();
            return null;
        }

        // 2. Check Redis Cache
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);
                if (json != null) {
                    CachedUrlEntry entry = objectMapper.readValue(json, CachedUrlEntry.class);
                    if (entry.isExpired() || !entry.isActive()) {
                        evict(shortCode);
                        cacheMisses.incrementAndGet();
                        return null;
                    }
                    cacheHits.incrementAndGet();
                    return entry;
                }
            } catch (Exception e) {
                log.debug("Redis cache get error, checking local fallback: {}", e.getMessage());
            }
        }

        // 3. Check Local L1 Cache
        CachedUrlEntry localEntry = localCache.get(shortCode);
        if (localEntry != null) {
            if (localEntry.isExpired() || !localEntry.isActive()) {
                localCache.remove(shortCode);
                cacheMisses.incrementAndGet();
                return null;
            }
            cacheHits.incrementAndGet();
            return localEntry;
        }

        // Cache Miss
        cacheMisses.incrementAndGet();
        return null;
    }

    /**
     * Store URL in Cache with TTL.
     */
    public void put(String shortCode, String originalUrl, Instant expiresAt, boolean active) {
        CachedUrlEntry entry = new CachedUrlEntry(originalUrl, expiresAt, active);

        // Compute effective TTL
        Duration effectiveTtl = defaultTtl;
        if (expiresAt != null) {
            Duration untilExpire = Duration.between(Instant.now(), expiresAt);
            if (untilExpire.isNegative() || untilExpire.isZero()) {
                return; // Already expired, do not cache
            }
            if (untilExpire.compareTo(defaultTtl) < 0) {
                effectiveTtl = untilExpire;
            }
        }

        // Populate Local Cache
        localCache.put(shortCode, entry);

        // Populate Redis Cache
        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(entry);
                redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, json, effectiveTtl);
            } catch (Exception e) {
                log.debug("Redis cache put error: {}", e.getMessage());
            }
        }
    }

    /**
     * Negative caching to prevent Cache Penetration for nonexistent keys.
     */
    public void putNegative(String shortCode) {
        localNegativeCache.put(shortCode, Instant.now().plus(negativeTtl));
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(NOT_FOUND_PREFIX + shortCode, "1", negativeTtl);
            } catch (Exception e) {
                log.debug("Redis negative cache put error: {}", e.getMessage());
            }
        }
    }

    public boolean isNegativeCached(String shortCode) {
        Instant exp = localNegativeCache.get(shortCode);
        if (exp != null) {
            if (Instant.now().isBefore(exp)) {
                return true;
            }
            localNegativeCache.remove(shortCode);
        }

        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(NOT_FOUND_PREFIX + shortCode));
            } catch (Exception e) {
                log.debug("Redis negative cache check error: {}", e.getMessage());
            }
        }
        return false;
    }

    /**
     * Evicts key from all cache tiers.
     */
    public void evict(String shortCode) {
        localCache.remove(shortCode);
        localNegativeCache.remove(shortCode);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(CACHE_PREFIX + shortCode);
                redisTemplate.delete(NOT_FOUND_PREFIX + shortCode);
            } catch (Exception e) {
                log.debug("Redis cache evict error: {}", e.getMessage());
            }
        }
    }

    /**
     * Computes real-time Cache Hit Ratio metrics.
     */
    public CacheMetricsResponse getMetrics() {
        long lookups = totalLookups.get();
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        double ratio = lookups > 0 ? ((double) hits / lookups) * 100.0 : 0.0;

        return new CacheMetricsResponse(
                lookups,
                hits,
                misses,
                Math.round(ratio * 100.0) / 100.0,
                negativeCacheHits.get(),
                localCache.size()
        );
    }

    public void resetMetrics() {
        totalLookups.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        negativeCacheHits.set(0);
    }
}
