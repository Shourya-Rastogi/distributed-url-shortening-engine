package com.distributed.urlshortener.controller;

import com.distributed.urlshortener.domain.document.ClickEventDocument;
import com.distributed.urlshortener.domain.dto.UrlAnalyticsResponse;
import com.distributed.urlshortener.exception.UrlNotFoundException;
import com.distributed.urlshortener.service.AnalyticsQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Click Analytics and Time-Series Metrics.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    /**
     * Retrieves aggregated analytics report for the specified shortCode.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlAnalyticsResponse> getAnalytics(@PathVariable("shortCode") String shortCode) {
        UrlAnalyticsResponse response = analyticsQueryService.getAnalytics(shortCode);
        if (response == null) {
            throw new UrlNotFoundException(shortCode);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves granular recent click events for the specified shortCode.
     */
    @GetMapping("/{shortCode}/events")
    public ResponseEntity<List<ClickEventDocument>> getRecentEvents(
            @PathVariable("shortCode") String shortCode,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        List<ClickEventDocument> events = analyticsQueryService.getRecentClicks(shortCode, Math.min(100, limit));
        return ResponseEntity.ok(events);
    }
}
