package com.distributed.urlshortener.repository;

import com.distributed.urlshortener.domain.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data JPA Repository for indexed PostgreSQL persistence.
 */
@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Look up URL mapping by B-Tree indexed shortCode.
     */
    Optional<UrlMapping> findByShortCode(String shortCode);

    /**
     * Check existence by shortCode for uniqueness verification.
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Atomically increment relational click counter in batch/sync.
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.totalClicks = u.totalClicks + 1 WHERE u.shortCode = :shortCode")
    int incrementClickCount(@Param("shortCode") String shortCode);

    /**
     * Mark expired URLs as inactive.
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.active = false WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.active = true")
    int deactivateExpiredUrls(@Param("now") Instant now);
}
