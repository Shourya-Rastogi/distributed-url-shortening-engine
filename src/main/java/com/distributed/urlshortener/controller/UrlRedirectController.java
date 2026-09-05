package com.distributed.urlshortener.controller;

import com.distributed.urlshortener.exception.RateLimitExceededException;
import com.distributed.urlshortener.service.RateLimiterService;
import com.distributed.urlshortener.service.UrlRedirectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Ultra-Fast Redirect Controller.
 * Redirects client via HTTP 302 Found to original long URL while asynchronously streaming analytics.
 */
@RestController
public class UrlRedirectController {

    private final UrlRedirectService urlRedirectService;
    private final RateLimiterService rateLimiterService;

    public UrlRedirectController(
            UrlRedirectService urlRedirectService,
            RateLimiterService rateLimiterService) {
        this.urlRedirectService = urlRedirectService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * High-speed Redirect Path.
     */
    @GetMapping("/{shortCode:[a-zA-Z0-9_-]{1,64}}")
    public ResponseEntity<Void> redirect(
            @PathVariable("shortCode") String shortCode,
            HttpServletRequest request) {

        String clientIp = extractClientIp(request);

        // Apply rate limit on redirect lookups
        RateLimiterService.RateLimitResult rateResult = rateLimiterService.checkLimit(clientIp, "read");
        if (!rateResult.isAllowed()) {
            throw new RateLimitExceededException(
                    "Redirect rate limit exceeded. Please retry after " + rateResult.getRetryAfterSeconds() + " seconds.",
                    rateResult.getRetryAfterSeconds()
            );
        }

        String targetUrl = urlRedirectService.resolveAndRedirect(shortCode, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        // Cache-Control: no-cache prevents browser from bypassing our server analytics
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.set(HttpHeaders.PRAGMA, "no-cache");
        headers.set(HttpHeaders.EXPIRES, "0");

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
