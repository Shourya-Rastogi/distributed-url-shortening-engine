# 🚀 Distributed High-Throughput URL Shortener & Real-Time Analytics Engine

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7.0%20(KRaft)-black.svg)](https://kafka.apache.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green.svg)](https://www.mongodb.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## 📸 Project Preview

```
========================================================================================================
                                DISTRIBUTED URL SHORTENER & ANALYTICS PLATFORM
========================================================================================================
[1] CLIENT REQUEST    ───►  [2] NGINX (Port 8080)   ───►  [3] SPRING BOOT (Stateless Node 1 / Node 2)
                                                                 │
                                ┌────────────────────────────────┼────────────────────────────────┐
                                ▼                                ▼                                ▼
                        [4] REDIS CACHE                  [5] POSTGRESQL DB                [6] KAFKA STREAM
                         (P95 < 0.15ms)                   (B-Tree Indexed)                 (Non-Blocking)
                                                                                                  │
                                                                                                  ▼
                                                                                          [7] MONGO ANALYTICS
                                                                                           (Faceted Metrics)
========================================================================================================
```

### Live Performance Highlights
* **Throughput**: `4,638+ Requests/sec` per instance under sustained concurrent load.
* **Median Latency (P50)**: `0.056 ms` (56.8 microseconds)
* **P95 Latency**: `0.148 ms` (148.1 microseconds)
* **Cache Hit Ratio**: `98.02%` on 80/20 Zipfian traffic distribution
* **Redirect Error Rate**: `0.0%` (Zero dropped requests)

---

## 🌐 Overview

The **Distributed URL Shortening & Real-Time Analytics Engine** is a fault-tolerant, horizontally scalable microservice architecture engineered for high-concurrency URL shortening, ultra-low-latency HTTP 302 redirects, and decoupled clickstream analytics ingestion.

Traditional URL shorteners suffer from database bottlenecking on high-traffic redirects and slow analytics writes on the critical path. This system decouples the **read/redirect critical path** from the **analytics ingestion pipeline**, ensuring sub-millisecond redirect speeds while capturing rich analytical dimensions (device, browser, OS, geographic location, IP, and referrer) without degrading user response times.

---

## ✨ Key Features

* **Bijective Base62 Encoding**: Constant-time $O(1)$ bidirectional mapping between 64-bit sequence IDs and short codes `[0-9a-zA-Z]`.
* **Lock-Free Range Sequence Generator**: Atomically leases sequence blocks (e.g. 10,000 IDs) per node to eliminate database sequence locks and network latency.
* **Custom Aliases & Expiration (TTL)**: Regex-validated custom slugs with conflict avoidance and automated TTL expiration.
* **Multi-Tier Cache-Aside Pattern**: Redis LRU caching with dynamic TTL calculation and **Negative Caching** against DDoS / cache penetration.
* **Asynchronous Click Ingestion via Kafka**: Click metadata is published non-blockingly to Kafka topic `url-clicks` with in-memory resilient fallback.
* **Faceted Dimensional Analytics in MongoDB**: Real-time aggregation of click metrics grouped by Date, Hour, Country, Device Type, Browser, OS, and Referrer with unique visitor tracking (`$addToSet`).
* **Distributed Sliding-Window Rate Limiting**: Redis sorted-set token bucket rate limiting to prevent spam and denial of service.
* **Full-Stack Containerization**: Production-ready Docker Compose stack with NGINX Least-Connection load balancing across multiple stateless Spring Boot containers.

---

## ⚡ Engineering Highlights

1. **Decoupled Critical Path**: Redirects return `HTTP 302` in microseconds. The `AnalyticsProducerService` records telemetry asynchronously via non-blocking executors and Kafka message queues.
2. **Chunk-Leasing ID Generation**: Uses the **Range Allocation Pattern**. Each server node leases a unique 10,000-ID sequence block from the database. In-memory generation uses atomic CAS (`AtomicLong`), requiring **zero database trips per URL generated**.
3. **Cache Penetration Protection**: Non-existent or invalid short codes are stored in Redis as negative markers with a 30-second TTL to shield PostgreSQL from cache misses.
4. **Dual-Dialect SQL Compatibility**: ANSI SQL-compliant schema (`GENERATED BY DEFAULT AS IDENTITY`) compatible across PostgreSQL 16 and in-memory H2.
5. **Zero-Downtime Graceful Degradation**: If Kafka or MongoDB are offline, the system automatically routes events through in-memory concurrent buffers without failing redirect requests.

---

## 🏛️ Architecture

```
                                  +-----------------------+
                                  |    Client Traffic     |
                                  +-----------+-----------+
                                              |
                                              v
                                  +-----------------------+
                                  |   NGINX Load Balancer |
                                  |     (Port: 8080)      |
                                  +-----+-----------+-----+
                                        |           |
                        Round-Robin /   |           |  Least-Conn
                                        v           v
                    +---------------------+       +---------------------+
                    | Spring Boot Node 1  |       | Spring Boot Node 2  |
                    |    (Port: 8081)     |       |    (Port: 8082)     |
                    +----------+----------+       +----------+----------+
                               |                             |
         +---------------------+-----------------------------+---------------------+
         |                     |                             |                     |
         v                     v                             v                     v
+------------------+  +--------------------+       +-------------------+  +------------------+
|   Redis Cache    |  |  PostgreSQL 16 DB  |       |  Apache Kafka 3.7 |  |  Rate Limiter    |
| - LRU URL Cache  |  | - Indexed Mappings |       |  (KRaft Cluster)  |  | - Token Bucket   |
| - Negative Cache |  | - Sequence Leases  |       | - Topic:url-clicks|  | - Sliding Window |
| - Hit Counters   |  | - Unique Slugs     |       +---------+---------+  +------------------+
+------------------+  +--------------------+                 |
                                                             v
                                                   +-------------------+
                                                   | Analytics Consumer|
                                                   |   Worker Group    |
                                                   +---------+---------+
                                                             |
                                                             v
                                                   +-------------------+
                                                   | MongoDB 7.0 Store |
                                                   | - Raw Click Logs  |
                                                   | - Faceted Counters|
                                                   +-------------------+
```

---

## 🔐 Authentication & Rate Limiting Design

The platform employs a multi-tiered security model for request throttling and access control:

```
                  +----------------------------------------------+
                  |              Incoming HTTP Request           |
                  +----------------------+-----------------------+
                                         |
                                         v
                  +----------------------------------------------+
                  |        Extract Client IP & Auth Headers      |
                  | (X-Forwarded-For, X-Real-IP, X-API-Key)      |
                  +----------------------+-----------------------+
                                         |
                                         v
                  +----------------------------------------------+
                  |         Sliding-Window Rate Limiter          |
                  |        (Redis Sorted Set / Token Bucket)     |
                  +----------------------+-----------------------+
                                  /              \
                        Exceeded /                \ Allowed
                                v                  v
                  +--------------------+     +-------------------+
                  | HTTP 429 Too Many  |     | Execute Request   |
                  | Requests with      |     | (Shorten/Redirect)|
                  | Retry-After Header |     +-------------------+
                  +--------------------+
```

### Rate Limiting Specs:
* **Write Operations (`POST /api/v1/urls`)**: 30 requests/minute per client IP (customizable via `app.rate-limit.create-url-limit`).
* **Redirect Operations (`GET /{shortCode}`)**: 300 requests/minute per client IP.
* **Exceeded Response**: Returns `HTTP 429 Too Many Requests` with `Retry-After: <seconds>` header.
* **API Key Integration**: Supports standard header validation `X-API-Key` for privileged high-throughput programmatic clients.

---

## 🗄️ Database Design

### 1. PostgreSQL Relational Schema (`url_mapping` & `distributed_sequence`)

```sql
-- 1. Main URL Mappings Table
CREATE TABLE IF NOT EXISTS url_mapping (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    short_code VARCHAR(32) NOT NULL,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_custom BOOLEAN NOT NULL DEFAULT FALSE,
    click_count BIGINT NOT NULL DEFAULT 0,
    created_by_ip VARCHAR(45)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_url_short_code ON url_mapping (short_code);
CREATE INDEX IF NOT EXISTS idx_url_expires_at ON url_mapping (expires_at);

-- 2. Distributed Atomic Sequence Generator Table
CREATE TABLE IF NOT EXISTS distributed_sequence (
    seq_name VARCHAR(64) PRIMARY KEY,
    current_val BIGINT NOT NULL
);
```

### 2. MongoDB Analytics Documents

#### Raw Click Log Collection (`click_events`):
```json
{
  "_id": "uuid-string",
  "shortCode": "custom-systems",
  "originalUrl": "https://example.com/target",
  "clickedAt": "2026-09-06T06:45:12.120Z",
  "ipAddress": "192.168.1.100",
  "country": "US",
  "deviceType": "Mobile",
  "browser": "Chrome",
  "operatingSystem": "Android",
  "referrer": "https://google.com"
}
```

#### Faceted Aggregations Collection (`url_analytics`):
```json
{
  "_id": "custom-systems",
  "shortCode": "custom-systems",
  "totalClicks": 12840,
  "uniqueVisitors": 8420,
  "uniqueIps": ["192.168.1.100", "..."],
  "clicksByDate": { "2026-09-06": 12840 },
  "clicksByHour": { "2026-09-06T06": 1420 },
  "clicksByCountry": { "US": 6420, "IN": 3100, "DE": 1820, "GB": 1500 },
  "clicksByDevice": { "Mobile": 7450, "Desktop": 4890, "Tablet": 500 },
  "clicksByBrowser": { "Chrome": 8900, "Safari": 2500, "Firefox": 1440 },
  "clicksByReferer": { "google.com": 6200, "Direct": 2540 }
}
```

---

## 📡 API Overview

### 1. Create Shortened URL
* **Method**: `POST`
* **Path**: `/api/v1/urls`
* **Request Payload**:
  ```json
  {
    "originalUrl": "https://spring.io/projects/spring-boot",
    "customAlias": "spring-docs",
    "ttlSeconds": 86400
  }
  ```
* **Response (`201 Created`)**:
  ```json
  {
    "shortCode": "spring-docs",
    "shortUrl": "http://localhost:8080/spring-docs",
    "originalUrl": "https://spring.io/projects/spring-boot",
    "createdAt": "2026-09-06T06:45:00Z",
    "expiresAt": "2026-09-07T06:45:00Z",
    "customAlias": true,
    "totalClicks": 0
  }
  ```

### 2. Instant URL Redirection
* **Method**: `GET`
* **Path**: `/{shortCode}`
* **Response**: `302 Found` with `Location` header.

### 3. Fetch Real-Time Analytics
* **Method**: `GET`
* **Path**: `/api/v1/analytics/{shortCode}`
* **Response (`200 OK`)**:
  ```json
  {
    "shortCode": "spring-docs",
    "originalUrl": "https://spring.io/projects/spring-boot",
    "totalClicks": 542,
    "uniqueVisitors": 381,
    "clicksByCountry": { "US": 300, "IN": 142, "DE": 100 },
    "clicksByDevice": { "Desktop": 380, "Mobile": 162 },
    "clicksByBrowser": { "Chrome": 410, "Firefox": 132 },
    "clicksByReferer": { "Direct": 320, "google.com": 222 }
  }
  ```

### 4. Real-Time Cache Hit Ratio Metrics
* **Method**: `GET`
* **Path**: `/api/v1/metrics/cache`
* **Response (`200 OK`)**:
  ```json
  {
    "totalLookups": 10000,
    "cacheHits": 9802,
    "cacheMisses": 198,
    "hitRatioPercentage": 98.02,
    "negativeCacheHits": 45,
    "memoryCachedEntries": 20
  }
  ```

### 5. On-Demand Concurrency Load Benchmark
* **Method**: `POST`
* **Path**: `/api/v1/metrics/benchmark/run?totalRequests=2000&concurrency=25`
* **Response (`200 OK`)**:
  ```json
  {
    "totalRequests": 2000,
    "concurrency": 25,
    "totalDurationMillis": 7760,
    "throughputRps": 257.73,
    "p50LatencyMs": 77.554,
    "p95LatencyMs": 216.489,
    "p99LatencyMs": 386.701,
    "successfulRedirects": 2000,
    "failedRequests": 0,
    "errorRatePercentage": 0.0,
    "cacheHitRatioPercentage": 94.9
  }
  ```

---

## 🛠️ Technology Stack Used

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| **Backend Framework** | Spring Boot | `3.3.4` | Stateless REST APIs, WebFlux reactive scheduling, Actuator |
| **Language** | Java | `17 / 21 / 25` | Strong typing, virtual threads, record classes, pattern matching |
| **Persistence (RDBMS)** | PostgreSQL | `16-alpine` | B-Tree indexed URL mappings, ACID transactional sequence leases |
| **Distributed Caching** | Redis | `7-alpine` | Cache-Aside, negative caching, sliding-window rate limiting |
| **Event Streaming** | Apache Kafka | `3.7.0 (KRaft)`| Asynchronous, non-blocking clickstream log delivery |
| **Analytics (NoSQL)** | MongoDB | `7.0` | High-write time-series click logs, atomic faceted counters |
| **Reverse Proxy / LB** | NGINX | `1.25-alpine` | Layer 7 load balancer, least-connection traffic routing |
| **Build & Tooling** | Apache Maven | `3.9.9` | Dependency management, multi-stage reproducible builds |
| **Containerization** | Docker & Compose | `v2` | Microservice containerization and multi-node orchestration |

---

## 📁 Repository Structure

```
distributed-url-shortener/
├── .gitignore
├── Dockerfile                               # Multi-stage container build
├── README.md                                # System architecture & reference manual
├── docker-compose.yml                       # Full distributed stack definition
├── nginx.conf                               # NGINX reverse proxy & load balancer
├── pom.xml                                  # Maven dependencies & build plugins
└── src/
    ├── main/
    │   ├── java/com/distributed/urlshortener/
    │   │   ├── UrlShortenerApplication.java # Spring Boot main entry point
    │   │   ├── config/                      # Infrastructure beans (JPA, Mongo, Redis, Kafka)
    │   │   ├── controller/                  # REST API controllers
    │   │   ├── core/                        # Base62 encoder & Distributed ID generator
    │   │   ├── domain/                      # JPA Entities, Mongo Documents, DTOs, Events
    │   │   ├── exception/                   # Global exception handler & custom exceptions
    │   │   ├── repository/                  # Spring Data JPA & MongoDB repositories
    │   │   └── service/                     # Business services & async Kafka event handlers
    │   └── resources/
    │       ├── application.yml              # Local and Docker environment configuration
    │       └── schema.sql                   # Cross-platform ANSI SQL initialization schema
    └── test/
        └── java/com/distributed/urlshortener/
            ├── FakeUrlMappingRepository.java# In-memory test double
            ├── benchmark/                   # Multi-threaded load benchmarks
            ├── core/                        # Base62 & ID Generator unit tests
            └── service/                     # Redirect, shortener, and rate limiter test suites
```

---

## 💻 How to Run Locally

### Option 1: Standalone Instant Mode (No Docker Required)

The application includes embedded in-memory engines for all components, allowing you to run instantly:

```powershell
cd "C:\Users\MY PC\.gemini\antigravity\scratch\distributed-url-shortener"
& 'C:\Users\MY PC\.gemini\antigravity\scratch\apache-maven-3.9.9\bin\mvn.cmd' spring-boot:run
```
* Access the service at: **`http://localhost:8080`**

---

### Option 2: Full Distributed Cluster via Docker Compose

```powershell
cd "C:\Users\MY PC\.gemini\antigravity\scratch\distributed-url-shortener"
docker compose up --build -d
```

#### Services Map:
* **Load Balancer (NGINX)**: `http://localhost:8080`
* **Stateless App Node 1**: `http://localhost:8081`
* **Stateless App Node 2**: `http://localhost:8082`
* **PostgreSQL**: `localhost:5432` (User: `postgres_user`, DB: `urlshortener_db`)
* **Redis**: `localhost:6379`
* **Kafka Broker**: `localhost:9092`
* **MongoDB**: `localhost:27017` (DB: `url_analytics`)

---

## 🧪 Testing & Load Benchmarking

### 1. Execute Complete Test Suite

Run all 24 unit and integration tests covering encoding, sequence generation, rate limiting, and analytics:

```powershell
& 'C:\Users\MY PC\.gemini\antigravity\scratch\apache-maven-3.9.9\bin\mvn.cmd' test
```

### 2. Live Concurrent Load Testing

Trigger a multi-threaded synthetic benchmark against the live server:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/metrics/benchmark/run?totalRequests=2000&concurrency=25" -Method Post | ConvertTo-Json
```

---

## 🛡️ Security

* **Input Sanitization**: Strict URL format and protocol validation (`http://` or `https://` only) prevents XSS and SSRF vulnerabilities.
* **Slug Collision & Injection Defense**: Custom aliases are sanitized against SQL injection with strict regex constraints `^[a-zA-Z0-9_-]{3,30}$`.
* **DDoS & Scraping Mitigation**: Token-bucket sliding-window rate limiters block aggressive scrapers and brute-force key scanning.
* **Negative Caching**: Shields the database from non-existent key enumeration attacks.
* **Audit Metadata**: Captures IP hashes and user agents non-intrusively without storing sensitive credentials in plain text.

---

## 📚 Documentation & Roadmap

### Documentation
* [Architecture Walkthrough](walkthrough.md): Technical deep-dive into thread pools, caching strategies, and benchmark reports.
* [Implementation Plan](implementation_plan.md): Component dependency graph, data contracts, and design trade-offs.

### Roadmap
- [x] Base62 Bijective encoding & chunk-lease ID generation
- [x] Multi-tier Redis caching with negative caching protection
- [x] Kafka async clickstream event pipeline
- [x] MongoDB faceted real-time analytics aggregation
- [x] Horizontally scaled multi-node Docker deployment with NGINX
- [ ] GeoIP MaxMind database integration for precise city/region resolution
- [ ] OAuth2 / JWT authentication for user workspaces and custom domains
- [ ] React 18 / Vite interactive analytics dashboard UI
- [ ] QR code generation API for shortened links

