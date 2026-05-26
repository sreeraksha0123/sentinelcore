# SentinelCore

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-7.4-231F20?logo=apachekafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

> **Production-grade subscription & entitlement engine.** Handles plan lifecycle (FREE → PRO → ENTERPRISE), enforces feature-gating per tier, and serves entitlement checks at **5,000+ req/s** via a Redis cache that is populated on login and invalidated on plan change. Payment events arrive via Kafka — completely decoupled from the subscription logic.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Load Testing](#load-testing)
- [Project Structure](#project-structure)
- [Design Decisions](#design-decisions)
- [Sample Output](#sample-output)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (REST)                           │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP + Bearer JWT
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SENTINELCORE (Spring Boot)                   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  JwtAuthFilter → SecurityContextHolder                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌────────────────┐  ┌───────────────────┐  ┌──────────────┐  │
│  │ AuthController │  │SubscriptionCtrl   │  │Entitlement   │  │
│  │  /api/auth/**  │  │ /api/subscriptions│  │Controller    │  │
│  └───────┬────────┘  └────────┬──────────┘  │/api/entitle- │  │
│          │                    │             │ ments/**     │  │
│          ▼                    ▼             └──────┬───────┘  │
│  ┌──────────────┐    ┌───────────────────┐         │          │
│  │  UserService │    │SubscriptionService│         │          │
│  └──────┬───────┘    └────────┬──────────┘         │          │
│         │                     │              ┌──────▼───────┐  │
│         └─────────────────────┼──────────────► Entitlement  │  │
│                               │              │  Service     │  │
│                               │              └──────┬───────┘  │
│                               │                     │          │
└───────────────────────────────┼─────────────────────┼──────────┘
                                │                     │
              ┌─────────────────┼─────────────────────┼──────────────┐
              │                 │                     │              │
              ▼                 ▼                     ▼              │
       ┌────────────┐   ┌──────────────┐    ┌─────────────────┐     │
       │ PostgreSQL │   │ PostgreSQL   │    │  Redis Cache    │     │
       │   users    │   │subscriptions │    │ entitlement:{id}│     │
       │entitlements│   │              │    │   TTL: 1 hour   │     │
       └────────────┘   └──────────────┘    └─────────────────┘     │
                                                                     │
              ┌──────────────────────────────────────────────────┐   │
              │              KAFKA (payment-events)              │   │
              │                                                  │   │
              │  Payment Service ──► Topic ──► SentinelCore      │   │
              │                               KafkaConsumer      │   │
              │                               → upgrade DB       │   │
              │                               → invalidate Redis │   │
              └──────────────────────────────────────────────────┘   │
```

### Entitlement Check — Hot Path

```
GET /api/entitlements/{userId}
     │
     ├─► Redis.get("entitlement:{userId}")
     │        │
     │   ┌────┴─────────────────────────────┐
     │   │  HIT (>99% of requests)          │  MISS (<1%)
     │   │  return cached JSON  <1ms        │  → PostgreSQL.query(subscriptions)
     │   └──────────────────────────────────┘  → Redis.set(TTL 1h)
     │                                          → return
```

### Payment Event Path

```
Payment Service → Kafka Topic: payment-events
    → SentinelCore PaymentEventConsumer
        → if status == "SUCCESS"
            → PostgreSQL.update(subscription.plan, status = ACTIVE)
            → Redis.delete("entitlement:{userId}")   // explicit invalidation
            → log success
        → if status != "SUCCESS"
            → log warning, skip
```

---

## Tech Stack

| Component        | Technology                      | Purpose                                    |
|------------------|---------------------------------|--------------------------------------------|
| API Framework    | Spring Boot 3.2 + Spring Web    | REST endpoints, DI container               |
| Auth             | Spring Security + JJWT 0.11.5   | Stateless JWT filter chain                 |
| Database         | PostgreSQL 15 + Spring Data JPA | ACID-compliant subscription persistence    |
| Cache            | Redis 7 + Spring Data Redis     | Entitlement cache, 5k+ req/s hot path      |
| Event Bus        | Apache Kafka 7.4                | Decoupled payment event consumption        |
| Validation       | Jakarta Bean Validation         | Request DTO validation                     |
| Observability    | Spring Actuator                 | Health checks, metrics                     |
| Build            | Maven 3.8+                      | Dependency management, packaging           |
| Infra            | Docker Compose                  | Local Postgres, Redis, Kafka, Zookeeper    |

---

## Prerequisites

- **Java 17+** — `java -version`
- **Maven 3.8+** — `mvn -version`
- **Docker + Docker Compose** — runs all infrastructure locally
- **Postman** (optional) — for API testing
- **Apache JMeter 5.6+** (optional) — for load testing

---

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/your-username/sentinelcore.git
cd sentinelcore
```

### 2. Start infrastructure

```bash
docker-compose up -d
```

Wait ~15 seconds for Kafka to be ready, then verify:

```bash
docker-compose ps   # all containers should be "healthy" or "running"
```

### 3. Run the application

```bash
mvn spring-boot:run
```

Or run `SentinelCoreApplication.java` from IntelliJ / VS Code.

### 4. Verify the app is up

```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

### 5. Register and get a JWT

```bash
# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123"}' | jq .

# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123"}' | jq -r .token)

echo "Token: $TOKEN"
```

### 6. Check entitlements (hot path)

```bash
curl -s http://localhost:8080/api/entitlements/1 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 7. Check Redis cache directly

```bash
docker exec sentinel-redis redis-cli get "entitlement:1"
```

---

## API Reference

All endpoints except `/api/auth/**` require `Authorization: Bearer <token>`.

### Auth

| Method | Endpoint              | Description                              | Body                              |
|--------|-----------------------|------------------------------------------|-----------------------------------|
| POST   | `/api/auth/register`  | Register new user; returns JWT           | `{"email":"...","password":"..."}` |
| POST   | `/api/auth/login`     | Login; populates Redis cache; returns JWT | `{"email":"...","password":"..."}` |

### Subscriptions

| Method | Endpoint                       | Description                                        | Body                    |
|--------|--------------------------------|----------------------------------------------------|-------------------------|
| GET    | `/api/subscriptions/me`        | Current user's subscription details                | —                       |
| POST   | `/api/subscriptions/upgrade`   | Upgrade plan (FREE→PRO, PRO→ENTERPRISE). Invalidates cache | `{"plan":"PRO"}` |
| POST   | `/api/subscriptions/pause`     | Pause active subscription                          | —                       |
| POST   | `/api/subscriptions/cancel`    | Cancel subscription. Invalidates cache             | —                       |

### Entitlements ⚡ Hot Path

| Method | Endpoint                               | Description                                |
|--------|----------------------------------------|--------------------------------------------|
| GET    | `/api/entitlements/{userId}`           | Full entitlement map, served from Redis    |
| GET    | `/api/entitlements/{userId}/check?feature=export_csv` | Single feature boolean check |

### Dev / Testing

| Method | Endpoint                   | Description                                              |
|--------|----------------------------|----------------------------------------------------------|
| POST   | `/api/test/payment-event`  | Publish a Kafka payment event (triggers upgrade flow)    |

### Actuator

| Endpoint              | Description         |
|-----------------------|---------------------|
| `/actuator/health`    | Health status       |
| `/actuator/info`      | App info            |
| `/actuator/metrics`   | Micrometer metrics  |

---

## Load Testing

The entitlement endpoint is designed to handle **5,000+ req/s** because Redis cache bypasses PostgreSQL on every cached request.

### JMeter Test Plan

1. Open JMeter → New Test Plan
2. Add **Thread Group**: 500 threads, ramp-up 10s, loop 100 → ≈5,000 req/s
3. Add **HTTP Request**: `GET http://localhost:8080/api/entitlements/1`
4. Add **HTTP Header Manager**: `Authorization: Bearer <your-token>`
5. Add **Aggregate Report** listener
6. Run → check **Throughput** column (target: >5,000/sec), **Average** <10ms

### Expected Results

```
Target:    > 5,000 req/s throughput
Average:   < 5ms  (warm cache)
p99:       < 10ms (warm cache)
Error %:   0%

Cold cache (first request per user): ~50ms (DB round trip)
Warm cache (all subsequent):         < 1ms (Redis only)
```

---

## Project Structure

```
sentinelcore/
├── src/
│   ├── main/
│   │   ├── java/com/raksha/sentinelcore/
│   │   │   ├── SentinelCoreApplication.java
│   │   │   ├── config/
│   │   │   │   ├── RedisConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── EntitlementController.java
│   │   │   │   ├── SubscriptionController.java
│   │   │   │   └── TestController.java
│   │   │   ├── dto/
│   │   │   │   ├── ApiError.java
│   │   │   │   ├── AuthDtos.java
│   │   │   │   ├── PaymentEvent.java
│   │   │   │   └── SubscriptionDtos.java
│   │   │   ├── entity/
│   │   │   │   ├── Entitlement.java
│   │   │   │   ├── Subscription.java
│   │   │   │   └── User.java
│   │   │   ├── enums/
│   │   │   │   ├── PlanType.java
│   │   │   │   ├── Role.java
│   │   │   │   └── SubStatus.java
│   │   │   ├── exception/
│   │   │   │   ├── Exceptions.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── filter/
│   │   │   │   └── JwtAuthFilter.java
│   │   │   ├── kafka/
│   │   │   │   ├── PaymentEventConsumer.java
│   │   │   │   └── PaymentEventProducer.java
│   │   │   ├── repository/
│   │   │   │   ├── EntitlementRepository.java
│   │   │   │   ├── SubscriptionRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/
│   │   │   │   └── JwtService.java
│   │   │   └── service/
│   │   │       ├── EntitlementService.java
│   │   │       ├── SubscriptionService.java
│   │   │       └── UserService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── schema.sql
│   │       └── data.sql
│   └── test/
│       └── java/com/raksha/sentinelcore/
│           ├── EntitlementServiceTest.java
│           └── SubscriptionServiceTest.java
├── docker-compose.yml
├── pom.xml
├── .gitignore
└── README.md
```

---

## Design Decisions

### Why Redis for entitlements?
The entitlement endpoint is the most-called API — every feature gate in the product hits it. Serving from Redis means PostgreSQL sees virtually zero load on this path. The cache is pre-warmed on login and explicitly invalidated (not TTL-only) on plan change, so clients always see fresh data without polling.

### Why Kafka instead of a direct DB call from the payment service?
Decoupling. The payment service doesn't need to know SentinelCore exists. If SentinelCore restarts, events queue up and replay automatically — no data loss, no synchronous dependency. In production, add a Dead Letter Queue for failed events.

### Why stateless JWT?
No session storage required. Scales horizontally without sticky sessions or shared session stores. The 24-hour TTL is intentional; for sensitive operations, pair with refresh token rotation.

### Why explicit cache invalidation over TTL-only?
A 1-hour TTL is a safety net, not the primary invalidation strategy. When a user upgrades or cancels, they expect the change to be reflected immediately. Explicit `redis.delete(key)` on state change ensures consistency within milliseconds.

---

## Sample Output

### Register
```json
POST /api/auth/register
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0...",
  "type": "Bearer"
}
```

### Get Subscription
```json
GET /api/subscriptions/me
{
  "id": 1,
  "userId": 1,
  "email": "test@example.com",
  "plan": "FREE",
  "status": "ACTIVE",
  "startedAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Entitlement Check (FREE plan)
```json
GET /api/entitlements/1
{
  "api_calls_per_day": true,
  "export_csv": false,
  "custom_sso": false,
  "priority_support": false,
  "audit_logs": false
}
```

### Feature Check
```json
GET /api/entitlements/1/check?feature=export_csv
false
```

### After Upgrade to PRO
```json
GET /api/entitlements/1
{
  "api_calls_per_day": true,
  "export_csv": true,
  "custom_sso": false,
  "priority_support": true,
  "audit_logs": false
}
```

### Kafka Payment Event (dev trigger)
```json
POST /api/test/payment-event
{
  "userId": 1,
  "plan": "ENTERPRISE",
  "transactionId": "txn-abc123",
  "status": "SUCCESS"
}

Response:
{
  "message": "Payment event published",
  "transactionId": "txn-abc123"
}
```

---

## License

MIT
