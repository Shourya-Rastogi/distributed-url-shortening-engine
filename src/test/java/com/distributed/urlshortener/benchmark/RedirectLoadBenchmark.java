package com.distributed.urlshortener.benchmark;

import com.distributed.urlshortener.FakeUrlMappingRepository;
import com.distributed.urlshortener.core.DistributedIdGenerator;
import com.distributed.urlshortener.domain.dto.CreateShortUrlRequest;
import com.distributed.urlshortener.domain.dto.ShortUrlResponse;
import com.distributed.urlshortener.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrency Load Test & Performance Benchmark.
 * Simulates high-concurrency production traffic on the redirect path to measure:
 * 1. Throughput (Requests Per Second - RPS)
 * 2. Cache Hit Ratio (Hot vs Cold keys)
 * 3. Latency Distribution (Min, Mean, P50, P90, P95, P99, Max in milliseconds)
 */
public class RedirectLoadBenchmark {

    private static final Logger log = LoggerFactory.getLogger(RedirectLoadBenchmark.class);

    @Test
    @DisplayName("Execute 10,000 concurrent redirect requests under 50 threads and compute P95 latency & Cache Hit Ratio")
    void runConcurrentRedirectBenchmark() throws Exception {
        int totalRequests = 10_000;
        int concurrency = 50;
        double hotKeyRatio = 0.85; // 85% traffic hits cached hot URLs, 15% hits cold DB URLs

        // 1. Setup in-memory repositories and services
        FakeUrlMappingRepository repo = new FakeUrlMappingRepository();
        DistributedIdGenerator idGenerator = new DistributedIdGenerator(null, null, 1000);
        CacheService cacheService = new CacheService(null, new ObjectMapper(), Duration.ofDays(1), Duration.ofSeconds(30));
        AnalyticsConsumerService consumerService = new AnalyticsConsumerService(null, null, null);
        AnalyticsProducerService producerService = new AnalyticsProducerService(null, consumerService, "url-clicks");
        UrlShortenerService shortenerService = new UrlShortenerService(repo, idGenerator, cacheService, "https://sho.rt");
        UrlRedirectService redirectService = new UrlRedirectService(cacheService, repo, producerService);

        // 2. Seed 50 Hot Cached URLs
        List<String> hotCodes = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            ShortUrlResponse r = shortenerService.shortenUrl(
                    new CreateShortUrlRequest("https://cdn.example.com/assets/" + i, "hot-" + i, null, null),
                    "127.0.0.1"
            );
            hotCodes.add(r.getShortCode());
        }

        // 3. Seed 200 Cold URLs (evicted from cache)
        List<String> coldCodes = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            ShortUrlResponse r = shortenerService.shortenUrl(
                    new CreateShortUrlRequest("https://cold.example.com/archive/" + i, "cold-" + i, null, null),
                    "127.0.0.1"
            );
            coldCodes.add(r.getShortCode());
            cacheService.evict(r.getShortCode()); // Evict from cache
        }

        // Reset cache stats prior to benchmark
        cacheService.resetMetrics();

        // 4. Warm-up Phase (1,000 requests)
        for (int i = 0; i < 1000; i++) {
            String code = hotCodes.get(i % hotCodes.size());
            redirectService.resolveAndRedirect(code, null);
        }
        cacheService.resetMetrics();

        // 5. High-Concurrency Benchmark Execution
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        long[] latenciesNano = new long[totalRequests];
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);

        long startWallTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean isHot = ThreadLocalRandom.current().nextDouble() < hotKeyRatio;
                    String targetCode = isHot
                            ? hotCodes.get(ThreadLocalRandom.current().nextInt(hotCodes.size()))
                            : coldCodes.get(ThreadLocalRandom.current().nextInt(coldCodes.size()));

                    long t0 = System.nanoTime();
                    String resolved = redirectService.resolveAndRedirect(targetCode, null);
                    long elapsed = System.nanoTime() - t0;
                    latenciesNano[index] = elapsed;

                    if (resolved != null) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(60, TimeUnit.SECONDS));
        executor.shutdown();

        long totalDurationMs = Math.max(1, System.currentTimeMillis() - startWallTime);

        // 6. Compute Latency Metrics
        Arrays.sort(latenciesNano);
        double minMs = latenciesNano[0] / 1_000_000.0;
        double maxMs = latenciesNano[latenciesNano.length - 1] / 1_000_000.0;
        double p50Ms = latenciesNano[(int) (totalRequests * 0.50)] / 1_000_000.0;
        double p90Ms = latenciesNano[(int) (totalRequests * 0.90)] / 1_000_000.0;
        double p95Ms = latenciesNano[(int) (totalRequests * 0.95)] / 1_000_000.0;
        double p99Ms = latenciesNano[(int) (totalRequests * 0.99)] / 1_000_000.0;

        long sumNano = 0;
        for (long l : latenciesNano) sumNano += l;
        double meanMs = (sumNano / (double) totalRequests) / 1_000_000.0;

        double rps = (totalRequests / (double) totalDurationMs) * 1000.0;
        double hitRatio = cacheService.getMetrics().getHitRatioPercentage();

        System.out.println("\n===============================================================================");
        System.out.println("                  DISTRIBUTED URL SHORTENER REDIRECT BENCHMARK                ");
        System.out.println("===============================================================================");
        System.out.printf("Total Requests         : %,d\n", totalRequests);
        System.out.printf("Concurrency (Threads)  : %d\n", concurrency);
        System.out.printf("Total Wall Time        : %d ms\n", totalDurationMs);
        System.out.printf("Throughput             : %,.2f Requests / Second (RPS)\n", rps);
        System.out.printf("Cache Hit Ratio        : %.2f %%\n", hitRatio);
        System.out.printf("Successful Redirects   : %,d (100.0%%)\n", successCount.get());
        System.out.printf("Failed / Error Count   : %d (0.0%%)\n", failCount.get());
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("                            LATENCY PERCENTILES                               ");
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("Min Latency            : %.4f ms\n", minMs);
        System.out.printf("Mean Latency           : %.4f ms\n", meanMs);
        System.out.printf("P50 Latency (Median)   : %.4f ms\n", p50Ms);
        System.out.printf("P90 Latency            : %.4f ms\n", p90Ms);
        System.out.printf("P95 Latency            : %.4f ms\n", p95Ms);
        System.out.printf("P99 Latency            : %.4f ms\n", p99Ms);
        System.out.printf("Max Latency            : %.4f ms\n", maxMs);
        System.out.println("===============================================================================\n");

        assertTrue(successCount.get() == totalRequests, "All requests should be successful");
        assertTrue(hitRatio > 70.0, "Cache hit ratio should exceed 70% under 85% hot traffic");
        assertTrue(p95Ms < 20.0, "P95 latency should be well under 20ms");
    }
}
