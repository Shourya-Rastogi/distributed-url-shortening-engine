package com.distributed.urlshortener.domain.dto;

/**
 * Real-time Cache Hit Ratio & Performance Metrics DTO.
 */
public class CacheMetricsResponse {

    private long totalLookups;
    private long cacheHits;
    private long cacheMisses;
    private double hitRatioPercentage;
    private long negativeCacheHits;
    private long memoryCachedEntries;

    public CacheMetricsResponse() {
    }

    public CacheMetricsResponse(long totalLookups, long cacheHits, long cacheMisses,
                                double hitRatioPercentage, long negativeCacheHits, long memoryCachedEntries) {
        this.totalLookups = totalLookups;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
        this.hitRatioPercentage = hitRatioPercentage;
        this.negativeCacheHits = negativeCacheHits;
        this.memoryCachedEntries = memoryCachedEntries;
    }

    public long getTotalLookups() {
        return totalLookups;
    }

    public void setTotalLookups(long totalLookups) {
        this.totalLookups = totalLookups;
    }

    public long getCacheHits() {
        return cacheHits;
    }

    public void setCacheHits(long cacheHits) {
        this.cacheHits = cacheHits;
    }

    public long getCacheMisses() {
        return cacheMisses;
    }

    public void setCacheMisses(long cacheMisses) {
        this.cacheMisses = cacheMisses;
    }

    public double getHitRatioPercentage() {
        return hitRatioPercentage;
    }

    public void setHitRatioPercentage(double hitRatioPercentage) {
        this.hitRatioPercentage = hitRatioPercentage;
    }

    public long getNegativeCacheHits() {
        return negativeCacheHits;
    }

    public void setNegativeCacheHits(long negativeCacheHits) {
        this.negativeCacheHits = negativeCacheHits;
    }

    public long getMemoryCachedEntries() {
        return memoryCachedEntries;
    }

    public void setMemoryCachedEntries(long memoryCachedEntries) {
        this.memoryCachedEntries = memoryCachedEntries;
    }
}
