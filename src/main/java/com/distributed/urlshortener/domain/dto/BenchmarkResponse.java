package com.distributed.urlshortener.domain.dto;

import java.util.Map;

/**
 * Load-Testing & Redirect Benchmark Execution Report DTO.
 */
public class BenchmarkResponse {

    private int totalRequests;
    private int concurrency;
    private long totalDurationMillis;
    private double throughputRps;
    private double minLatencyMs;
    private double meanLatencyMs;
    private double p50LatencyMs;
    private double p90LatencyMs;
    private double p95LatencyMs;
    private double p99LatencyMs;
    private double maxLatencyMs;
    private long successfulRedirects;
    private long failedRequests;
    private double errorRatePercentage;
    private double cacheHitRatioPercentage;
    private Map<Integer, Long> statusCodeDistribution;

    public BenchmarkResponse() {
    }

    // Getters and Setters
    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public long getTotalDurationMillis() {
        return totalDurationMillis;
    }

    public void setTotalDurationMillis(long totalDurationMillis) {
        this.totalDurationMillis = totalDurationMillis;
    }

    public double getThroughputRps() {
        return throughputRps;
    }

    public void setThroughputRps(double throughputRps) {
        this.throughputRps = throughputRps;
    }

    public double getMinLatencyMs() {
        return minLatencyMs;
    }

    public void setMinLatencyMs(double minLatencyMs) {
        this.minLatencyMs = minLatencyMs;
    }

    public double getMeanLatencyMs() {
        return meanLatencyMs;
    }

    public void setMeanLatencyMs(double meanLatencyMs) {
        this.meanLatencyMs = meanLatencyMs;
    }

    public double getP50LatencyMs() {
        return p50LatencyMs;
    }

    public void setP50LatencyMs(double p50LatencyMs) {
        this.p50LatencyMs = p50LatencyMs;
    }

    public double getP90LatencyMs() {
        return p90LatencyMs;
    }

    public void setP90LatencyMs(double p90LatencyMs) {
        this.p90LatencyMs = p90LatencyMs;
    }

    public double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(double p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public double getP99LatencyMs() {
        return p99LatencyMs;
    }

    public void setP99LatencyMs(double p99LatencyMs) {
        this.p99LatencyMs = p99LatencyMs;
    }

    public double getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public void setMaxLatencyMs(double maxLatencyMs) {
        this.maxLatencyMs = maxLatencyMs;
    }

    public long getSuccessfulRedirects() {
        return successfulRedirects;
    }

    public void setSuccessfulRedirects(long successfulRedirects) {
        this.successfulRedirects = successfulRedirects;
    }

    public long getFailedRequests() {
        return failedRequests;
    }

    public void setFailedRequests(long failedRequests) {
        this.failedRequests = failedRequests;
    }

    public double getErrorRatePercentage() {
        return errorRatePercentage;
    }

    public void setErrorRatePercentage(double errorRatePercentage) {
        this.errorRatePercentage = errorRatePercentage;
    }

    public double getCacheHitRatioPercentage() {
        return cacheHitRatioPercentage;
    }

    public void setCacheHitRatioPercentage(double cacheHitRatioPercentage) {
        this.cacheHitRatioPercentage = cacheHitRatioPercentage;
    }

    public Map<Integer, Long> getStatusCodeDistribution() {
        return statusCodeDistribution;
    }

    public void setStatusCodeDistribution(Map<Integer, Long> statusCodeDistribution) {
        this.statusCodeDistribution = statusCodeDistribution;
    }
}
