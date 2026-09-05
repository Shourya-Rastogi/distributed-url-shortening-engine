package com.distributed.urlshortener.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class DistributedIdGeneratorTest {

    @Test
    @DisplayName("50 concurrent threads generating 5,000 IDs must produce 100% unique sequence IDs without collisions")
    void testConcurrentIdGenerationUniqueness() throws Exception {
        DistributedIdGenerator generator = new DistributedIdGenerator(null, null, 1000);

        int totalThreads = 50;
        int idsPerThread = 100;
        int expectedTotal = totalThreads * idsPerThread;

        Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalThreads);

        for (int t = 0; t < totalThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = generator.nextId();
                        generatedIds.add(id);
                    }
                } catch (Exception e) {
                    fail("Thread failed: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(expectedTotal, generatedIds.size(), "Collision detected! Number of unique IDs does not match total requests.");
    }
}
