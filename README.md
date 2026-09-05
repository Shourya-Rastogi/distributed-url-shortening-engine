# Distributed High-Throughput URL Shortening & Real-Time Analytics Engine

A production-grade, distributed, horizontally scalable URL shortening and analytics platform engineered with **Spring Boot 3**, **Base62 Bijective Encoding**, **PostgreSQL Indexing**, **Redis Distributed Multi-Tier Caching**, **Kafka Event Streaming**, and **MongoDB Real-Time Faceted Aggregations**.

---

## 🏛️ System Architecture

```
                                  [ Client Requests ]
                                           │
                                           ▼
                                 [ NGINX Load Balancer ]
                                (Least-Connection Routing)
                                  /                    \
                                 ▼                      ▼
                     [ Spring Boot Node 1 ]   [ Spring Boot Node 2 ]
                        (Stateless API)          (Stateless API)
                             │                        │
       ┌─────────────────────┼────────────────────────┼─────────────────────┐
       │                     │                        │                     │
       ▼                     ▼                        ▼                     ▼
[ Redis Cluster ]    [ PostgreSQL 16 ]        [ Apache Kafka ]     [ Token Bucket / ]
- LRU Cache-Aside    - B-Tree Indexes         - Topic: `url-clicks` [ Sliding Window ]
- Negative Caching   - Partitionable          - Snappy Compressed   [ Rate Limiter   ]
- Distributed Range  - Persistent Mappings    - Decoupled Pipeline
                                                      │
                                                      ▼
                                            [ Analytics Consumer ]
                                            (Faceted Aggregations)
                                                      │
                                                      ▼
                                              [ MongoDB Cluster ]
                                              - Time-Series Clicks
                                              - Real-Time Counters
```

---

## 🚀 Key Architectural Highlights

### 1. Base62 Bijective Encoding & Distributed ID Generation
- **Bijective Base62**: Encodes 64-bit monotonically increasing unique sequence numbers into compact URL-safe codes using `[0-9a-zA-Z]`. $O(1)$ constant time complexity with zero lookup collisions.
- **Range Allocation / Chunk Lease Pattern**: Each stateless application node atomically leases a chunk of 10,000 IDs from the database/Redis sequence. In-memory ID generation uses lock-free atomic CAS operations with **zero per-request network roundtrips**.
- **Custom Aliases**: Supports user-defined custom aliases with regex validation (`^[a-zA-Z0-9_-]{3,30}$`) and conflict detection.

### 2. Multi-Tier Cache-Aside & Attack Protection
- **Multi-Tier Caching**: Uses Redis LRU caching with dynamic TTL calculation based on link expiration timestamps.
- **Negative Caching**: Mitigates Cache Penetration and DDoS attacks by caching non-existent short codes with a short TTL (e.g. 30s) to guard the relational database.
- **Real-Time Hit Ratio Tracking**: Thread-safe atomic counters compute real-time Cache Hit Ratio metrics exposed via dedicated management endpoints.

### 3. Decoupled Click Analytics via Kafka & MongoDB
- **Zero Redirect Overhead**: HTTP 302 redirects resolve immediately in microseconds without waiting for database writes.
- **Asynchronous Event Publishing**: Click events (Client IP, User-Agent, Country, City, Device, Browser, OS, Referrer, Timestamp) are published to Kafka topic `url-clicks` using non-blocking asynchronous `CompletableFuture` callbacks with fallback buffers.
- **MongoDB Faceted Aggregation**: Kafka consumer workers ingest events and atomically upsert real-time dimensional counters (`date`, `hour`, `country`, `device`, `browser`, `os`, `referrer`, and `unique_ips`) using MongoDB `$inc` and `$addToSet` operators.

### 4. Distributed Rate Limiting & TTL Expiration
- **Sliding-Window Rate Limiting**: Enforces rate limiting (e.g., 60 requests/minute per client IP) using Redis sorted sets with in-memory token-bucket fallback.
- **Automatic Expiration Handling**: Expired links immediately return `HTTP 410 Gone`. A scheduled background job purges expired links in database batches.

---

## 📊 Performance & Concurrency Benchmark Results

Simulated high-concurrency production workload on the redirect path (**10,000 requests** under **50 concurrent worker threads** with 85/15 hot/cold distribution):

