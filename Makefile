# ============================================================
# Makefile — Multi-City Travel Platform
# Developer shortcuts for Docker, Maven, and DB operations
# ============================================================

.PHONY: help infra-up infra-down services-up services-down \
        up down wipe logs logs-svc build clean test \
        db-migrate db-seed kafka-topics

# Default target
help:
	@echo ""
	@echo "  Multi-City Travel Platform — Available Commands"
	@echo "  ================================================"
	@echo ""
	@echo "  Infrastructure:"
	@echo "    make infra-up        Start all infra (Postgres, Redis, Kafka, RabbitMQ, Zipkin, Prometheus, Grafana)"
	@echo "    make infra-down      Stop infra containers"
	@echo "    make infra-wipe      Stop infra + wipe all volumes (DESTRUCTIVE)"
	@echo ""
	@echo "  Services:"
	@echo "    make services-up     Start all Spring Boot services"
	@echo "    make services-down   Stop all services"
	@echo "    make build           Build all service Docker images"
	@echo ""
	@echo "  Combined:"
	@echo "    make up              Start everything (infra + services)"
	@echo "    make down            Stop everything"
	@echo "    make wipe            Stop everything + wipe volumes (DESTRUCTIVE)"
	@echo ""
	@echo "  Logs:"
	@echo "    make logs            Tail all service logs"
	@echo "    make logs-svc svc=flight-service   Tail specific service logs"
	@echo ""
	@echo "  Database:"
	@echo "    make db-seed         Run Python seed scripts"
	@echo "    make kafka-topics    List all Kafka topics"
	@echo ""
	@echo "  Development:"
	@echo "    make clean           Clean all Maven build artifacts"
	@echo "    make test            Run all unit + integration tests"
	@echo "    make mvn-build       Build all JARs with Maven"
	@echo ""
	@echo "  URLs (after startup):"
	@echo "    Eureka Dashboard:     http://localhost:8761"
	@echo "    API Gateway:          http://localhost:8080"
	@echo "    Kafka UI:             http://localhost:8090"
	@echo "    RabbitMQ UI:          http://localhost:15672"
	@echo "    Zipkin:               http://localhost:9411"
	@echo "    Prometheus:           http://localhost:9090"
	@echo "    Grafana:              http://localhost:3000"
	@echo ""

# ===========================
# Infrastructure
# ===========================

infra-up:
	@echo "🚀 Starting infrastructure..."
	docker compose -f docker-compose.infra.yml up -d
	@echo "✅ Infrastructure is up"
	@echo "   Waiting for healthchecks..."
	@sleep 10
	docker compose -f docker-compose.infra.yml ps

infra-down:
	@echo "🛑 Stopping infrastructure..."
	docker compose -f docker-compose.infra.yml down

infra-wipe:
	@echo "💥 Wiping infrastructure + all data volumes..."
	docker compose -f docker-compose.infra.yml down -v
	@echo "✅ Wiped"

# ===========================
# Services
# ===========================

build:
	@echo "🏗️  Building all service images..."
	docker compose -f docker-compose.services.yml build --parallel

services-up:
	@echo "🚀 Starting all services..."
	docker compose -f docker-compose.services.yml up -d
	@echo "✅ Services are up"

services-down:
	@echo "🛑 Stopping all services..."
	docker compose -f docker-compose.services.yml down

# ===========================
# Combined
# ===========================

up:
	@echo "🚀 Starting full stack..."
	docker compose up -d
	@echo "✅ Full stack is running"
	@make status

down:
	@echo "🛑 Stopping full stack..."
	docker compose down

wipe:
	@echo "💥 Full wipe..."
	docker compose down -v --remove-orphans
	@echo "✅ Everything wiped"

# ===========================
# Status
# ===========================

status:
	@echo ""
	@echo "Container Status:"
	docker compose ps
	@echo ""

# ===========================
# Logs
# ===========================

logs:
	docker compose logs -f --tail=100

logs-svc:
	docker compose logs -f --tail=200 $(svc)

# ===========================
# Database
# ===========================

db-seed:
	@echo "🌱 Running database seed scripts..."
	cd db/seed && pip install -r requirements.txt -q && python seed_all.py
	@echo "✅ Seed complete"

# ===========================
# Kafka
# ===========================

kafka-topics:
	@echo "📋 Kafka topics:"
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

kafka-create-topics:
	@echo "📝 Creating Kafka topics..."
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --if-not-exists --topic flight.delayed --partitions 3 --replication-factor 1
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --if-not-exists --topic flight.cancelled --partitions 3 --replication-factor 1
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --if-not-exists --topic booking.completed --partitions 3 --replication-factor 1
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --if-not-exists --topic payment.completed --partitions 3 --replication-factor 1
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --if-not-exists --topic payment.failed --partitions 3 --replication-factor 1
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
		--create --if-not-exists --topic seat.released --partitions 3 --replication-factor 1
	@echo "✅ Topics created"

# ===========================
# Maven
# ===========================

mvn-build:
	@echo "📦 Building all Maven modules..."
	mvn clean package -DskipTests --no-transfer-progress
	@echo "✅ Build complete"

clean:
	@echo "🧹 Cleaning Maven build artifacts..."
	mvn clean --no-transfer-progress
	@echo "✅ Clean complete"

test:
	@echo "🧪 Running all tests..."
	mvn test --no-transfer-progress
	@echo "✅ Tests complete"

# ===========================
# Useful dev shortcuts
# ===========================

psql-auth:
	docker exec -it postgres-auth psql -U $${AUTH_DB_USER:-auth_user} -d $${AUTH_DB_NAME:-auth_db}

psql-flight:
	docker exec -it postgres-flight psql -U $${FLIGHT_DB_USER:-flight_user} -d $${FLIGHT_DB_NAME:-flight_db}

psql-booking:
	docker exec -it postgres-booking psql -U $${BOOKING_DB_USER:-booking_user} -d $${BOOKING_DB_NAME:-booking_db}

redis-cli:
	docker exec -it redis redis-cli -a $${REDIS_PASSWORD:-redis_secret_pass}

rabbit-shell:
	docker exec -it rabbitmq rabbitmqctl status
