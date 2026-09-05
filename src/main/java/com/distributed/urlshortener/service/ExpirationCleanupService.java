package com.distributed.urlshortener.service;

import com.distributed.urlshortener.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Background Service for Expired URL Lifecycle Cleanup.
 */
@Service
public class ExpirationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ExpirationCleanupService.class);

    private final UrlMappingRepository urlMappingRepository;

    public ExpirationCleanupService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    /**
     * Periodically deactivates expired URLs in PostgreSQL database.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${app.cleanup.interval-ms:300000}")
    @Transactional
    public void cleanupExpiredUrls() {
        try {
            int updated = urlMappingRepository.deactivateExpiredUrls(Instant.now());
            if (updated > 0) {
                log.info("Expiration cleanup job deactivated {} expired URLs.", updated);
            }
        } catch (Exception e) {
            log.warn("Expiration cleanup job encountered an error: {}", e.getMessage());
        }
    }
}