```
===============================================================================
                  DISTRIBUTED URL SHORTENER REDIRECT BENCHMARK                
===============================================================================
Total Requests         : 10,000
Concurrency (Threads)  : 50
Total Wall Time        : 2,156 ms
Throughput             : 4,638.22 Requests / Second (RPS)
Cache Hit Ratio        : 98.02 %
Successful Redirects   : 10,000 (100.0%)
Failed / Error Count   : 0 (0.0%)
-------------------------------------------------------------------------------
                            LATENCY PERCENTILES                               
-------------------------------------------------------------------------------
Min Latency            : 0.0360 ms
Mean Latency           : 5.7157 ms
P50 Latency (Median)   : 0.0568 ms   (56.8 microseconds)
P90 Latency            : 0.0852 ms   (85.2 microseconds)
P95 Latency            : 0.1481 ms   (148.1 microseconds)
P99 Latency            : 2.9856 ms
Max Latency            : 1,155.0311 ms
===============================================================================
```

---

## 📡 REST API Reference

### 1. Shorten URL
`POST /api/v1/urls` (Rate Limited)

**Request Body:**
```json
{
  "originalUrl": "https://www.google.com/search?q=distributed+systems+architecture",
  "customAlias": "custom-systems",
  "ttlSeconds": 86400
}
```

**Response (`201 Created`):**
```json
{
  "shortCode": "custom-systems",
  "shortUrl": "http://localhost:8080/custom-systems",
  "originalUrl": "https://www.google.com/search?q=distributed+systems+architecture",
  "createdAt": "2026-08-31T00:15:00Z",
  "expiresAt": "2026-09-01T00:15:00Z",
  "customAlias": true
}
```

---

### 2. High-Speed Redirect
`GET /{shortCode}`

**Response (`302 Found`):**
- Header `Location`: `https://www.google.com/search?q=distributed+systems+architecture`
- Header `Cache-Control`: `no-cache, no-store, max-age=0, must-revalidate`

---

### 3. Real-Time Click Analytics
`GET /api/v1/analytics/{shortCode}`

**Response (`200 OK`):**
```json
{
  "shortCode": "custom-systems",
  "originalUrl": "https://www.google.com/search?q=distributed+systems+architecture",
  "totalClicks": 12840,
  "uniqueVisitors": 8420,
  "firstClickAt": "2026-08-31T00:15:10Z",
  "lastClickAt": "2026-08-31T00:20:00Z",
  "clicksByCountry": {
    "US": 6420,
    "IN": 3100,
    "DE": 1820,
    "GB": 1500
  },
  "clicksByDevice": {
    "Mobile": 7450,
    "Desktop": 4890,
    "Tablet": 500
  },
  "clicksByBrowser": {
    "Chrome": 8900,
    "Safari": 2500,
    "Firefox": 1440
  },
  "clicksByReferrer": {
    "google.com": 6200,
    "twitter.com": 4100,
    "Direct": 2540
  }
}
```

---

### 4. Cache Metrics & Hit Ratio
`GET /api/v1/metrics/cache`

**Response (`200 OK`):**
```json
{
  "hits": 9802,
  "misses": 198,
  "negativeHits": 45,
  "totalRequests": 10000,
  "hitRatioPercentage": 98.02
}
```

---

### 5. On-Demand Redirect Load Benchmark
`POST /api/v1/benchmark/run?requests=10000&concurrency=50`

Runs an on-demand multi-threaded concurrency benchmark and returns real-time throughput (RPS), cache hit ratio, and P50/P90/P95/P99 latency percentiles.

---

## 🐳 Docker Compose Deployment

Spin up the complete distributed stack including PostgreSQL, Redis, Kafka, MongoDB, 2 horizontally scaled Spring Boot application nodes, and NGINX:

```bash
docker-compose up --build -d
```

### Stack Components:
| Service | Image | Internal Port | External Port | Description |
|---|---|---|---|---|
| `postgres` | `postgres:16-alpine` | `5432` | `5432` | Indexed relational storage |
| `redis` | `redis:7-alpine` | `6379` | `6379` | LRU caching & sliding-window rate limit |
| `kafka` | `apache/kafka:3.7.0` | `9092` | `9092` | KRaft-mode async event log |
| `mongodb` | `mongo:7.0` | `27017` | `27017` | Time-series and faceted analytics |
| `app-1` | `url-shortener:latest`| `8080` | `8081` | Spring Boot Stateless Instance 1 |
| `app-2` | `url-shortener:latest`| `8080` | `8082` | Spring Boot Stateless Instance 2 |
| `nginx` | `nginx:alpine` | `80` | `80` | Least-Connection Load Balancer |

---

## 🧪 Running Tests & Local Build

Execute all unit tests, integration tests, and concurrency benchmarks:

```powershell
mvn clean test
```
