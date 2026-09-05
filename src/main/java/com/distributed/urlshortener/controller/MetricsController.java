package com.distributed.urlshortener.controller;

import com.distributed.urlshortener.domain.dto.BenchmarkResponse;
import com.distributed.urlshortener.domain.dto.CacheMetricsResponse;
import com.distributed.urlshortener.domain.dto.CreateShortUrlRequest;
import com.distributed.urlshortener.domain.dto.ShortUrlResponse;
import com.distributed.urlshortener.service.CacheService;
import com.distributed.urlshortener.service.UrlRedirectService;
import com.distributed.urlshortener.service.UrlShortenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller exposing Cache Metrics and Concurrency Load-Testing Benchmark APIs.
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);

    private final CacheService cacheService;
    private final UrlRedirectService urlRedirectService;
    private final UrlShortenerService urlShortenerService;

    public MetricsController(
            CacheService cacheService,
            UrlRedirectService urlRedirectService,
            UrlShortenerService urlShortenerService) {
        this.cacheService = cacheService;
        this.urlRedirectService = urlRedirectService;
        this.urlShortenerService = urlShortenerService;
    }

    /**
     * Retrieves real-time Cache Hit Ratio and lookup statistics.
     */
    @GetMapping("/cache")
    public ResponseEntity<CacheMetricsResponse> getCacheMetrics() {
        return ResponseEntity.ok(cacheService.getMetrics());
    }

    /**
     * Resets cache metrics counters.
     */
    @PostMapping("/cache/reset")
    public ResponseEntity<Map<String, String>> resetCacheMetrics() {
        cacheService.resetMetrics();
        return ResponseEntity.ok(Map.of("message", "Cache metrics counters reset successfully."));
    }

    /**
     * Executes an automated high-concurrency load test against redirect resolution path
     * and measures throughput, P50, P90, P95, P99 latency, and cache hit ratio.
     *
     * @param totalRequests total requests to send (default 10,000)
     * @param concurrency   concurrent threads (default 50)
     * @param hotKeyRatio   fraction of traffic hitting hot cached URLs (default 0.80 = 80/20 Zipfian distribution)
     */
    @PostMapping("/benchmark/run")
    public ResponseEntity<BenchmarkResponse> runLoadTest(
            @RequestParam(name = "totalRequests", defaultValue = "10000") int totalRequests,
            @RequestParam(name = "concurrency", defaultValue = "50") int concurrency,
            @RequestParam(name = "hotKeyRatio", defaultValue = "0.80") double hotKeyRatio) throws Exception {

        log.info("Starting Load-Test Benchmark: totalRequests={}, concurrency={}, hotKeyRatio={}", totalRequests, concurrency, hotKeyRatio);

        // 1. Prepare warm test URLs
        int warmCount = 20;
        List<String> hotCodes = new ArrayList<>();
        for (int i = 1; i <= warmCount; i++) {
            ShortUrlResponse resp = urlShortenerService.shortenUrl(
                    new CreateShortUrlRequest("https://example.com/target-" + i, "hot-key-" + i + "-" + UUID.randomUUID().toString().substring(0, 6), null, null),
                    "127.0.0.1"
            );
            hotCodes.add(resp.getShortCode());
        }

        // 2. Prepare cold test URLs
        int coldCount = 100;
        List<String> coldCodes = new ArrayList<>();
        for (int i = 1; i <= coldCount; i++) {
            ShortUrlResponse resp = urlShortenerService.shortenUrl(
                    new CreateShortUrlRequest("https://example.com/cold-target-" + i, "cold-key-" + i + "-" + UUID.randomUUID().toString().substring(0, 6), null, null),
                    "127.0.0.1"
            );
            coldCodes.add(resp.getShortCode());
            // Clear from cache to simulate cold read from DB
            cacheService.evict(resp.getShortCode());
        }

        // Reset cache counters prior to benchmark
        cacheService.resetMetrics();

        // 3. Multithreaded synthetic traffic execution
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        long[] latenciesNano = new long[totalRequests];
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);
        Map<Integer, Long> statusDistribution = new ConcurrentHashMap<>();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(totalRequests);

        long startWallTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize thread start for maximum burst concurrency

                    // 80/20 distribution: 80% requests hit hot cached keys, 20% hit cold/random keys
                    boolean isHot = ThreadLocalRandom.current().nextDouble() < hotKeyRatio;
                    String targetCode;
                    if (isHot) {
                        targetCode = hotCodes.get(ThreadLocalRandom.current().nextInt(hotCodes.size()));
                    } else {
                        targetCode = coldCodes.get(ThreadLocalRandom.current().nextInt(coldCodes.size()));
                    }

                    long t0 = System.nanoTime();
                    String redirectUrl = urlRedirectService.resolveAndRedirect(targetCode, null);
                    long duration = System.nanoTime() - t0;
                    latenciesNano[index] = duration;

                    if (redirectUrl != null) {
                        successCount.incrementAndGet();
                        statusDistribution.merge(302, 1L, Long::sum);
                    } else {
                        failCount.incrementAndGet();
                        statusDistribution.merge(404, 1L, Long::sum);
                    }
                } catch (Exception ex) {
                    failCount.incrementAndGet();
                    statusDistribution.merge(500, 1L, Long::sum);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Trigger concurrent execution
        startLatch.countDown();
        completeLatch.await(120, TimeUnit.SECONDS);
        executor.shutdown();

        long totalDurationMs = Math.max(1, System.currentTimeMillis() - startWallTime);

        // 4. Calculate Latency Percentiles
        Arrays.sort(latenciesNano);
        // Convert to milliseconds
        double minMs = latenciesNano[0] / 1_000_000.0;
        double maxMs = latenciesNano[latenciesNano.length - 1] / 1_000_000.0;
        double p50Ms = latenciesNano[(int) (totalRequests * 0.50)] / 1_000_000.0;
        double p90Ms = latenciesNano[(int) (totalRequests * 0.90)] / 1_000_000.0;
        double p95Ms = latenciesNano[(int) (totalRequests * 0.95)] / 1_000_000.0;
        double p99Ms = latenciesNano[(int) (totalRequests * 0.99)] / 1_000_000.0;

        long sumNano = 0;
        for (long l : latenciesNano) {
            sumNano += l;
        }
        double meanMs = (sumNano / (double) totalRequests) / 1_000_000.0;

        double rps = (totalRequests / (double) totalDurationMs) * 1000.0;
        double errorRate = ((double) failCount.get() / totalRequests) * 100.0;

        CacheMetricsResponse cacheMetrics = cacheService.getMetrics();

        BenchmarkResponse report = new BenchmarkResponse();
        report.setTotalRequests(totalRequests);
        report.setConcurrency(concurrency);
        report.setTotalDurationMillis(totalDurationMs);
        report.setThroughputRps(Math.round(rps * 100.0) / 100.0);
        report.setMinLatencyMs(Math.round(minMs * 1000.0) / 1000.0);
        report.setMeanLatencyMs(Math.round(meanMs * 1000.0) / 1000.0);
        report.setP50LatencyMs(Math.round(p50Ms * 1000.0) / 1000.0);
        report.setP90LatencyMs(Math.round(p90Ms * 1000.0) / 1000.0);
        report.setP95LatencyMs(Math.round(p95Ms * 1000.0) / 1000.0);
        report.setP99LatencyMs(Math.round(p99Ms * 1000.0) / 1000.0);
        report.setMaxLatencyMs(Math.round(maxMs * 1000.0) / 1000.0);
        report.setSuccessfulRedirects(successCount.get());
        report.setFailedRequests(failCount.get());
        report.setErrorRatePercentage(Math.round(errorRate * 100.0) / 100.0);
        report.setCacheHitRatioPercentage(cacheMetrics.getHitRatioPercentage());
        report.setStatusCodeDistribution(statusDistribution);

        log.info("Benchmark complete: Throughput={} RPS, P95={} ms, Cache Hit Ratio={}%",
                report.getThroughputRps(), report.getP95LatencyMs(), report.getCacheHitRatioPercentage());

        return ResponseEntity.ok(report);
    }
}
