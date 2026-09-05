package com.distributed.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding Window & Token Bucket Rate Limiter Service.
 * Protects APIs from abuse and denial of service across distributed instances using Redis,
 * with atomic local fallback.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final int defaultWriteLimitPerMinute;
    private final int defaultReadLimitPerMinute;

    // In-memory sliding window fallback map: IP:action:windowKey -> Count
    private final Map<String, WindowCounter> localRateMap = new ConcurrentHashMap<>();

    private static class WindowCounter {
        final long windowStartSecond;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStartSecond) {
            this.windowStartSecond = windowStartSecond;
        }
    }

    public RateLimiterService(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Value("${app.ratelimit.write-rpm:100}") int defaultWriteLimitPerMinute,
            @Value("${app.ratelimit.read-rpm:1000}") int defaultReadLimitPerMinute) {
        this.redisTemplate = redisTemplate;
        this.defaultWriteLimitPerMinute = defaultWriteLimitPerMinute;
        this.defaultReadLimitPerMinute = defaultReadLimitPerMinute;
    }

    /**
     * Checks whether the request from the specified IP for the given action is allowed.
     *
     * @param clientIp client IP address
     * @param action   "write" (URL creation) or "read" (redirect/query)
     * @return RateLimitResult containing allow status, remaining tokens, and retry-after
     */
    public RateLimitResult checkLimit(String clientIp, String action) {
        String ip = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";
        int limit = "write".equalsIgnoreCase(action) ? defaultWriteLimitPerMinute : defaultReadLimitPerMinute;
        long currentMinute = System.currentTimeMillis() / 60000;
        String key = RATE_LIMIT_PREFIX + action + ":" + ip + ":" + currentMinute;

        // 1. Try Redis Distributed Rate Limiter
        if (redisTemplate != null) {
            try {
                Long currentCount = redisTemplate.opsForValue().increment(key, 1);
                if (currentCount != null) {
                    if (currentCount == 1) {
                        redisTemplate.expire(key, Duration.ofSeconds(65));
                    }
                    if (currentCount > limit) {
                        long secondsLeft = 60 - ((System.currentTimeMillis() / 1000) % 60);
                        return new RateLimitResult(false, 0, Math.max(1, secondsLeft));
                    }
                    return new RateLimitResult(true, limit - currentCount, 0);
                }
            } catch (Exception e) {
                log.debug("Redis rate limiting unavailable, falling back to in-memory: {}", e.getMessage());
            }
        }

        // 2. In-Memory Local Fallback Rate Limiter
        long currentSec = System.currentTimeMillis() / 1000;
        long windowStart = (currentSec / 60) * 60;
        String localKey = ip + ":" + action + ":" + windowStart;

        WindowCounter counter = localRateMap.compute(localKey, (k, existing) -> {
            if (existing == null || existing.windowStartSecond != windowStart) {
                return new WindowCounter(windowStart);
            }
            return existing;
        });

        int val = counter.count.incrementAndGet();
        if (val > limit) {
            long secondsLeft = 60 - (currentSec % 60);
            return new RateLimitResult(false, 0, Math.max(1, secondsLeft));
        }

        // Periodic cleanup of old local keys
        if (localRateMap.size() > 5000) {
            localRateMap.entrySet().removeIf(entry -> entry.getValue().windowStartSecond < (currentSec - 120));
        }

        return new RateLimitResult(true, limit - val, 0);
    }

    public static class RateLimitResult {
        private final boolean allowed;
        private final long remainingRequests;
        private final long retryAfterSeconds;

        public RateLimitResult(boolean allowed, long remainingRequests, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remainingRequests = remainingRequests;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRemainingRequests() {
            return remainingRequests;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
