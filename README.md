# Souqi Market — E-commerce Platform (Spring Boot 3.5 / Java 21)

A capstone project that brings the whole course together in one application: a product catalog, cart,
atomic order placement, a simulated payment gateway behind a circuit breaker, outbox events over Kafka
and idempotent notification consumption — plus JWT security, OpenAPI documentation, integration tests
and a Docker Compose deployment.

---

## 1) Features

| Feature | Details |
|---------|---------|
| Authentication | Register/login, JWT, `CUSTOMER` and `ADMIN` roles, BCrypt passwords |
| Catalog | Categories, search by name/SKU, price and category filters, paging and sorting, Redis cache |
| Administration | Create/update products, adjust stock, list all orders |
| Cart | Add/update/remove items, live stock and price validation |
| Orders | Atomic purchase with pessimistic stock locking + `Idempotency-Key` + stock restoration on payment failure |
| Payments | Simulated gateway wrapped in Resilience4j `@CircuitBreaker` with a safe fallback |
| Events | Outbox written in the order transaction, then published to Kafka by a scheduler, consumed idempotently |
| Quality | Unified error handling (ProblemDetail), OpenAPI, Actuator, traceId in logs, integration tests |

---

## 2) Quick start (Docker Compose)

```bash
cp .env.example .env      # edit the secrets
docker compose up --build
```

| Service | URL |
|---------|-----|
| Application | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5432 (souqi/souqi) |
| Redis | localhost:6379 |
| Kafka | localhost:9092 |

On first startup Flyway migrations run and the following are created:

- Admin account: `admin@souqi.local` / `Admin@12345` (from `.env`)
- Demo catalog: 6 products in 2 categories

Change the admin password and `JWT_SECRET` before any real deployment.

---

## 3) Local development

```bash
# 1) Start the infrastructure only
docker compose up -d postgres redis kafka

# 2) Run the application
mvn spring-boot:run
```

Or run the tests (Docker is required to start a temporary PostgreSQL container):

```bash
mvn test
```

---

## 4) Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/souqi` | Database connection |
| `DB_USER` / `DB_PASSWORD` | `souqi` / `souqi` | Database credentials |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Product cache |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka broker |
| `JWT_SECRET` | development secret | **At least 32 bytes** |
| `JWT_EXPIRATION_MINUTES` | `60` | Token lifetime |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@souqi.local` / `Admin@12345` | Initial admin account |
| `PAYMENT_FAILURE_RATE` | `0.05` | Simulated payment failure rate |
| `OUTBOX_PUBLISHER_ENABLED` | `true` | Enable the outbox publisher |
| `OUTBOX_CONSUMER_ENABLED` | `true` | Enable the notification consumer |
| `CACHE_ENABLED` | `true` | Enable the Redis cache |
| `SEED_ENABLED` | `true` | Seed demo data |

---

## 5) API endpoints

```http
# Authentication (public)
POST   /api/auth/register                        → 201 { id }
POST   /api/auth/login                           → 200 { accessToken, role, fullName }

# Catalog (public)
GET    /api/products?q=&category=&categoryId=&minPrice=&maxPrice=&page=&size=&sort=
GET    /api/products/{sku}
GET    /api/categories

# Catalog (ADMIN)
POST   /api/products                             → 201
PATCH  /api/products/{sku}                       → 200
PATCH  /api/products/{sku}/stock                 → 200
POST   /api/admin/categories                     → 201

# Cart (authenticated)
GET    /api/cart
POST   /api/cart/items                           { sku, quantity }
PATCH  /api/cart/items/{sku}                     { quantity }  (0 = remove)
DELETE /api/cart/items/{sku}

# Orders (authenticated)
POST   /api/orders/purchase                      + Idempotency-Key → 201
POST   /api/orders/from-cart                     + Idempotency-Key → 201
GET    /api/orders?page=&size=
GET    /api/orders/{orderNo}                     → 200 | 403 | 404
POST   /api/orders/{orderNo}/cancel

# Administration (ADMIN)
GET    /api/admin/orders?status=&page=&size=
```

### Full walkthrough with curl

```bash
# Register and log in
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"sara@example.com","fullName":"Sara","password":"Secret@123"}'

TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"sara@example.com","password":"Secret@123"}' | jq -r .accessToken)

# Browse products (public)
curl 'localhost:8080/api/products?q=phone&page=0&size=10'

