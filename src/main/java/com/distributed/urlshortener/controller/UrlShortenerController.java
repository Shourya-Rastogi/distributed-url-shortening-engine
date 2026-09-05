package com.distributed.urlshortener.controller;

import com.distributed.urlshortener.domain.dto.CreateShortUrlRequest;
import com.distributed.urlshortener.domain.dto.ShortUrlResponse;
import com.distributed.urlshortener.exception.RateLimitExceededException;
import com.distributed.urlshortener.service.RateLimiterService;
import com.distributed.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for URL Shortening operations.
 */
@RestController
@RequestMapping("/api/v1/urls")
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;
    private final RateLimiterService rateLimiterService;

    public UrlShortenerController(
            UrlShortenerService urlShortenerService,
            RateLimiterService rateLimiterService) {
        this.urlShortenerService = urlShortenerService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Creates a new shortened URL with Base62 encoding or custom alias.
     */
    @PostMapping
    public ResponseEntity<ShortUrlResponse> createShortUrl(
            @Valid @RequestBody CreateShortUrlRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);

        // Apply rate limit on URL creation
        RateLimiterService.RateLimitResult rateResult = rateLimiterService.checkLimit(clientIp, "write");
        if (!rateResult.isAllowed()) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded for URL creation. Please retry after " + rateResult.getRetryAfterSeconds() + " seconds.",
                    rateResult.getRetryAfterSeconds()
            );
        }

        ShortUrlResponse response = urlShortenerService.shortenUrl(request, clientIp);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves URL metadata.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<ShortUrlResponse> getUrlDetails(@PathVariable("shortCode") String shortCode) {
        ShortUrlResponse response = urlShortenerService.getUrlDetails(shortCode);
        return ResponseEntity.ok(response);
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
