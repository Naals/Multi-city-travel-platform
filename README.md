# ✈️ Multi-City Travel Platform

A production-grade microservices platform for searching and booking multi-city flights, featuring **Dijkstra's shortest-path algorithm** for intelligent route discovery through intermediate cities (e.g., NYC → Istanbul → Berlin → Paris).

---

## 🏗️ Architecture

```
                        ┌─────────────────────────┐
                        │       Client App         │
                        └────────────┬────────────┘
                                     │ HTTP
                        ┌────────────▼────────────┐
                        │       API Gateway        │  :8080
                        │  JWT Filter | Rate Limit │
                        │  Circuit Breaker | Routes│
                        └──┬──────┬──────┬─────┬──┘
                           │      │      │     │
             ┌─────────────┘      │      │     └──────────────┐
             │                    │      │                     │
    ┌────────▼──────┐   ┌─────────▼──┐ ┌▼──────────┐  ┌──────▼────────┐
    │ auth-service  │   │user-service│ │flight-svc  │  │booking-service│
    │    :8081      │   │   :8082    │ │   :8083    │  │    :8084      │
    │ JWT + Redis   │   │ REST+gRPC  │ │ Dijkstra   │  │gRPC+Feign+MQ  │
    └───────────────┘   └────────────┘ │ Graph+Cache│  └───────┬───────┘
                                       └────────────┘          │ RabbitMQ
                                                       ┌────────▼───────┐
                                                       │payment-service │
                                                       │    :8085       │
                                                       │  MQ Consumer   │
                                                       └────────┬───────┘
                                                                │ Kafka
                                               ┌────────────────┼──────────────┐
                                               │                │              │
                                      ┌────────▼──────┐  ┌──────▼─────────────▼──────┐
                                      │review-service │  │   notification-service     │
                                      │    :8086      │  │         :8087              │
                                      └───────────────┘  └────────────────────────────┘
```

## 🛣️ Dijkstra Route Search

Search for flights from **NYC → Paris** and the engine finds:
- NYC → **Istanbul** → Paris
- NYC → **Frankfurt** → Berlin → Paris
- NYC → Istanbul → **Berlin** → Paris

```json
POST /api/routes/search
{
  "originCityCode": "NYC",
  "destinationCityCode": "PAR",
  "departureDate": "2025-06-15",
  "maxStops": 2,
  "sortBy": "PRICE"
}
```

**Graph model:**
- Vertices → cities
- Edges → flights between cities  
- Edge weight → price (or duration)
- Constraints → min 60min layover, active flights only, no circular paths

## 🔧 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Service Discovery | Netflix Eureka |
| Config Management | Spring Cloud Config |
| API Gateway | Spring Cloud Gateway |
| Auth | JWT + Redis (refresh tokens) |
| Inter-service REST | OpenFeign + Resilience4j |
| Async Events | Apache Kafka |
| Task Queues | RabbitMQ |
| RPC | gRPC (user-service ↔ booking-service) |
| Circuit Breaker | Resilience4j |
| Database | PostgreSQL 16 (×6, one per service) |
| Migrations | Flyway |
| Caching | Redis 7 |
| Tracing | Zipkin + Micrometer |
| Metrics | Prometheus + Grafana |
| Containerization | Docker + Docker Compose |
| Build | Maven (multi-module) |
| Mapping | MapStruct |

## 📁 Project Structure

```
multi-city-travel/
├── docker-compose.yml          # Master (infra + services)
├── docker-compose.infra.yml    # Infra only
├── docker-compose.services.yml # Services only
├── Makefile                    # Developer shortcuts
├── .env.example                # Environment template
├── config-server/              # Spring Cloud Config
├── eureka-server/              # Service Discovery
├── api-gateway/                # Entry point + auth filter
├── auth-service/               # JWT auth + refresh
├── user-service/               # User profiles + gRPC server
├── flight-service/             # Dijkstra routing engine
├── booking-service/            # Booking lifecycle
├── payment-service/            # Payment processing
├── review-service/             # Flight reviews
├── notification-service/       # Event-driven notifications
├── proto/                      # Shared gRPC .proto files
├── db/
│   ├── migrations/             # Flyway SQL per service
│   ├── seed/                   # Python seed scripts
│   └── plpgsql/                # Functions, triggers, cursors
└── monitoring/
    ├── prometheus.yml
    └── grafana/
```

## 🚀 Quick Start

### Prerequisites
- Docker 24+ and Docker Compose v2
- Java 17+ (for local dev)
- Maven 3.9+
- Python 3.9+ (for seed scripts)

### 1. Clone & Configure
```bash
git clone https://github.com/your-org/multi-city-travel.git
cd multi-city-travel
cp .env.example .env
# Edit .env and set JWT_SECRET (min 256-bit random string)
```

### 2. Start Infrastructure
```bash
make infra-up
# Wait ~30s for healthchecks to pass
```

### 3. Build & Start Services
```bash
make build
make services-up
```

### 4. Seed Database
```bash
make db-seed
```

### 5. Verify Everything
```bash
make status
```

## 🌐 Service URLs

| Service | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Auth Swagger | http://localhost:8081/swagger-ui.html |
| Flight Swagger | http://localhost:8083/swagger-ui.html |
| Kafka UI | http://localhost:8090 |
| RabbitMQ UI | http://localhost:15672 |
| Zipkin | http://localhost:9411 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

## 📡 Key API Endpoints

### Auth
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

### Route Search (Dijkstra)
```
POST /api/routes/search
GET  /api/routes/cities
```

### Flights
```
GET  /api/flights
GET  /api/flights/{id}
POST /api/flights          (ADMIN)
PUT  /api/flights/{id}     (ADMIN)
```

### Bookings
```
POST   /api/bookings
GET    /api/bookings/{id}
DELETE /api/bookings/{id}  (cancel)
GET    /api/bookings/user/{userId}
```

### Payments
```
GET  /api/payments/{id}
POST /api/payments/refund/{bookingId}
```

## ✈️ Flight Problem Handling

| Problem | Mechanism |
|---|---|
| Overbooking | DB trigger `trg_overbooking_guard` |
| Flight cancelled | Kafka `flight.cancelled` → cascade booking updates |
| Flight delayed | Kafka `flight.delayed` → check layover validity |
| Layover too short | Dijkstra edge validator (min 60 min) |
| Duplicate payment | Redis idempotency key check |
| Service down | Resilience4j circuit breaker + fallback |

## 👥 Contributing

Each feature is committed following the plan in `docs/commit-plan.md`.
Branch naming: `day{N}/{service}-{feature}` e.g. `day3/auth-jwt-refresh`

## 📄 License

MIT
