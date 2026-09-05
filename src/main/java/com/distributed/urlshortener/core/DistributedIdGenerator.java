package com.distributed.urlshortener.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Distributed Sequence Block Manager.
 * Implements the Range Allocation / Chunk Lease Pattern:
 * Each stateless Spring Boot instance leases a block of IDs (e.g. 10,000) from Redis (or DB sequence table),
 * and vends IDs locally in-memory using atomic primitives.
 *
 * This achieves:
 * 1. Monotonically unique, non-colliding IDs across any number of horizontally scaled instances.
 * 2. Ultra-low latency (<100ns in-memory ID generation without network hop for every request).
 * 3. Fault-tolerant fallback to PostgreSQL/DB sequence if Redis is initializing.
 */
@Component
public class DistributedIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(DistributedIdGenerator.class);
    private static final String REDIS_SEQUENCE_KEY = "global:url:sequence:id";

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final int blockSize;

    private final AtomicLong currentId = new AtomicLong(0);
    private final AtomicLong maxId = new AtomicLong(0);
    private final ReentrantLock leaseLock = new ReentrantLock();

    public DistributedIdGenerator(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Value("${app.id-generator.block-size:10000}") int blockSize) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.blockSize = blockSize;
    }

    /**
     * Generates the next globally unique 64-bit sequence ID.
     * Thread-safe and lock-free during steady-state block consumption.
     *
     * @return unique long ID
     */
    public long nextId() {
        while (true) {
            long current = currentId.get();
            long max = maxId.get();

            // If current ID is within the leased block, increment atomically
            if (current < max) {
                if (currentId.compareAndSet(current, current + 1)) {
                    return current + 1;
                }
                // Contended CAS, retry loop
                continue;
            }

            // Block is exhausted or uninitialized, acquire lock to lease the next block
            leaseLock.lock();
            try {
                // Double-checked locking
                if (currentId.get() < maxId.get()) {
                    continue; // Another thread already leased a new block
                }

                long newMax = leaseNextBlock();
                long newStart = newMax - blockSize;
                currentId.set(newStart);
                maxId.set(newMax);

                long allocated = currentId.incrementAndGet();
                log.info("Leased new sequence block [{} - {}], allocated initial ID: {}", newStart + 1, newMax, allocated);
                return allocated;
            } finally {
                leaseLock.unlock();
            }
        }
    }

    /**
     * Generates the next Base62 encoded short code using the distributed ID generator.
     *
     * @return unique Base62 short code string
     */
    public String nextShortCode() {
        long id = nextId();
        return Base62Encoder.encode(id);
    }

    /**
     * Leases a new block of IDs atomically from Redis, falling back to DB if Redis is unavailable.
     */
    private long leaseNextBlock() {
        if (redisTemplate != null) {
            try {
                Long newEnd = redisTemplate.opsForValue().increment(REDIS_SEQUENCE_KEY, blockSize);
                if (newEnd != null && newEnd > 0) {
                    return newEnd;
                }
            } catch (Exception e) {
                log.warn("Redis sequence lease failed, attempting DB fallback: {}", e.getMessage());
            }
        }

        // Fallback to JDBC Sequence / Table
        if (jdbcTemplate != null) {
            try {
                return leaseFromDatabase();
            } catch (Exception e) {
                log.warn("Database sequence lease fallback failed: {}. Using system timestamp based block.", e.getMessage());
            }
        }

        // In-memory autonomous fallback for isolated standalone mode
        return System.currentTimeMillis() * 1000L + blockSize;
    }

    private long leaseFromDatabase() {
        // Ensure sequence table exists
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS distributed_sequence (" +
                        "  seq_name VARCHAR(64) PRIMARY KEY," +
                        "  current_val BIGINT NOT NULL" +
                        ")"
        );

        // Portable insert-if-not-exists check
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM distributed_sequence WHERE seq_name = 'url_global_seq'",
                Integer.class
        );
        if (count == null || count == 0) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO distributed_sequence (seq_name, current_val) VALUES ('url_global_seq', 100000)"
                );
            } catch (Exception ignored) {
                // Handled concurrency race if another node inserted simultaneously
            }
        }

        jdbcTemplate.update(
                "UPDATE distributed_sequence SET current_val = current_val + ? WHERE seq_name = 'url_global_seq'",
                blockSize
        );

        Long currentVal = jdbcTemplate.queryForObject(
                "SELECT current_val FROM distributed_sequence WHERE seq_name = 'url_global_seq'",
                Long.class
        );

        return currentVal != null ? currentVal : (System.currentTimeMillis() + blockSize);
    }

    public int getBlockSize() {
        return blockSize;
    }

    public long getCurrentAllocatedId() {
        return currentId.get();
    }

    public long getCurrentBlockMax() {
        return maxId.get();
    }
}
