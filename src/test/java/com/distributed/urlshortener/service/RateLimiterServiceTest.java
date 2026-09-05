package com.distributed.urlshortener.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    @Test
    @DisplayName("Rate limiter should allow requests within threshold and block exceeding requests with HTTP 429 semantics")
    void testRateLimitingThreshold() {
        // Configure with a low threshold for deterministic testing: 5 write rpm, 10 read rpm
        RateLimiterService rateLimiter = new RateLimiterService(null, 5, 10);
        String clientIp = "198.51.100.42";

        // First 5 requests should succeed
        for (int i = 1; i <= 5; i++) {
            RateLimiterService.RateLimitResult result = rateLimiter.checkLimit(clientIp, "write");
            assertTrue(result.isAllowed(), "Request " + i + " should have been allowed");
            assertEquals(5 - i, result.getRemainingRequests());
        }

        // 6th request must be denied
        RateLimiterService.RateLimitResult blocked = rateLimiter.checkLimit(clientIp, "write");
        assertFalse(blocked.isAllowed(), "Request 6 should have been blocked");
        assertEquals(0, blocked.getRemainingRequests());
        assertTrue(blocked.getRetryAfterSeconds() > 0);

        // Different IP should still have its full quota available
        RateLimiterService.RateLimitResult diffIpResult = rateLimiter.checkLimit("203.0.113.1", "write");
        assertTrue(diffIpResult.isAllowed());
        assertEquals(4, diffIpResult.getRemainingRequests());
    }
}