# Add to cart and check out
curl -X POST localhost:8080/api/cart/items -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"sku":"PHONE-001","quantity":1}'

curl -X POST localhost:8080/api/orders/purchase -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: buy-1001" \
  -d '{"lines":[{"sku":"PHONE-001","quantity":1}]}'

# Repeat the same request with the same key: same result, no extra stock deducted
curl -X POST localhost:8080/api/orders/purchase -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: buy-1001" \
  -d '{"lines":[{"sku":"PHONE-001","quantity":1}]}'
```

---

## 6) Architecture decisions (short ADR)

| Decision | Alternative | Rationale |
|----------|-------------|-----------|
| Stateless JWT | In-memory sessions | Horizontal scaling without session storage |
| Flyway + `ddl-auto=validate` | Automatic `update` | Reviewable, reversible schema changes |
| Pessimistic lock on purchase | Optimistic locking only | Prevents overselling under high concurrency |
| Stored Idempotency-Key | No keys | Prevents duplicate payment/stock deduction on retries |
| Outbox inside the transaction | Direct publish in the transaction | Events are not lost when Kafka is down |
| Idempotent notification consumer | Send without checks | Duplicate messages do not duplicate emails |
| Redis cache | Local cache | Consistency across multiple application instances |
| Simulated gateway + CircuitBreaker | Real external call | Demonstrates boundaries and trade-offs without provider keys |

### Purchase flow

```
Client ──POST /orders/purchase (Idempotency-Key)──▶ OrderController
                                                     │
                                            OrderFacade│
                          ┌──────────────────────────┴─────────────────────┐
                          ▼                                                 ▼
              OrderService.place (TX)                          PaymentGatewayClient
  1. Idempotency check                                             (CircuitBreaker + fallback)
  2. Lock rows and deduct stock                                            │
  3. Create Order + Items                                                  ▼
  4. Write OutboxEvent(OrderPlaced)                             success ⇒ markPaid (TX)
     └──────── same transaction (Commit/Rollback) ────────┘    failure ⇒ failAndRestoreStock (TX)

OutboxPublisher (scheduled) ──▶ Kafka topic shop.orders.v1 ──▶ OrderNotificationConsumer
                                                           (notification_log = deduplication)
```

---

## 7) Project structure

```
src/main/java/com/example/market
├── MarketApplication.java
├── auth/            # User, JWT, register/login, UserDetails
├── catalog/         # Product, Category, search, admin, cache
├── cart/            # Cart and its items
├── order/           # Order, Idempotency, atomic transaction, admin
├── payment/         # Simulated gateway + CircuitBreaker
├── outbox/          # OutboxEvent + scheduled publisher
├── notification/    # Idempotent Kafka consumer
├── common/          # Unified errors, PageResponse, TraceId
└── config/          # Security, Cache, OpenAPI, DataSeeder

src/main/resources
├── application.yml
├── logback-spring.xml
└── db/migration/    # V1..V4 (Flyway)

src/test/java/com/example/market
├── TestcontainersConfiguration.java
├── catalog/ProductDomainTest.java            # unit
├── auth/AuthFlowIntegrationTest.java         # Web + JPA integration
├── catalog/CatalogApiIntegrationTest.java    # integration + authorization
└── order/OrderPlacementIntegrationTest.java  # Idempotency + E2E
```

---

## 8) Final checklist

- [x] Full purchase flow: browse → cart → checkout → order status
- [x] Administration: create product, adjust stock, list orders
- [x] Complete Flyway migrations, no `ddl-auto=update`
- [x] Unified error handling with correct HTTP status codes
- [x] Idempotency on purchase (test replays the request with the same key)
- [x] Stock restoration on payment failure + `PAYMENT_FAILED` status
- [x] Unit + integration + E2E tests
- [x] OpenAPI + Swagger UI
- [x] Health endpoint + traceId in logs
- [x] Multi-stage Dockerfile + one-command Compose
- [x] Order ownership check (a customer cannot read another customer's order)

---

## 9) Interview talking points

> "Atomic transactions and idempotency are the heart of reliable checkout: the stock lock prevents
> overselling, and the idempotency key prevents double deduction on retries. I chose the outbox over
> direct publishing because a Kafka outage during checkout would have lost the notification, and the
> cost of one extra table and a small scheduler is cheaper than a lost event."
